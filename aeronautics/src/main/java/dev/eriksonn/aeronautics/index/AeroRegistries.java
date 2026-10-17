package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.ryanhcode.sable.veillite.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Aeronautics' own registries (plain Fabric registries, created on class init; entries register immediately).
 */
public class AeroRegistries {
    public static class Keys {
        public static final ResourceKey<Registry<LiftingGasType>> LIFTING_GAS_TYPE = key("lifting_gas_type");
        public static final ResourceKey<Registry<CrystalPropagationContext>> LEVITITE_CRYSTAL_PROPAGATION_CONTEXT = key("levitite_crystal_propagation_context");

        private static <T> ResourceKey<Registry<T>> key(final String name) {
            return ResourceKey.createRegistryKey(Aeronautics.path(name));
        }
    }

    public static final Registry<LiftingGasType> LIFTING_GAS_TYPE = registry(Keys.LIFTING_GAS_TYPE);
    public static final Registry<CrystalPropagationContext> LEVITITE_CRYSTAL_PROPAGATION_CONTEXT = registry(Keys.LEVITITE_CRYSTAL_PROPAGATION_CONTEXT);

    private static <T> Registry<T> registry(final ResourceKey<Registry<T>> registryKey) {
        return RegistrationProvider.get(registryKey, Aeronautics.MOD_ID).asVanillaRegistry();
    }

    public static void register() {
        // creates the registries (class init)
    }
}
