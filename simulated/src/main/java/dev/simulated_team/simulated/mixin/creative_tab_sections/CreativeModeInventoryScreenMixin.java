package dev.simulated_team.simulated.mixin.creative_tab_sections;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// The section banners are drawn from AbstractContainerScreenMixin (renderContents TAIL), so that they stay below the
// carried item, which 1.21.11 draws in a later GUI stratum.
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

	// Minecraft jars have no local variable table, so locals are matched by their inferred types: the copied tooltip
	// list is an ArrayList (Lists.newArrayList). The tab-name insertion index is always 1 right before the tabs() loop.
	@Inject(method = "getTooltipFromContainerItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;tabs()Ljava/util/List;"))
	private void simulated$getTooltipFromContainerItem(final ItemStack stack, final CallbackInfoReturnable<List<Component>> cir, @Local final ArrayList<Component> list1) {
		final Identifier id = SimulatedRegistrate.sectionOf(stack.getItem());
		if(id != null) {
			final SimulatedSection section = SimResourceManagers.SIMULATED_SECTION.get(id);
			if(section != null) {
				list1.add(1, section.title().text().copy().withStyle(ChatFormatting.BLUE));
			}
		}
	}
}
