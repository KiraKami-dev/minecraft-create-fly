package dev.mcf.e2e;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Release jars are intermediary-named: Loom renames Minecraft methods ({@code BlockEntity.getLevel()} becomes
 * {@code method_10997}), but an abstract method of a mod interface that overrides nothing in Minecraft keeps its name.
 * If the only implementation is the inherited Minecraft method, it works in dev and throws {@link AbstractMethodError}
 * in production (the hot air burner crash).
 *
 * <p>This checks every concrete class of the ported mods, and every Create class (mixin targets get mod interfaces),
 * against the abstract methods of the mod interfaces it implements. A method fails if, at runtime, it
 * <ul>
 *     <li>has no implementation at all (what happens in production), or</li>
 *     <li>is implemented by a method that is, or overrides, a Minecraft method, while the mod interface itself does
 *     not extend that Minecraft type (what makes it break in production; this is what catches it in dev).</li>
 * </ul>
 */
public final class RemapSafety {
    private static final List<String> MODS = List.of("sable", "simulated", "aeronautics");
    private static final List<String> MIXIN_TARGET_MODS = List.of("create");
    private static final String MINECRAFT_PACKAGE = "net.minecraft.";

    private RemapSafety() {
    }

    public record Result(int checked, int skipped, List<String> failures) {
    }

    public static Result run() {
        final List<String> failures = new ArrayList<>();
        final Set<String> modClassNames = new LinkedHashSet<>();
        for (final String modId : MODS) {
            classNames(modId, modClassNames, failures);
        }
        final Set<String> scanNames = new LinkedHashSet<>(modClassNames);
        for (final String modId : MIXIN_TARGET_MODS) {
            classNames(modId, scanNames, failures);
        }

        final Set<String> problems = new TreeSet<>();
        int checked = 0;
        int skipped = 0;
        for (final String className : scanNames) {
            final Class<?> clazz;
            try {
                clazz = Class.forName(className, false, RemapSafety.class.getClassLoader());
            } catch (final Throwable t) {
                // client-only classes (@Environment(CLIENT)) are stripped on a server and never loaded there
                skipped++;
                continue;
            }
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            try {
                if (checkClass(clazz, modClassNames, problems)) {
                    checked++;
                }
            } catch (final Throwable t) {
                // signatures with client-only types; ServerClassSafety reports those for the mods' own classes
                skipped++;
            }
        }
        failures.addAll(problems);
        return new Result(checked, skipped, failures);
    }

    /**
     * @return whether the class implements any mod interface
     */
    private static boolean checkClass(final Class<?> clazz, final Set<String> modClassNames, final Set<String> problems) {
        final Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            collectInterfaces(c, interfaces);
        }
        boolean relevant = false;
        for (final Class<?> iface : interfaces) {
            if (!modClassNames.contains(iface.getName())) {
                continue;
            }
            relevant = true;
            for (final Method method : iface.getDeclaredMethods()) {
                final int mods = method.getModifiers();
                if (!Modifier.isAbstract(mods) || Modifier.isStatic(mods) || method.isSynthetic()) {
                    continue;
                }
                final String what = clazz.getName() + " does not implement " + iface.getName() + "." + method.getName()
                        + Arrays.toString(method.getParameterTypes());
                final Method impl = findInClassChain(clazz, method);
                if (impl == null) {
                    if (!hasDefault(interfaces, method)) {
                        problems.add(what + " (AbstractMethodError at runtime)");
                    }
                    continue;
                }
                if (!mayRelyOnMinecraftName(iface, method)) {
                    continue;
                }
                final Class<?> minecraftOwner = minecraftDeclarer(impl.getDeclaringClass(), method);
                if (minecraftOwner != null && minecraftDeclarer(iface, method) == null) {
                    problems.add(what + " except through Minecraft's " + minecraftOwner.getName() + "." + method.getName()
                            + ", which is renamed in release jars (AbstractMethodError in production)");
                }
            }
        }
        return relevant;
    }

    /**
     * Mod-prefixed names ({@code sable$...}) are members that mixins add to Minecraft classes, and accessor/invoker
     * interfaces (in mixin packages) get their implementations generated under their own names, so neither depends
     * on a Minecraft name.
     */
    private static boolean mayRelyOnMinecraftName(final Class<?> iface, final Method method) {
        return method.getName().indexOf('$') < 0 && !iface.getName().contains(".mixin.");
    }

    private static void collectInterfaces(final Class<?> type, final Set<Class<?>> out) {
        for (final Class<?> iface : type.getInterfaces()) {
            if (out.add(iface)) {
                collectInterfaces(iface, out);
            }
        }
    }

    private static Method findInClassChain(final Class<?> clazz, final Method method) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            final Method declared = declared(c, method);
            if (declared != null && !Modifier.isStatic(declared.getModifiers())) {
                return Modifier.isAbstract(declared.getModifiers()) ? null : declared;
            }
        }
        return null;
    }

    private static boolean hasDefault(final Set<Class<?>> interfaces, final Method method) {
        for (final Class<?> iface : interfaces) {
            final Method declared = declared(iface, method);
            if (declared != null && declared.isDefault()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the Minecraft class or interface in the hierarchy of {@code type} (including itself) that declares a
     * method with the same name and parameters, or null
     */
    private static Class<?> minecraftDeclarer(final Class<?> type, final Method method) {
        final Set<Class<?>> types = new LinkedHashSet<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            types.add(c);
            collectInterfaces(c, types);
        }
        for (final Class<?> c : types) {
            if (c.getName().startsWith(MINECRAFT_PACKAGE) && declared(c, method) != null) {
                return c;
            }
        }
        return null;
    }

    private static Method declared(final Class<?> type, final Method method) {
        try {
            return type.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    private static void classNames(final String modId, final Set<String> out, final List<String> failures) {
        final ModContainer mod = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if (mod == null) {
            return;
        }
        for (final Path root : mod.getRootPaths()) {
            try (Stream<Path> files = Files.walk(root)) {
                for (final Path file : (Iterable<Path>) files::iterator) {
                    final String name = root.relativize(file).toString().replace('\\', '/');
                    if (name.endsWith(".class") && !name.startsWith("META-INF") && !name.endsWith("module-info.class")) {
                        out.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                    }
                }
            } catch (final IOException e) {
                failures.add("cannot scan " + modId + ": " + e);
            }
        }
    }
}
