/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.registry;

import dev.ryanhcode.sable.veillite.platform.registry.RegistrationProvider;
import dev.ryanhcode.sable.veillite.platform.registry.RegistryObject;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class FabricRegistrationFactory {

    private FabricRegistrationFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistrationProvider<T> create(final ResourceKey<? extends Registry<T>> key, final String modId) {
        Registry<?> reg = BuiltInRegistries.REGISTRY.getValue(key.identifier());
        if (reg == null) {
            reg = FabricRegistryBuilder.createSimple((ResourceKey<Registry<T>>) key).buildAndRegister();
        }
        return new Provider<>(modId, (Registry<T>) reg);
    }

    public static <T> RegistrationProvider<T> create(final Registry<T> registry, final String modId) {
        return new Provider<>(modId, registry);
    }

    private static final class Provider<T> implements RegistrationProvider<T> {

        private final String modId;
        private final Registry<T> registry;

        private final Set<RegistryObject<T>> entries = new LinkedHashSet<>();
        private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(this.entries);

        private Provider(final String modId, final Registry<T> registry) {
            this.modId = modId;
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> RegistryObject<I> register(final Identifier id, final Supplier<? extends I> supplier) {
            final I value = Registry.register(this.registry, id, supplier.get());
            final ResourceKey<I> key = ResourceKey.create((ResourceKey<? extends Registry<I>>) this.registry.key(), id);
            final RegistryObject<I> object = new FabricRegistryObject<>(this.registry, key, value);
            this.entries.add((RegistryObject<T>) object);
            return object;
        }

        @Override
        public Collection<RegistryObject<T>> getEntries() {
            return this.entriesView;
        }

        @Override
        public Registry<T> asVanillaRegistry() {
            return this.registry;
        }

        @Override
        public String getModId() {
            return this.modId;
        }
    }

    private record FabricRegistryObject<T, I>(Registry<T> registry,
                                              ResourceKey<I> key,
                                              I value) implements RegistryObject<I> {

        @Override
        public ResourceKey<I> getResourceKey() {
            return this.key;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public I get() {
            return this.value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Holder<I> asHolder() {
            return (Holder<I>) this.registry.getOrThrow((ResourceKey<T>) this.key);
        }
    }
}
