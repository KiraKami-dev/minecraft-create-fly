package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the accumulator's input delay value box (label, slot and formatting). Mirrors Create Fly's
 * {@code BrassDiodeScrollValueBehaviour}, which hard-codes the brass diode label and slot.
 */
public class RedstoneAccumulatorScrollValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public RedstoneAccumulatorScrollValueBehaviour(final SmartBlockEntity be) {
        super(Component.translatable("block.simulated.redstone_accumulator.input_delay"), be, new RedstoneAccumulatorValueBoxTransform());
        this.withFormatter(RedstoneAccumulatorScrollValueBehaviour::format);
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                this.label,
                60,
                10,
                CreateLang.translatedOptions("generic.unit", "ticks", "seconds", "minutes"),
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    public MutableComponent formatSettings(final ValueSettings settings) {
        final int value = Math.max(1, settings.value());
        return Component.literal(switch (settings.row()) {
            case 0 -> Math.max(2, value) + "t";
            case 1 -> "0:" + (value < 10 ? "0" : "") + value;
            default -> value + ":00";
        });
    }

    private static String format(final int value) {
        if (value < 20)
            return value + "t";
        if (value < 20 * 60)
            return (value / 20) + "s";
        return (value / 20 / 60) + "m";
    }

    private static class RedstoneAccumulatorValueBoxTransform extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(final BlockState blockState) {
            return new Vec3(0.5, 6.6f / 16.0f, 0.5);
        }

        @Override
        public void rotate(final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(blockState.getValue(RedstoneAccumulatorBlock.FACING)) + 180;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }
    }
}
