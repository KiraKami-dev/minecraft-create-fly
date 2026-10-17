package dev.simulated_team.simulated.content.blocks.lasers.laser_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.zurrtum.create.content.redstone.DirectedDirectionalBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the laser sensor and optical sensor filters (Create Fly splits filtering into the server
 * {@code ServerFilteringBehaviour} and a client {@link FilteringBehaviour} that owns the slot).
 */
public final class LaserSensorFilterBehaviour {

    private LaserSensorFilterBehaviour() {
    }

    public static BlockEntityBehaviour<SmartBlockEntity> create(final SmartBlockEntity be) {
        return new FilteringBehaviour<>(be, new FilterValueBoxTransform());
    }

    /**
     * The filter sits on the top (or side, when mounted on the floor or ceiling) of a {@link DirectedDirectionalBlock}
     */
    public static class FilterValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return (switch (state.getValue(DirectedDirectionalBlock.TARGET)) {
                case FLOOR, CEILING -> state.getValue(DirectedDirectionalBlock.FACING);
                default -> Direction.UP;
            }).getAxis() == direction.getAxis();
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public void rotate(final BlockState state, final PoseStack ms) {
            super.rotate(state, ms);
            final Direction facing = state.getValue(DirectedDirectionalBlock.FACING);

            if (facing.getAxis() == Direction.Axis.Y)
                return;

            if (this.getSide() != Direction.UP)
                return;

            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }
}
