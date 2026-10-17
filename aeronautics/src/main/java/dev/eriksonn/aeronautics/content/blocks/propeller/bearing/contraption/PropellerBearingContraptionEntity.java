package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.ControlledContraptionEntity;
import com.zurrtum.create.content.contraptions.IControlContraption;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.simulated_team.simulated.util.SimMathUtils;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import static com.zurrtum.create.catnip.math.AngleHelper.angleLerp;

/**
 * The client-side local transform (upstream {@code applyLocalTransforms}) lives in
 * {@link PropellerBearingContraptionEntityRenderer#applyLocalTransforms}, used by the renderer and
 * {@link PropellerBearingContraptionVisual}.
 */
public class PropellerBearingContraptionEntity extends ControlledContraptionEntity {

    public Quaternionf tiltQuat = new Quaternionf();
    public Quaternionf previousTiltQuat = new Quaternionf();
    public Direction direction = Direction.UP;

    public PropellerBearingContraptionEntity(final EntityType<? extends ControlledContraptionEntity> type, final Level world) {
        super(type, world);
    }

    public static ControlledContraptionEntity create(final Level world, final IControlContraption controller,
                                                     final Contraption contraption) {
        final PropellerBearingContraptionEntity entity =
                new PropellerBearingContraptionEntity(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION, world);
        entity.setControllerPos(controller.getBlockPosition());
        entity.setContraption(contraption);
        return entity;
    }

    public PropellerBearingBlockEntity getBearingEntity() {
        if (this.controllerPos == null)
            return null;
        if (!this.level().isLoaded(this.controllerPos))
            return null;
        final BlockEntity te = this.level().getBlockEntity(this.controllerPos);
        if (!(te instanceof PropellerBearingBlockEntity))
            return null;
        return (PropellerBearingBlockEntity) te;
    }

    Quaternionf interpolatedQuat = new Quaternionf();

    Quaternionf getInterpolatedQuat(final float partialTick) {
        return this.previousTiltQuat.slerp(this.tiltQuat, partialTick, this.interpolatedQuat);
    }

    @Override
    public Vec3 applyRotation(Vec3 localPos, final float partialTicks) {
        localPos = VecHelper.rotate(localPos, this.getAngle(partialTicks), this.rotationAxis);
        localPos = SimMathUtils.rotateQuatReverse(localPos, this.getInterpolatedQuat(partialTicks));
        return localPos;
    }

    @Override
    public Vec3 reverseRotation(Vec3 localPos, final float partialTicks) {
        localPos = SimMathUtils.rotateQuat(localPos, this.getInterpolatedQuat(partialTicks));
        localPos = VecHelper.rotate(localPos, -this.getAngle(partialTicks), this.rotationAxis);
        return localPos;
    }

    @Override
    public float getAngle(final float partialTicks) {
        final IControlContraption controller = this.getController();
        if (controller instanceof final PropellerBearingBlockEntity tile) {
            if (tile.disassemblySlowdown) {
                return tile.getInterpolatedAngle(partialTicks - 1);
            }
        }

        return partialTicks == 1.0F ? this.angle : angleLerp(partialTicks, this.prevAngle, this.angle);
    }

    public void setControllerPos(final BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    @Override
    public void setContraption(final Contraption contraption) {
        super.setContraption(contraption);
    }
}
