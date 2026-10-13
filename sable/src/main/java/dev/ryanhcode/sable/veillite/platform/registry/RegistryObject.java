/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.platform.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * Represents a wrapper for a registered object.
 *
 * @param <T> the type of the object
 */
public interface RegistryObject<T> extends Supplier<T> {

    ResourceKey<T> getResourceKey();

    default Identifier getId() {
        return this.getResourceKey().identifier();
    }

    boolean isPresent();

    @Override
    T get();

    Holder<T> asHolder();
}
