package dev.simulated_team.simulated.compat.jei;

import com.zurrtum.create.client.compat.jei.GhostIngredientHandler;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.SearchAlias;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimResourceManagers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JEI plugin, loaded by JEI through the {@code jei_mod_plugin} entrypoint (never loaded without JEI).
 */
// PORT-TODO(1.21.11): upstream's NeoForge-only JEI plugin also showed the portable engine dyeing crafting recipes; the
//  special dyeing recipe (NeoForge module's PortableEngineDyeingRecipe) isn't ported, so they are not advertised here.
@JeiPlugin
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class SimulatedJEI implements IModPlugin {

    private static final Identifier ID = Simulated.path("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(LinkedTypewriterScreen.class, new GhostIngredientHandler<>());
    }

    @Override
    public void registerModInfo(final IModInfoRegistration modAliasRegistration) {
        final Set<String> mods = simulatedMods();
        for (final String mod : mods) {
            for (final String otherMod : mods.stream().filter(v -> !v.equals(mod)).toList()) {
                modAliasRegistration.addModAliases(mod, otherMod);
            }
        }
    }

    @Override
    public void registerIngredientAliases(final IIngredientAliasRegistration registration) {
        for (final SearchAlias searchAlias : SimResourceManagers.SEARCH_ALIAS.entries()) {
            registration.addAliases(VanillaTypes.ITEM_STACK, searchAlias.getItems(), searchAlias.terms());
        }
    }

    /**
     * Simulated and the mods built on it (upstream: the mods that created a {@code SimulatedRegistrate}), which share
     * their JEI mod aliases.
     */
    private static Set<String> simulatedMods() {
        final Set<String> mods = new LinkedHashSet<>();
        mods.add(Simulated.MOD_ID);

        for (final ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            for (final ModDependency dependency : mod.getMetadata().getDependencies()) {
                if (dependency.getKind() == ModDependency.Kind.DEPENDS && dependency.getModId().equals(Simulated.MOD_ID)) {
                    mods.add(mod.getMetadata().getId());
                }
            }
        }

        return mods;
    }
}
