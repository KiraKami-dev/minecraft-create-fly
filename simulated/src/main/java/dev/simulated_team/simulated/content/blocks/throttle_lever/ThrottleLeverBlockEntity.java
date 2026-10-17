package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Goggle information is added client side by {@link ThrottleLeverTooltipBehaviour}.
 */
public class ThrottleLeverBlockEntity extends SmartBlockEntity {
    protected int state = 0;
    protected int lastChange;
    protected LerpedFloat clientAngle;
    public final LerpedFloat clientPressedLerp = LerpedFloat.linear().chase(0, 0.45, LerpedFloat.Chaser.EXP);
    // SimLang (catnip LangBuilder) is client-only on Create Fly
    private static final MutableComponent HOLD_TIP = Component.translatable("simulated.gui.hold_tip.hold_to_adjust");

    public ThrottleLeverBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.clientAngle = LerpedFloat.linear();
    }

    @Override
    public void write(final ValueOutput view, final boolean clientPacket) {
        view.putInt("State", this.state);
        view.putInt("ChangeTimer", this.lastChange);
        super.write(view, clientPacket);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        this.state = view.getIntOr("State", 0);
        this.lastChange = view.getIntOr("ChangeTimer", 0);
        this.clientAngle.chase(this.getBlockState().getValue(ThrottleLeverBlock.INVERTED) ? 15 - this.state : this.state, 0.5f, LerpedFloat.Chaser.EXP);
        super.read(view, clientPacket);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.lastChange > 0) {
            this.lastChange--;
            if (this.lastChange == 0)
                this.updateOutput();
        }

        if (this.level.isClientSide()) {
            this.clientAngle.tickChaser();
            final boolean pressed = SimClickInteractions.THROTTLE_LEVER_MANAGER.isBlockActive(this.getBlockPos());
            this.clientPressedLerp.updateChaseTarget(pressed ? 1 : 0);
            this.clientPressedLerp.tickChaser();

            ThrottleLeverClientGripHandler.tickGrip(this);
        }
    }

    @Override
    public void initialize() {
        super.initialize();

    }

    private void updateOutput() {
        ThrottleLeverBlock.updateNeighbors(this.getBlockState(), this.level, this.worldPosition);
    }

    /**
     * Upstream ThrottleLeverBlock#onRemove: let the neighbours know the signal is gone. On 1.21.11 the block entity is
     * removed before Block#affectNeighborsAfterRemoval runs, so this happens here.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.state != 0 && this.level != null)
            ThrottleLeverBlock.updateNeighbors(oldState, this.level, pos);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        behaviours.add(new HoldTipBehaviour(this, HOLD_TIP));
    }

    public void changeState(final boolean back) {
        final int prevState = this.state;
        this.state += back ? -1 : 1;
        this.state = Mth.clamp(this.state, 0, 15);
        if (prevState != this.state)
            this.lastChange = 15;
        this.sendData();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return AABB.ofSize(this.getBlockPos().getCenter(), 1.5, 1.5, 1.5);
    }

    public int getState() {
        return this.state;
    }

    public void setSignal(final int signal) {
        this.state = this.getBlockState().getValue(ThrottleLeverBlock.INVERTED) ? 15 - signal : signal;
        this.lastChange = 2;
        this.level.playSound(null, this.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.2F, 0.25F + (float)(signal + 5) / 15.0F * 0.5F);
        this.sendData();
    }
}
