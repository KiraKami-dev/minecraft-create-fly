package dev.ryanhcode.offroad.content.blocks.wheel_mount;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.content.contraptions.actors.roller.RollerBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.ryanhcode.offroad.data.OffroadLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the wheel mount's suspension strength value box (label, slot and board); the server half is the
 * {@link ServerScrollValueBehaviour} added by {@link WheelMountBlockEntity}.
 */
public class WheelMountValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public WheelMountValueBehaviour(final SmartBlockEntity be) {
        super(OffroadLang.translate("scroll_option.suspension_strength").component(), be, new SuspensionStrengthValueBox(0));
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        return new ValueSettingsBoard(this.label, WheelMountBlockEntity.MAX_SUSPENSION_STRENGTH, 20, ImmutableList.of(OffroadLang.translate("scroll_option.suspension_strength_label").component()),
                new ValueSettingsFormatter());
    }

    private static final class SuspensionStrengthValueBox extends ValueBoxTransform {
        private final int hOffset;

        public SuspensionStrengthValueBox(final int hOffset) {
            this.hOffset = hOffset;
        }

        @Override
        public void rotate(final BlockState state, final PoseStack ms) {
            final Direction facing = state.getValue(RollerBlock.FACING);
            final float yRot = AngleHelper.horizontalAngle(facing) + 180;
            TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < this.scale / 3;
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            final Direction facing = state.getValue(RollerBlock.FACING);
            final float stateAngle = AngleHelper.horizontalAngle(facing) + 180;
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8 + this.hOffset, 15.5f, 11), stateAngle, Direction.Axis.Y);
        }
    }
}
