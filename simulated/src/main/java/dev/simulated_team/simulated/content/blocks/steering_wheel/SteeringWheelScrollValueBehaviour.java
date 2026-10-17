package dev.simulated_team.simulated.content.blocks.steering_wheel;

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
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the steering wheel angle limit (value box, board and formatting). The value itself is
 * {@link SteeringWheelBlockEntity#angleInput}.
 */
public class SteeringWheelScrollValueBehaviour extends ScrollValueBehaviour<SteeringWheelBlockEntity, ServerScrollValueBehaviour> {
    public SteeringWheelScrollValueBehaviour(final SteeringWheelBlockEntity be) {
        super(SimLang.translate("torsion_spring.angle_limit").component(), be, new SteeringWheelValueBoxTransform());
        this.withFormatter(v -> Math.abs(v) + CreateLang.translateDirect("generic.unit.degrees")
                .getString());
    }

    /**
     * @return whether the given hit location targets the angle limit value box of this steering wheel
     */
    public static boolean testHit(final SmartBlockEntity be, final Vec3 hit) {
        final ScrollValueBehaviour<?, ?> behaviour = be.getBehaviour(ScrollValueBehaviour.TYPE);
        return behaviour != null && behaviour.testHit(hit);
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        return new ValueSettingsBoard(this.label, 360, 45, ImmutableList.of(Component.literal("⟳").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue));
    }

    public MutableComponent formatValue(final ValueSettings settings) {
        return SimLang.number(Math.max(1, Math.abs(settings.value())))
                .add(CreateLang.translateDirect("generic.unit.degrees"))
                .component();
    }

    public static class SteeringWheelValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction == (state.getValue(SteeringWheelBlock.ON_FLOOR) ? Direction.UP : Direction.DOWN);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public void rotate(final BlockState state, final PoseStack ms) {
            super.rotate(state, ms);
            final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }
}
