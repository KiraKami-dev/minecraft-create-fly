package dev.ryanhcode.sable.mixinterface.respawn_point;

import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented on {@link net.minecraft.client.renderer.entity.state.LivingEntityRenderState}, carrying the render
 * orientation of the sub-level an entity is sleeping in.
 */
public interface SleepingSubLevelOrientationExtension {

    @Nullable
    Quaternionf sable$getSleepingSubLevelOrientation();

    void sable$setSleepingSubLevelOrientation(@Nullable Quaternionf orientation);
}
