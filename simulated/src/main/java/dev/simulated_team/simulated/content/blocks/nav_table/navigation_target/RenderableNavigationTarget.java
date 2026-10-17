package dev.simulated_team.simulated.content.blocks.nav_table.navigation_target;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface RenderableNavigationTarget extends NavigationTarget {

    /**
     * Renders the Item's partials centered on the {@link NavTableRenderer Navigation Table's} item Pedestal <p>
     * Because of this, The Partial model must be centered at its origin <p>
     * Called while the Navigation Table's render state is extracted, so read everything needed from the block entity here;
     * the returned {@link NavTableItemRender} is submitted later with the pose stack already on the pedestal.
     *
     * @param itemState the held item's model, resolved for {@link net.minecraft.world.item.ItemDisplayContext#FIXED}
     */
    default NavTableItemRender extractInNavTable(final ItemStack self, final NavTableBlockEntity navBE, final BlockState navState, final float partialTicks, final ItemStackRenderState itemState) {
        return (ms, queue, light, overlay) -> itemState.submit(ms, queue, light, overlay, 0);
    }

    @FunctionalInterface
    interface NavTableItemRender {
        void submit(PoseStack ms, SubmitNodeCollector queue, int light, int overlay);
    }
}
