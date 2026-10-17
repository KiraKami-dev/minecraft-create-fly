package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.PortableEngineDyeingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Recipe serializers (upstream: {@code SimNeoForgeRecipeTypes}, NeoForge module). The recipe type stays vanilla
 * {@code crafting}.
 */
public class SimRecipeSerializers {
    public static final RecipeSerializer<PortableEngineDyeingRecipe> PORTABLE_ENGINE_DYEING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Simulated.path("portable_engine_dyeing"),
            new CustomRecipe.Serializer<>(PortableEngineDyeingRecipe::new)
    );

    public static void register() {
    }
}
