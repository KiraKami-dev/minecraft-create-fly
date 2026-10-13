/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.platform.registry;

import dev.ryanhcode.sable.veillite.impl.registry.FabricRegistrationFactory;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Utility class for registration. Objects are registered eagerly into the vanilla registry
 * (Fabric semantics); if the registry does not exist yet, a simple Fabric registry is created.
 *
 * @param <T> the type of the objects that this provider registers
 */
public interface RegistrationProvider<T> {

    static <T> RegistrationProvider<T> get(final ResourceKey<? extends Registry<T>> resourceKey, final String modId) {
        return FabricRegistrationFactory.create(resourceKey, modId);
    }

    static <T> RegistrationProvider<T> get(final Registry<T> registry, final String modId) {
        return FabricRegistrationFactory.create(registry, modId);
    }

    default <I extends T> RegistryObject<I> register(final String name, final Supplier<? extends I> supplier) {
        return this.register(Identifier.fromNamespaceAndPath(this.getModId(), name), supplier);
    }

    <I extends T> RegistryObject<I> register(Identifier id, Supplier<? extends I> supplier);

    /**
     * @return An <strong>immutable</strong> view of all the objects currently registered
     */
    Collection<RegistryObject<T>> getEntries();

    /**
     * @return The underlying registry this provider registers into
     */
    Registry<T> asVanillaRegistry();

    /**
     * @return The mod id this provider registers objects for
     */
    String getModId();
}
