package dev.eriksonn.aeronautics.mixin.steam_vent;


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.zurrtum.create.content.fluids.tank.BoilerData;
import com.zurrtum.create.content.fluids.tank.FluidTankBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.mixinterface.BoilerDataExtension;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Steam vents count as steam consumers of a boiler. The goggle tooltip part (upstream {@code addToGoggleTooltip}) lives
 * in Create Fly's client {@code FluidTankTooltipBehaviour}, see {@link FluidTankTooltipBehaviourMixin}.
 */
@Mixin(BoilerData.class)
public abstract class BoilerDataMixin implements BoilerDataExtension {

	@Shadow(remap = false)
	@Final
	private static float passiveEngineEfficiency;

	@Unique
	private int aeronautics$attachedVents = 0;

	@Override
	public int aeronautics$getAttachedVents() {
		return this.aeronautics$attachedVents;
	}

	@Override
	public float aeronautics$getPassiveEngineEfficiency() {
		return passiveEngineEfficiency;
	}

	@Inject(method = "evaluate", at = @At("HEAD"), remap = false)
	private void aeronautics$countVents1(final FluidTankBlockEntity controller, final CallbackInfoReturnable<Boolean> cir, @Share("prevVents") final LocalIntRef prevVents) {
		prevVents.set(this.aeronautics$attachedVents);
		this.aeronautics$attachedVents = 0;
	}

	// upstream: AllBlocks.STEAM_ENGINE.has(attachedState); Create Fly: attachedState.is(AllBlocks.STEAM_ENGINE)
	@Inject(method = "evaluate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 0, remap = true), remap = false)
	private void aeronautics$countVents2(final FluidTankBlockEntity controller, final CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) final BlockState attachedState) {
		if (attachedState.is(AeroBlocks.STEAM_VENT)) {
			this.aeronautics$attachedVents++;
		}
	}

	@ModifyReturnValue(method = "evaluate", at = @At("RETURN"), remap = false)
	private boolean aeronautics$countVents3(final boolean original, @Share("prevVents") final LocalIntRef prevVents) {
		return original || this.aeronautics$attachedVents != prevVents.get();
	}

	@ModifyReturnValue(method = "isActive", at = @At("RETURN"), remap = false)
	private boolean aeronautics$activeWithVents(final boolean original) {
		return original || this.aeronautics$attachedVents > 0;
	}

	@ModifyExpressionValue(method = {"getEngineEfficiency", "updateOcclusion"}, at = @At(value = "FIELD", target = "Lcom/zurrtum/create/content/fluids/tank/BoilerData;attachedEngines:I"), remap = false)
	private int aeronautics$ventEfficiency(final int original) {
		return original + this.aeronautics$attachedVents;
	}

	@Inject(method = "clear", at = @At("TAIL"), remap = false)
	private void aeronautics$clearVents(final CallbackInfo ci) {
		this.aeronautics$attachedVents = 0;
	}

	@Inject(method = "write", at = @At("TAIL"), remap = false)
	private void aeronautics$writeVentData(final ValueOutput view, final CallbackInfo ci) {
		view.putInt("SimVents", this.aeronautics$attachedVents);
	}

	@Inject(method = "read", at = @At("TAIL"), remap = false)
	private void aeronautics$readVentData(final ValueInput view, final int boilerSize, final CallbackInfo ci) {
		this.aeronautics$attachedVents = view.getIntOr("SimVents", 0);
	}
}
