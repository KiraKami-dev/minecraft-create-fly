/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

/**
 * Generic matrix stack abstraction over a vanilla {@link PoseStack}.
 */
public interface MatrixStack {

    /**
     * Resets the stack to a single identity matrix.
     */
    void clear();

    default void translate(final Vector3dc offset) {
        this.translate(offset.x(), offset.y(), offset.z());
    }

    default void translate(final Vector3fc offset) {
        this.translate(offset.x(), offset.y(), offset.z());
    }

    default void translate(final double x, final double y, final double z) {
        this.translate((float) x, (float) y, (float) z);
    }

    void translate(float x, float y, float z);

    void rotate(Quaterniondc rotation);

    void rotate(Quaternionfc rotation);

    default void rotate(final double angle, final double x, final double y, final double z) {
        this.rotate((float) angle, (float) x, (float) y, (float) z);
    }

    void rotate(float angle, float x, float y, float z);

    void rotateAround(Quaterniondc rotation, double x, double y, double z);

    void rotateAround(Quaternionfc rotation, float x, float y, float z);

    default void applyScale(final Vector3dc scale) {
        this.applyScale(scale.x(), scale.y(), scale.z());
    }

    default void applyScale(final double xyz) {
        this.applyScale(xyz, xyz, xyz);
    }

    default void applyScale(final double x, final double y, final double z) {
        this.applyScale((float) x, (float) y, (float) z);
    }

    void applyScale(float x, float y, float z);

    default void setIdentity() {
        this.pose().setIdentity();
    }

    boolean isEmpty();

    void matrixPush();

    void matrixPop();

    PoseStack.Pose pose();

    default Matrix4f position() {
        return this.pose().pose();
    }

    default Matrix3f normal() {
        return this.pose().normal();
    }

    /**
     * @return This matrix stack as a vanilla {@link PoseStack}
     */
    PoseStack toPoseStack();
}
