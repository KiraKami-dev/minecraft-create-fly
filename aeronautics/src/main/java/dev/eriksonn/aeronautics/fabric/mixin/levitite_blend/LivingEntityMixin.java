package dev.eriksonn.aeronautics.fabric.mixin.levitite_blend;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.eriksonn.aeronautics.fabric.content.fluids.levitite.LevititeBlendFluid;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge fluid type hooks of {@code LivingEntity} for Levitite Blend (upstream {@code LevititeBlendFluidType#move}
 * is called by NeoForge's patched {@code travel}; jumping in a modded fluid type calls {@code jumpInFluid}).
 * Water keeps priority, as in NeoForge.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    @Shadow
    protected abstract double getEffectiveGravity();

    @Shadow
    public abstract boolean canStandOnFluid(FluidState fluidState);

    @Shadow
    protected abstract void jumpInLiquid(TagKey<Fluid> tagKey);

    @Shadow
    public abstract boolean isAffectedByFluids();

    private boolean aeronautics$isInLevititeBlend() {
        return !this.isInWater() && this.getFluidHeight(AeroTags.FluidTags.LEVITITE_BLEND) > 0.0;
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void aeronautics$travelInLevititeBlend(final Vec3 movement, final CallbackInfo ci) {
        if (!this.aeronautics$isInLevititeBlend()) {
            return;
        }

        final FluidState state = this.level().getFluidState(this.blockPosition());
        if (state.is(AeroTags.FluidTags.LEVITITE_BLEND) && this.isAffectedByFluids() && !this.canStandOnFluid(state)) {
            LevititeBlendFluid.move((LivingEntity) (Object) this, movement, this.getEffectiveGravity(),
                    this.getFluidHeight(AeroTags.FluidTags.LEVITITE_BLEND));
            ci.cancel();
        }
    }

    /**
     * Jumping while in the blend (and not in water or lava) swims upwards, unless standing on the ground in shallow
     * blend. Returning false skips vanilla's jump branch (which then resets {@code noJumpDelay}).
     */
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAffectedByFluids()Z"))
    private boolean aeronautics$jumpInLevititeBlend(final LivingEntity instance, final Operation<Boolean> original) {
        final boolean affected = original.call(instance);
        if (affected && !this.isInLava() && this.aeronautics$isInLevititeBlend()) {
            final double height = this.getFluidHeight(AeroTags.FluidTags.LEVITITE_BLEND);
            if (!this.onGround() || height > this.getFluidJumpThreshold()) {
                this.jumpInLiquid(AeroTags.FluidTags.LEVITITE_BLEND);
                return false;
            }
        }
        return affected;
    }
}
