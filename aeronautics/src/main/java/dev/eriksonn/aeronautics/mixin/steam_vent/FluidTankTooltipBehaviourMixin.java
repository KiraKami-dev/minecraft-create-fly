package dev.eriksonn.aeronautics.mixin.steam_vent;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.FluidTankTooltipBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.fluids.tank.BoilerData;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.mixinterface.BoilerDataExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The boiler goggle tooltip with steam vents (upstream: {@code BoilerDataMixin} on {@code BoilerData#addToGoggleTooltip};
 * Create Fly moved that method into the client {@code FluidTankTooltipBehaviour#addBoilerDataTooltip}).
 */
@Mixin(FluidTankTooltipBehaviour.class)
public abstract class FluidTankTooltipBehaviourMixin {

	@ModifyExpressionValue(method = "addBoilerDataTooltip", at = {
			@At(value = "FIELD", target = "Lcom/zurrtum/create/content/fluids/tank/BoilerData;attachedEngines:I", ordinal = 0),
			@At(value = "FIELD", target = "Lcom/zurrtum/create/content/fluids/tank/BoilerData;attachedEngines:I", ordinal = 2)
	}, remap = false)
	private int aeronautics$countSteamConsumer1(final int original, @Local(argsOnly = true) final BoilerData data) {
		return original + ((BoilerDataExtension) data).aeronautics$getAttachedVents();
	}

	/*
	double totalSU = getEngineEfficiency(boilerSize) * 16 * Math.max(boilerLevel, attachedEngines)
			* BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE);
		->
	double totalSU = (1 or 1/8) * 16 * Math.max(boilerLevel, 1) * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE)
	 */
	@Redirect(method = "addBoilerDataTooltip", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/fluids/tank/BoilerData;getEngineEfficiency(I)F"), remap = false)
	private float aeronautics$trueMaxSU1(final BoilerData instance, final int boilerSize) {
		return instance.isPassive(boilerSize) ? ((BoilerDataExtension) instance).aeronautics$getPassiveEngineEfficiency() : 1;
	}

	@Redirect(method = "addBoilerDataTooltip", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), remap = false)
	private int aeronautics$trueMaxSU2(final int boilerLevel, final int attachedEngines) {
		return Math.max(boilerLevel, 1);
	}

	/*
	Kinetic Stress Capacity:
		[number]su
	Kinetic Stress Used:
		[number]su via n engines
		[number]su via n vents
	 */
	@Inject(method = "addBoilerDataTooltip", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/catnip/lang/LangBuilder;forGoggles(Ljava/util/List;)V", shift = At.Shift.AFTER, ordinal = 2), cancellable = true, remap = false)
	private void aeronautics$reformatBoilerTooltip(final BoilerData data, final List<Component> tooltip, final int boilerSize, final CallbackInfoReturnable<Boolean> cir, @Local final double totalSU, @Local(ordinal = 1) int boilerLevel) {
		final int attachedVents = ((BoilerDataExtension) data).aeronautics$getAttachedVents();
		final float passiveEngineEfficiency = ((BoilerDataExtension) data).aeronautics$getPassiveEngineEfficiency();

		CreateLang.number(totalSU)
				.translate("generic.unit.stress")
				.style(ChatFormatting.AQUA)
				.forGoggles(tooltip, 1);

		boilerLevel = Math.max(boilerLevel, 1);
		final float efficiency = data.isPassive(boilerSize) ? data.getEngineEfficiency(boilerSize) / passiveEngineEfficiency : data.getEngineEfficiency(boilerSize);
		final double engineSU = totalSU * efficiency * data.attachedEngines / boilerLevel;
		final double ventSU = totalSU * efficiency * attachedVents / boilerLevel;

		if (engineSU > 0 || ventSU > 0) {
			AeroLang.translate("tooltip.capacity_used").style(ChatFormatting.GRAY).forGoggles(tooltip);
		}
		if (engineSU > 0) {
			CreateLang.number(engineSU)
					.translate("generic.unit.stress")
					.style(ChatFormatting.AQUA)
					.space()
					.add((data.attachedEngines == 1 ? CreateLang.translate("boiler.via_one_engine")
							: CreateLang.translate("boiler.via_engines", data.attachedEngines)).style(ChatFormatting.DARK_GRAY))
					.forGoggles(tooltip, 1);
		}
		if (ventSU > 0) {
			CreateLang.number(ventSU)
					.translate("generic.unit.stress")
					.style(ChatFormatting.AQUA)
					.space()
					.add((attachedVents == 1 ? AeroLang.translate("boiler.via_one_vent")
							: AeroLang.translate("boiler.via_vents", attachedVents)).style(ChatFormatting.DARK_GRAY))
					.forGoggles(tooltip, 1);
		}
		cir.setReturnValue(true); // cancels
	}
}
