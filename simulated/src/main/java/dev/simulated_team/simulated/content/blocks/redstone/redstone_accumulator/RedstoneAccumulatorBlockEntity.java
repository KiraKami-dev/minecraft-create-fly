package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.serialization.Codec;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerBrassDiodeScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Optional;


public class RedstoneAccumulatorBlockEntity extends SmartBlockEntity implements ClipboardCloneable {
    /**
     * Server half of the input delay setting, the value box (label, slot, formatting) is
     * {@link RedstoneAccumulatorScrollValueBehaviour} on the client
     */
    protected ServerScrollValueBehaviour inputDelay;
    protected int delayTicks;
    protected int outputSignal;
    protected LerpedFloat lerpedState;

    public RedstoneAccumulatorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
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
        this.inputDelay = new ServerBrassDiodeScrollValueBehaviour(this);
        this.inputDelay.between(2, 60 * 20 * 60);
        this.inputDelay.setValue(10);
        this.inputDelay.withCallback(this::inputDelayChanged);
        behaviours.add(this.inputDelay);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null) return;

        //check behind power and update block accordingly
        final Direction facing = this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING);
        final boolean backSignal = this.getBlockState().getValue(RedstoneAccumulatorBlock.POWERED);
        final boolean sideSignal = this.getBlockState().getValue(RedstoneAccumulatorBlock.SIDE_POWERED);

        if(backSignal && sideSignal) return;
        if(!backSignal && !sideSignal) this.delayTicks = 0;

        int tempSignal = this.outputSignal;
        if (this.delayTicks == this.inputDelay.getValue()) {
            if (backSignal) {
                tempSignal++;
                this.delayTicks = 0;
            } else if (sideSignal) {
                tempSignal--;
                this.delayTicks = 0;
            }

            if (tempSignal != this.outputSignal) this.setOutputSignal(tempSignal);
        } else {
            this.delayTicks = Math.min(this.delayTicks + 1 , this.inputDelay.getValue());
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(facing), this.getBlockState().getBlock());
        }
        if(this.level.isClientSide()) this.lerpedState.tickChaser();
        this.lerpedState.chase(this.outputSignal, 0.4, LerpedFloat.Chaser.EXP);
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

    private void inputDelayChanged(final Integer integer) {
        this.sendData();
    }

    public void updateSignal() {
        this.sendData();
    }

    public void setOutputSignal(final int output){
        final boolean update = output != this.outputSignal;
        this.outputSignal = Mth.clamp(output, 0, 15);
        if (update) {
            this.updateFacingBlock(this.getBlockState().getBlock(), this.level);
        }
    }

    private void updateFacingBlock(final Block block, final Level levelIn) {
        levelIn.updateNeighborsAt(this.worldPosition, block);
        levelIn.updateNeighborsAt(this.worldPosition.relative(this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING).getOpposite()), block);
    }

    /**
     * @return The lerped output signal, for rendering
     */
    public float getLerpedState(final float partialTicks) {
        return this.lerpedState.getValue(partialTicks);
    }

    public int getOutputSignal() {
        return this.outputSignal;
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
            if (blockState.getValue(RedstoneAccumulatorBlock.INVERTED) != inverted.get()) {
                this.level.setBlockAndUpdate(this.worldPosition, blockState.cycle(RedstoneAccumulatorBlock.INVERTED));
            }

            return true;
        }
    }

    @Override
    public boolean writeToClipboard(final ValueOutput view, final Direction direction) {
        view.store("Inverted", Codec.BOOL, this.getBlockState().getValueOrElse(RedstoneAccumulatorBlock.INVERTED, false));
        return true;
    }
}
