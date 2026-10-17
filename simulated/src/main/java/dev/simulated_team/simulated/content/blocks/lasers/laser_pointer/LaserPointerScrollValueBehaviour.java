package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the laser pointer's max length value box (the server half is a plain {@link ServerScrollValueBehaviour}).
 */
public class LaserPointerScrollValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public LaserPointerScrollValueBehaviour(final SmartBlockEntity be) {
        super(SimLang.translate("laser_pointer.max_length").component(), be, new RangeValueBoxTransform());
    }

    private static class RangeValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return state.getValue(LaserPointerBlock.FACING).getOpposite() == direction;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public void rotate(final BlockState state, final PoseStack ms) {
            super.rotate(state, ms);
            final Direction facing = state.getValue(LaserPointerBlock.FACING);

            if (facing.getAxis() == Direction.Axis.Y)
                return;

            if (this.getSide() != Direction.UP)
                return;

            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }
}
