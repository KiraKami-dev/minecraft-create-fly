package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Before ticking a particle, try and kick it from a plot (ticking moved from ParticleEngine to ParticleGroup in 1.21.11)
 */
@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin {

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void sable$onParticleTick(final Particle instance, final Operation<Void> original) {
        final ParticleExtension extension = ((ParticleExtension) instance);

        extension.sable$initialKickOut();
        original.call(instance);
        extension.sable$moveWithInheritedVelocity();
    }
}
