package dev.simulated_team.simulated.mixin.creative_tab_sections;

import dev.simulated_team.simulated.client.sections.SimulatedSectionRenderer;
import dev.simulated_team.simulated.mixin.accessor.CreativeModeInventoryScreenSelectedTabAccessor;
import dev.simulated_team.simulated.service.SimTabService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Upstream drew the section banners at the TAIL of CreativeModeInventoryScreen.render with the depth test on. 1.21.11's
// GUI has no depth: the carried item is drawn in a later stratum at the end of AbstractContainerScreen.render, so the
// banners go right after the slots instead (renderContents TAIL), which keeps them above the slots and below the item.
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Inject(method = "renderContents", at = @At("TAIL"))
	private void simulated$renderSectionBanners(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick, final CallbackInfo ci) {
		if ((Object) this instanceof final CreativeModeInventoryScreen screen
				&& CreativeModeInventoryScreenSelectedTabAccessor.simulated$getSelectedTab() == SimTabService.INSTANCE.getCreativeTab()) {
			SimulatedSectionRenderer.renderBanners(screen, guiGraphics, mouseX, mouseY);
		}
	}
}
