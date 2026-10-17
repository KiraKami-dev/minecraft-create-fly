package dev.simulated_team.simulated.fabric;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.fabric.service.FabricSimConfigService;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric main entrypoint (upstream: {@code SimulatedNeoForge}).
 * <p>
 * Registration timing: everything registers here, in {@code onInitialize}. Create Fly registers its own blocks from a
 * {@code create_plugin} entrypoint during {@code Blocks.<clinit>} only so that it also works without Fabric API (which
 * otherwise adds late block states to the id map and initializes their shape caches); nothing of Create Fly's init
 * references Simulated blocks, so Simulated needs no early registration.
 */
public final class SimulatedFabric implements ModInitializer {

    /**
     * The Simulated creative tab (also holds Aeronautics' items, in its own section). Its contents are laid out by
     * {@code CreativeModeTabMixin} through {@link SimulatedCreativeTab#processItems}; the display generator below is
     * only a fallback.
     */
    public static final CreativeModeTab TAB = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup." + Simulated.MOD_ID + ".group"))
            .icon(() -> new ItemStack(SimBlocks.PHYSICS_ASSEMBLER))
            .displayItems((parameters, output) -> SimulatedCreativeTab.processItems(
                    stack -> output.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY),
                    stack -> output.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)))
            .build();

    @Override
    public void onInitialize() {
        Simulated.init();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Simulated.path("main_tab"), TAB);

        // after Simulated.init(): the stress config entries are collected while registering blocks
        FabricSimConfigService.registerCommon();
    }
}
