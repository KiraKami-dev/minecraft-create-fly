package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.serialization.Codec;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Optional;

public class RedstoneInductorBlockEntity extends SmartBlockEntity implements ClipboardCloneable {

    /**
     * Server half of the interval setting, the value box is {@link RedstoneInductorValueBehaviour} on the client
     */
    protected ServerScrollValueBehaviour inputDelay;
    int delayTicks;
    int outputSignal;
    LerpedFloat lerpedState;

    public RedstoneInductorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.lerpedState = LerpedFloat.linear();
        this.delayTicks = 0;
        this.outputSignal = 0;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.updateSignal();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        this.inputDelay = new ServerRedstoneInductorValueBehaviour(this);
        this.inputDelay.between(0, 60 * 20 * 60);
        this.inputDelay.setValue(10);
        this.inputDelay.withCallback(this::inputDelayChanged);
        behaviours.add(this.inputDelay);
    }

    private void inputDelayChanged(final Integer integer) {
        this.updateSignal();
    }

    public void updateSignal() {
        this.updateFacingBlock((RedstoneInductorBlock) this.getBlockState().getBlock(), this.getLevel());
        this.notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null) {
            return;
        }

        final RedstoneInductorBlock block = (RedstoneInductorBlock) this.getBlockState().getBlock();

        final int backSignal = block.getBackSignal(this.level, this.worldPosition, this.getBlockState());
        int tempPower = this.outputSignal;

        final boolean powered = this.getBlockState().getValue(RedstoneInductorBlock.POWERED);

        if (!this.level.isClientSide()) {
            if (tempPower == 0 && !powered) {
                this.delayTicks = 0;
                // setDischarge(false, level);
            }

            if (this.inputDelay.getValue() != 0 && this.delayTicks >= this.inputDelay.getValue()) {
                this.delayTicks = 0;

                if (tempPower > backSignal) tempPower--;
                if (tempPower < backSignal) tempPower++;

                // setDischarge(outputSignal > tempPower, level);

            } else if (this.inputDelay.getValue() == 0 && this.delayTicks > 2) {
                this.delayTicks = 0;
                tempPower = backSignal;
            }

            this.delayTicks++;
            if(this.outputSignal != tempPower) {
                this.outputSignal = tempPower;
                this.updateFacingBlock(block, this.level);
                this.sendData();
            }
        }

        if(this.level.isClientSide()) this.lerpedState.tickChaser();
    }

    private void updateFacingBlock(final RedstoneInductorBlock block, final Level levelIn) {
        if (levelIn == null) {
            return;
        }
        levelIn.updateNeighborsAt(this.worldPosition, block);
        levelIn.updateNeighborsAt(this.worldPosition.relative(this.getBlockState().getValue(RedstoneInductorBlock.FACING).getOpposite()), block);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        this.outputSignal = view.getIntOr("OutputSignal", 0);
        this.delayTicks = view.getIntOr("DelayTicks", 0);
        this.lerpedState.chase(this.outputSignal, 0.4, LerpedFloat.Chaser.EXP);
        super.read(view, clientPacket);
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        view.putInt("OutputSignal", this.outputSignal);
        view.putInt("DelayTicks", this.delayTicks);
        super.write(view, clientPacket);
    }

    /**
     * @return The lerped output signal, for rendering
     */
    public float getLerpedState(final float partialTicks) {
        return this.lerpedState.getValue(partialTicks);
    }

    @Override
    public String getClipboardKey() {
        return "Block";
    }

    @Override
    public boolean readFromClipboard(final ValueInput view, final Player player, final Direction direction, final boolean simulate) {
        final Optional<Boolean> inverted = view.read("Inverted", Codec.BOOL);
        if (inverted.isEmpty()) {
            return false;
        } else if (simulate) {
            return true;
        } else {
            final BlockState blockState = this.getBlockState();
            if (blockState.getValue(RedstoneInductorBlock.INVERTED) != inverted.get()) {
                this.level.setBlockAndUpdate(this.worldPosition, blockState.cycle(RedstoneInductorBlock.INVERTED));
            }

            return true;
        }
    }

    @Override
    public boolean writeToClipboard(final ValueOutput view, final Direction direction) {
        view.store("Inverted", Codec.BOOL, this.getBlockState().getValueOrElse(RedstoneInductorBlock.INVERTED, false));
        return true;
    }
}
