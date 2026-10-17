package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Client: slot of the rotation direction value box of the portable engine.
 */
public class PortableEngineValueBoxTransform extends ValueBoxTransform {

    @Override
    public Vec3 getLocalOffset(final BlockState blockState) {
        final Direction facing = blockState.getValue(PortableEngineBlock.HORIZONTAL_FACING);
        final float yRot = AngleHelper.horizontalAngle(facing);
        return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 13.5f, 7.4f), yRot, Direction.Axis.Y);
    }

    @Override
    public void rotate(final BlockState blockState, final PoseStack poseStack) {
        final float yRot = AngleHelper.horizontalAngle(blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        TransformStack.of(poseStack)
                .rotateYDegrees(yRot)
                .rotateXDegrees(90)
                .translate(0, 0.1, 0);
    }

}
