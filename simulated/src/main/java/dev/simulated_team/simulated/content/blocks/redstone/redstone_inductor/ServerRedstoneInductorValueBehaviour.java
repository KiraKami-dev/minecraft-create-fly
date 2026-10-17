package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerBrassDiodeScrollValueBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Server half of the inductor's interval setting (the client half is {@link RedstoneInductorValueBehaviour}).
 * Unlike the brass diodes, the first row starts at 0 ticks, which copies/inverts the input directly.
 */
public class ServerRedstoneInductorValueBehaviour extends ServerBrassDiodeScrollValueBehaviour {

    public ServerRedstoneInductorValueBehaviour(final SmartBlockEntity be) {
        super(be);
    }

    @Override
    public void onShortInteract(final Player player, final InteractionHand hand, final Direction side, final BlockHitResult hitResult) {
        final BlockState blockState = this.blockEntity.getBlockState();
        if (blockState.getBlock() instanceof final RedstoneInductorBlock bdb)
            bdb.toggle(this.getLevel(), this.getPos(), blockState, player, hand);
    }

    @Override
    public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlHeld) {
        final int value = valueSetting.value();
        final int multiplier = switch (valueSetting.row()) {
            case 0 -> 1;
            case 1 -> 20;
            default -> 60 * 20;
        };

        if (!valueSetting.equals(this.getValueSettings()))
            this.playFeedbackSound(this);

        final int clampingValue = valueSetting.row() == 0 ? 0 : 1;
        this.setValue(Math.max(clampingValue, Math.max(clampingValue, value) * multiplier));
    }
}
