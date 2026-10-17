package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.Create;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the gimbal sensor's angle limit value box; the server half is
 * {@link GimbalSensorBlockEntity.GimbalSensorScrollValueBehaviour}.
 */
public class GimbalSensorValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public GimbalSensorValueBehaviour(final GimbalSensorBlockEntity be) {
        super(Component.translatable("create.kinetics.valve_handle.rotated_angle"), be, new GimbalSensorValueBox(be));
        this.withFormatter(v -> Math.abs(v) + Component.translatable("create.generic.unit.degrees")
                .getString());
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        final ImmutableList<Component> rows = ImmutableList.of(Component.literal("⟳") // ⟳
                        .withStyle(ChatFormatting.BOLD),
                Component.literal("⟲") // ⟲
                        .withStyle(ChatFormatting.BOLD));
        return new ValueSettingsBoard(this.label, 90, 15, rows, new ValueSettingsFormatter(this::formatValue));
    }

    public MutableComponent formatValue(final ValueSettings settings) {
        return SimLang.number(Math.max(0, Math.abs(settings.value())))
                .add(Component.translatable(Create.MOD_ID + "." + "generic.unit.degrees"))
                .component();
    }

    public static class GimbalSensorValueBox extends ValueBoxTransform.Sided {
        GimbalSensorBlockEntity be;

        public GimbalSensorValueBox(final GimbalSensorBlockEntity be) {
            this.be = be;
        }

        @Override
        public void fromSide(final Direction direction) {
            this.direction = direction;
            this.be.axisBehaviour.setLastSide(direction);
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction.getAxis().isHorizontal();
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(state);

            if (offset == null)
                return false;

            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

    }

    public static class DualGimbalSensorValueBox extends ValueBoxTransform.Dual {

        protected Direction direction = Direction.UP;

        public DualGimbalSensorValueBox(final boolean first) {
            super(first);
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            Vec3 location = this.getSouthLocation();
            location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(this.getSide()), Direction.Axis.Y);
            location = VecHelper.rotateCentered(location, AngleHelper.verticalAngle(this.getSide()), Direction.Axis.X);
            return location;
        }


        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16);
        }

        @Override
        public void rotate(final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180;
            final float xRot = this.getSide() == Direction.UP ? 90 : this.getSide() == Direction.DOWN ? 270 : 0;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(xRot);
        }

        @Override
        public boolean shouldRender(final BlockState state) {
            return super.shouldRender(state) && this.isSideActive(state, this.getSide());
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

        protected boolean isSideActive(final BlockState state, final Direction direction) {
            final boolean a = (direction.getAxis() == state.getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
            return direction.getAxis().isHorizontal() && (a == this.first);
        }

        public Direction getSide() {
            return Direction.NORTH;
        }
    }
}
