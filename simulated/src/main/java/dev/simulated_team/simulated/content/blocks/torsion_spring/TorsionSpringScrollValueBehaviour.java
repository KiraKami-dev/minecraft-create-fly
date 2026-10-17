package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
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
 * Client half of the torsion spring angle limit (value box, board and formatting). The value itself is
 * {@link TorsionSpringBlockEntity#angleInput}.
 */
public class TorsionSpringScrollValueBehaviour extends ScrollValueBehaviour<TorsionSpringBlockEntity, ServerScrollValueBehaviour> {

    public TorsionSpringScrollValueBehaviour(final TorsionSpringBlockEntity be) {
        super(SimLang.translate("torsion_spring.angle_limit").component(), be, new TorsionSpringValueBox());
        this.withFormatter(v -> Math.max(1, v) + CreateLang.translateDirect("generic.unit.degrees")
                .getString());
        this.onlyActiveWhen(be::showValue);
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        return new ValueSettingsBoard(this.label, 360, 45, ImmutableList.of(Component.literal("⟳").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue));
    }

    public MutableComponent formatValue(final ValueSettings settings) {
        return SimLang.number(Math.max(1, settings.value()))
                .add(CreateLang.translateDirect("generic.unit.degrees"))
                .component();
    }

    public static class TorsionSpringValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            return super.getLocalOffset(state)
                    .add(Vec3.atLowerCornerOf(state.getValue(TorsionSpringBlock.FACING).getUnitVec3i()).scale(-5 / 16f));
        }

        @Override
        public void rotate(final BlockState state, final PoseStack ms) {
            if (!this.getSide().getAxis().isHorizontal()) {
                TransformStack.of(ms)
                        .rotateY((AngleHelper.horizontalAngle(state.getValue(TorsionSpringBlock.FACING)) + 180) * (float) Math.PI / 180);
            }
            super.rotate(state, ms);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(state);
            if (offset == null) {
                return false;
            }
            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction.getAxis() != state.getValue(TorsionSpringBlock.FACING).getAxis();
        }
    }
}
