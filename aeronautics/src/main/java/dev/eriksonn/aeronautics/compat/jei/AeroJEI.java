package dev.eriksonn.aeronautics.compat.jei;

import com.zurrtum.create.client.compat.jei.JeiClientPlugin;
import com.zurrtum.create.client.compat.jei.display.MysteriousItemConversionDisplay;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.index.AeroTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * JEI plugin (upstream: the NeoForge module's {@code AeroJEI}), loaded by JEI through the {@code jei_mod_plugin}
 * entrypoint. Adds Aeronautics' "mystery conversions" to Create's category. Create Fly's conversion display takes a
 * single input item, so the music disc conversion is listed once per disc of {@code converts_to_cloud_skipper}.
 */
@JeiPlugin
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public final class AeroJEI implements IModPlugin {

    private static final Identifier ID = Aeronautics.path("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final List<MysteriousItemConversionDisplay> recipes = new ArrayList<>();
        recipes.add(createConversion("levitite", AeroItems.LEVITITE_BLEND_BUCKET, AeroItems.LEVITITE));
        recipes.add(createConversion("pearlescent_levitite", AeroItems.LEVITITE_BLEND_BUCKET, AeroItems.PEARLESCENT_LEVITITE));

        // the tag is "#c:music_discs minus the cloud skipper" upstream (NeoForge tag "remove"), so skip it here
        for (final Holder<Item> disc : BuiltInRegistries.ITEM.getTagOrEmpty(AeroTags.ItemTags.CONVERTS_TO_CLOUD_SKIPPER)) {
            if (disc.value() == AeroItems.MUSIC_DISC_CLOUD_SKIPPER) {
                continue;
            }
            final Identifier discId = BuiltInRegistries.ITEM.getKey(disc.value());
            recipes.add(createConversion("music_disc_cloud_skipper/" + discId.getNamespace() + "/" + discId.getPath(),
                    disc.value(), AeroItems.MUSIC_DISC_CLOUD_SKIPPER));
        }

        registration.addRecipes(JeiClientPlugin.MYSTERY_CONVERSION, recipes);
    }

    private static MysteriousItemConversionDisplay createConversion(final String name, final Item input, final Item output) {
        return new MysteriousItemConversionDisplay(Aeronautics.path("conversion_" + name), input, output);
    }
}
