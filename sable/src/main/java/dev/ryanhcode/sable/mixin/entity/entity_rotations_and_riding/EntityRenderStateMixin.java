package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import dev.ryanhcode.sable.mixinterface.entity.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateExtension {

    @Unique
    private Matrix4f sable$rotationTransform;

    @Unique
    private Quaternionf sable$rotationOrientation;

    @Unique
    private Quaternionf sable$subLevelOrientation;

    @Override
    public @Nullable Quaternionfc sable$getSubLevelOrientation() {
        return this.sable$subLevelOrientation;
    }

    @Override
    public void sable$setSubLevelOrientation(final @Nullable Quaternionfc orientation) {
        this.sable$subLevelOrientation = orientation == null ? null : new Quaternionf(orientation);
    }

    @Override
    public @Nullable Matrix4fc sable$getRotationTransform() {
        return this.sable$rotationTransform;
    }

    @Override
    public @Nullable Quaternionfc sable$getRotationOrientation() {
        return this.sable$rotationOrientation;
    }

    @Override
    public void sable$setRotation(final @Nullable Matrix4fc transform, final @Nullable Quaternionfc orientation) {
        this.sable$rotationTransform = transform == null ? null : new Matrix4f(transform);
        this.sable$rotationOrientation = orientation == null ? null : new Quaternionf(orientation);
    }
}
