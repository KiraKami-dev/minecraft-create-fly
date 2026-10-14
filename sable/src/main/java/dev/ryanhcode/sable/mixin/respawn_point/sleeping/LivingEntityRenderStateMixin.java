package dev.ryanhcode.sable.mixin.respawn_point.sleeping;

import dev.ryanhcode.sable.mixinterface.respawn_point.SleepingSubLevelOrientationExtension;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SleepingSubLevelOrientationExtension {

    @Unique
    private Quaternionf sable$sleepingSubLevelOrientation;

    @Override
    public Quaternionf sable$getSleepingSubLevelOrientation() {
        return this.sable$sleepingSubLevelOrientation;
    }

    @Override
    public void sable$setSleepingSubLevelOrientation(final Quaternionf orientation) {
        this.sable$sleepingSubLevelOrientation = orientation;
    }
}
