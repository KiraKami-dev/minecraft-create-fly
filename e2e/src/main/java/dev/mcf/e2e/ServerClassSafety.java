package dev.mcf.e2e;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Mods like Lithium call getDeclaredMethods()/getDeclaredFields() on every Block, Item, BlockEntity, Entity and menu
 * class at start-up. On a dedicated server that resolves every signature type, so one client-only type in a
 * signature (e.g. a method returning Create Fly's client LangBuilder) crashes the server. This reflects over every
 * such class of the ported mods, and over everything registered under their namespaces, and reports the failures.
 */
public final class ServerClassSafety {
    private static final List<String> MODS = List.of("sable", "simulated", "aeronautics", "offroad");
    private static final Set<String> NAMESPACES = Set.of("sable", "simulated", "aeronautics", "offroad");
    private static final List<Class<?>> ROOTS = List.of(
            Block.class, Item.class, BlockEntity.class, Entity.class, AbstractContainerMenu.class);

    private ServerClassSafety() {
    }

    public record Result(int checked, int skippedClientOnly, List<String> failures) {
    }

    public static Result run() {
        final Set<Class<?>> targets = new LinkedHashSet<>();
        final List<String> failures = new ArrayList<>();
        int skipped = 0;

        BuiltInRegistries.BLOCK.entrySet().forEach(e -> {
            if (NAMESPACES.contains(e.getKey().identifier().getNamespace())) {
                targets.add(e.getValue().getClass());
            }
        });
        BuiltInRegistries.ITEM.entrySet().forEach(e -> {
            if (NAMESPACES.contains(e.getKey().identifier().getNamespace())) {
                targets.add(e.getValue().getClass());
            }
        });

        // block entity, entity and menu classes are not reachable from their registry entries, so scan the mod jars
        for (final String modId : MODS) {
            final ModContainer mod = FabricLoader.getInstance().getModContainer(modId).orElse(null);
            if (mod == null) {
                continue;
            }
            for (final Path root : mod.getRootPaths()) {
                try (Stream<Path> files = Files.walk(root)) {
                    for (final Path file : (Iterable<Path>) files::iterator) {
                        final String name = root.relativize(file).toString().replace('\\', '/');
                        if (!name.endsWith(".class") || name.startsWith("META-INF")) {
                            continue;
                        }
                        final String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                        final Class<?> clazz;
                        try {
                            clazz = Class.forName(className, false, ServerClassSafety.class.getClassLoader());
                        } catch (final Throwable t) {
                            // client-only classes (@Environment(CLIENT)) are stripped on a server and never loaded there
                            skipped++;
                            continue;
                        }
                        for (final Class<?> r : ROOTS) {
                            if (r.isAssignableFrom(clazz)) {
                                targets.add(clazz);
                                break;
                            }
                        }
                    }
                } catch (final IOException e) {
                    failures.add("cannot scan " + modId + ": " + e);
                }
            }
        }

        for (final Class<?> clazz : targets) {
            try {
                clazz.getDeclaredMethods();
                clazz.getDeclaredFields();
                clazz.getDeclaredConstructors();
            } catch (final Throwable t) {
                failures.add(clazz.getName() + " -> " + t);
            }
        }
        return new Result(targets.size(), skipped, failures);
    }
}
