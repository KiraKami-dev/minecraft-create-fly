package dev.eriksonn.aeronautics.fabric.mixin.levitite_blend;

import dev.eriksonn.aeronautics.fabric.content.fluids.levitite.LevititeBlendFluid;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge fluid type behaviour of Levitite Blend for every entity (NeoForge's
 * {@code Entity#updateInWaterStateAndDoFluidPushing} also handled modded fluid types): track the immersion
 * ({@code getFluidHeight(AeroTags.FluidTags.LEVITITE_BLEND)}), push with the fluid type's motion scale and apply its
 * fall distance modifier. The return value also reports being in the blend, like NeoForge's {@code isInFluidType()}.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public double fallDistance;

    @Shadow
    public abstract boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tagKey, double d);

    @Shadow
    public abstract Entity getVehicle();

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("RETURN"), cancellable = true)
    private void aeronautics$levititeBlendPushing(final CallbackInfoReturnable<Boolean> cir) {
        if (this.updateFluidHeightAndDoFluidPushing(AeroTags.FluidTags.LEVITITE_BLEND, LevititeBlendFluid.MOTION_SCALE)) {
            if (!(this.getVehicle() instanceof AbstractBoat)) {
                this.fallDistance *= LevititeBlendFluid.FALL_DISTANCE_MODIFIER;
            }
            cir.setReturnValue(true);
        }
    }
}
