package dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.contraptions;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.foundation.collision.Matrix3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.fabric.mixin.compatibility.create.contraptions.Matrix3dAccessor;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared logic of the redirects in {@code ContraptionColliderMixin} and {@code ContraptionColliderClientMixin}.
 * Create Fly splits Create's {@code ContraptionCollider#collideEntities} into a server half and a client half
 * ({@code ContraptionColliderClient}); both get the same sub-level transforms.
 */
public final class ContraptionColliderHelper {

    private ContraptionColliderHelper() {
    }

    public static org.joml.Matrix3d toJOML(final Matrix3d createMatrix) {
        final org.joml.Matrix3d jomlMatrix = new org.joml.Matrix3d();

        final Matrix3dAccessor accessor = ((Matrix3dAccessor) createMatrix);
        jomlMatrix.set(
                accessor.getM00(), accessor.getM01(), accessor.getM02(),
                accessor.getM10(), accessor.getM11(), accessor.getM12(),
                accessor.getM20(), accessor.getM21(), accessor.getM22()
        );

        return jomlMatrix;
    }

    public static Matrix3d toCreate(final org.joml.Matrix3d jomlMatrix) {
        final Matrix3d createMatrix = new Matrix3d();
        final Matrix3dAccessor accessor = ((Matrix3dAccessor) createMatrix);

        accessor.setM00(jomlMatrix.m00);
        accessor.setM01(jomlMatrix.m01);
        accessor.setM02(jomlMatrix.m02);

        accessor.setM10(jomlMatrix.m10);
        accessor.setM11(jomlMatrix.m11);
        accessor.setM12(jomlMatrix.m12);

        accessor.setM20(jomlMatrix.m20);
        accessor.setM21(jomlMatrix.m21);
        accessor.setM22(jomlMatrix.m22);

        return createMatrix;
    }

    public static AABB contraptionBounds(final AbstractContraptionEntity instance, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = Sable.HELPER.getContaining(instance);
        contraptionSubLevel.set(subLevel);

        if (subLevel != null) {
            final BoundingBox3d globalBB = new BoundingBox3d(instance.getBoundingBox());
            globalBB.transform(subLevel.logicalPose(), globalBB);
            return globalBB.toMojang();
        }

        return instance.getBoundingBox();
    }

    public static AABB entityQueryBounds(final AABB instance, final double d, final double e, final double f, final AbstractContraptionEntity contraption, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();

        if (subLevel != null) {
            final BoundingBox3d globalBB = new BoundingBox3d(contraption.getBoundingBox().inflate(2.0).expandTowards(d, e, f));
            globalBB.transform(subLevel.logicalPose(), globalBB);
            return globalBB.toMojang();
        }

        return instance.expandTowards(d, e, f);
    }

    public static Vec3 contraptionPosition(final AbstractContraptionEntity instance, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();

        if (subLevel != null) {
            return subLevel.logicalPose().transformPosition(instance.position());
        }

        return instance.position();
    }

    public static Vec3 getPrevPositionVec(final AbstractContraptionEntity instance, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();

        if (subLevel != null) {
            return subLevel.logicalPose().transformPosition(instance.getPrevPositionVec());
        }

        return instance.getPrevPositionVec();
    }

    public static Vec3 getAnchorVec(final AbstractContraptionEntity instance, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();

        if (subLevel != null) {
            return subLevel.logicalPose().transformPosition(instance.getAnchorVec().add(0.5, 0.5, 0.5)).subtract(0.5, 0.5, 0.5);
        }

        return instance.getAnchorVec();
    }

    public static Matrix3d rotationMatrix(final AbstractContraptionEntity.ContraptionRotationState rotationState, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();
        if (subLevel != null) {
            final Pose3d pose = subLevel.logicalPose();
            final org.joml.Matrix3d jomlMatrix = toJOML(rotationState.asMatrix());

            jomlMatrix.rotateLocal(pose.orientation());
            return toCreate(jomlMatrix);
        }

        return rotationState.asMatrix();
    }

    public static Vec3 toLocalVector(final AbstractContraptionEntity instance, final Vec3 localVec, final float partialTicks, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();

        if (subLevel != null) {
            final Pose3d pose = subLevel.logicalPose();
            return instance.toLocalVector(pose.transformPositionInverse(localVec), partialTicks);
        }

        return instance.toLocalVector(localVec, partialTicks);
    }

    public static Vec3 getContactPointMotion(final AbstractContraptionEntity instance, final Vec3 globalContactPoint, final LocalRef<SubLevel> contraptionSubLevel) {
        final SubLevel subLevel = contraptionSubLevel.get();
        if (subLevel != null) {
            final Pose3d pose = subLevel.logicalPose();
            final Vec3 localContactPoint = pose.transformPositionInverse(globalContactPoint);
            final Vec3 motion = pose.transformNormal(instance.getContactPointMotion(localContactPoint))
                    .add(globalContactPoint.subtract(subLevel.lastPose().transformPosition(localContactPoint)));
            return motion;
        }

        return instance.getContactPointMotion(globalContactPoint);
    }
}
