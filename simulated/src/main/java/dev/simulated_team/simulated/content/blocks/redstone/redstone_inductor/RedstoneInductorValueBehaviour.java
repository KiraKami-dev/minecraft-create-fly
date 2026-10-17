package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the inductor's interval value box (label, slot and formatting); the server half is
 * {@link ServerRedstoneInductorValueBehaviour}.
 */
public class RedstoneInductorValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public RedstoneInductorValueBehaviour(final SmartBlockEntity be) {
        super(CreateLang.translateDirect("logistics.redstone_interval"), be, new RedstoneInductorValueBoxTransform());
        this.withFormatter(this::format);
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
        final BlockState blockState = this.blockEntity.getBlockState();
        final Boolean inverted = blockState.getValue(RedstoneInductorBlock.INVERTED);

        final int row = settings.row();
        final int column = settings.value();
        if (row == 0 && column == 0) {
            return Component.translatable("block.simulated.redstone_inductor." + (inverted ? "invert" : "copy"));
        }

        return Component.literal(switch (settings.row()) {
            case 1 -> "0:" + (column < 10 ? "0" : "") + column;
            case 2 -> column + ":00";
            default -> column + "t";
        });
    }

    private String format(final int value) {
        if (value == 0) {
            return Component.translatable("block.simulated.redstone_inductor." +
                    (this.blockEntity.getBlockState().getValue(RedstoneInductorBlock.INVERTED) ? "invert" : "copy")).getString();
        }
        if (value <= 60) {
            return value + "t";
        }
        if (value < 20 * 60) {
            return (value / 20) + "s";
        }
        return (value / 20 / 60) + "m";
    }

    private static class RedstoneInductorValueBoxTransform extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(final BlockState blockState) {
            return new Vec3(0.5, 5.5f / 16.0f, 0.5);
        }

        @Override
        public void rotate(final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)) + 180;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }

        @Override
        public float getScale() {
            return 0.5f;
        }
    }
}
