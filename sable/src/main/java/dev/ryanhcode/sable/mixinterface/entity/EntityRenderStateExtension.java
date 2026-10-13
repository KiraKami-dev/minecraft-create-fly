package dev.ryanhcode.sable.mixinterface.entity;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

/**
 * Extension on {@link net.minecraft.client.renderer.entity.state.EntityRenderState} carrying the sub-level
 * rotation of an entity. Since 1.21.11 entity rendering is split into an extract and a submit phase, and the
 * submit phase no longer has access to the entity itself.
 */
public interface EntityRenderStateExtension {

    /**
     * @return the transform to apply to the pose stack before submitting the entity, or null if the entity is not rotated
     */
    @Nullable
    Matrix4fc sable$getRotationTransform();

    /**
     * @return the orientation applied by {@link #sable$getRotationTransform()}
     */
    @Nullable
    Quaternionfc sable$getRotationOrientation();

    void sable$setRotation(@Nullable Matrix4fc transform, @Nullable Quaternionfc orientation);
}
