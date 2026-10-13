/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.veillite.api.client.render.MatrixStack;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * {@link MatrixStack} view of a vanilla {@link PoseStack}.
 */
@ApiStatus.Internal
public final class PoseStackMatrixStack implements MatrixStack {

    private final PoseStack poseStack;
    private final Quaternionf castQuat = new Quaternionf();

    public PoseStackMatrixStack(final PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    @Override
    public void clear() {
        while (!this.poseStack.isEmpty()) {
            this.poseStack.popPose();
        }
        this.poseStack.setIdentity();
    }

    @Override
    public void translate(final float x, final float y, final float z) {
        this.poseStack.translate(x, y, z);
    }

    @Override
    public void rotate(final Quaterniondc rotation) {
        this.poseStack.mulPose(this.castQuat.set(rotation));
    }

    @Override
    public void rotate(final Quaternionfc rotation) {
        this.poseStack.mulPose(rotation);
    }

    @Override
    public void rotate(final float angle, final float x, final float y, final float z) {
        this.poseStack.mulPose(this.castQuat.identity().rotateAxis(angle, x, y, z));
    }

    @Override
    public void rotateAround(final Quaterniondc rotation, final double x, final double y, final double z) {
        this.poseStack.rotateAround(this.castQuat.set(rotation), (float) x, (float) y, (float) z);
    }

    @Override
    public void rotateAround(final Quaternionfc rotation, final float x, final float y, final float z) {
        this.poseStack.rotateAround(rotation, x, y, z);
    }

    @Override
    public void applyScale(final float x, final float y, final float z) {
        this.poseStack.scale(x, y, z);
    }

    @Override
    public boolean isEmpty() {
        return this.poseStack.isEmpty();
    }

    @Override
    public void matrixPush() {
        this.poseStack.pushPose();
    }

    @Override
    public void matrixPop() {
        this.poseStack.popPose();
    }

    @Override
    public PoseStack.Pose pose() {
        return this.poseStack.last();
    }

    @Override
    public PoseStack toPoseStack() {
        return this.poseStack;
    }
}
