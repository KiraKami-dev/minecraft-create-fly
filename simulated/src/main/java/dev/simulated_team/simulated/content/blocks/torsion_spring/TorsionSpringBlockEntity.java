package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.zurrtum.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * The angle limit value box (client half) is {@link TorsionSpringScrollValueBehaviour}.
 */
public class TorsionSpringBlockEntity extends KineticBlockEntity implements ExtraKinetics {
    private final Output springOutput;
    public ServerScrollValueBehaviour angleInput;
    protected double sequencedAngleLimit;

    public TorsionSpringBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos blockPos, final BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.springOutput = new Output(blockEntityType, new ExtraBlockPos(blockPos), blockState, this);
        this.sequencedAngleLimit = -1;
    }

    public boolean isSpringStatic() {
        return this.springOutput.angle == this.springOutput.oldAngle;
    }

    public float interpolatedSpring(final float pt) {
        return (float) (this.springOutput.oldAngle + (this.springOutput.angle - this.springOutput.oldAngle) * pt);
    }

    public float getAngle() {
        return (float) this.springOutput.angle;
    }

    public void setAngle(final float angle) {
        this.springOutput.angle = angle;
    }

    public void onSignalChanged() {

    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        super.addBehaviours(behaviours);
        this.angleInput = new ServerScrollValueBehaviour(this).between(1, 360);
        behaviours.add(this.angleInput);
        this.angleInput.setValue(90);
    }

    /**
     * Whether the angle limit value box is shown (client side, {@link TorsionSpringScrollValueBehaviour})
     */
    public boolean showValue() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.springOutput.tick();
    }

    @Override
    public void onSpeedChanged(final float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        this.sequencedAngleLimit = -1;

        if (this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE)
            this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue(this.getTheoreticalSpeed());

        this.springOutput.updateParentSpeed(previousSpeed, this.getSpeed());
    }

    @Override
    protected void copySequenceContextFrom(final KineticBlockEntity sourceBE) {
        super.copySequenceContextFrom(sourceBE);
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 0;
    }

    @Override
    public String getExtraKineticsSaveName() {
        return "TorsionSpringOutput";
    }

    @Override
    public KineticBlockEntity getExtraKinetics() {
        return this.springOutput;
    }

    @Override
    public boolean shouldConnectExtraKinetics() {
        return false;
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        if (this.sequencedAngleLimit >= 0)
            view.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);
        this.sequencedAngleLimit = view.getDoubleOr("SequencedAngleLimit", -1);
    }

    public static class Output extends GeneratingKineticBlockEntity implements ExtraKineticsBlockEntity {
        public static final IRotate CONFIG = new IRotate() {
            @Override
            public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
                return face == state.getValue(TorsionSpringBlock.FACING);
            }

            @Override
            public Direction.Axis getRotationAxis(final BlockState state) {
                return state.getValue(TorsionSpringBlock.FACING).getAxis();
            }
        };

        private final TorsionSpringBlockEntity parent;

        protected double oldAngle = 0.0f;
        protected double angle = 0.0f;

        private int rotationDurationTicks = 0; // duration of the currently active context.
        private int rotationProgressTicks = 0; // progress of our movement, always less than context duration
        private double sequencedAngleLimit = -1;

        /**
         * The previous, non-zero spring input speed
         */
        private float lastSpringSpeed = 0;

        private float generatedSpeed;
        private double targetAngle = 0;
        private State currentState = State.STOPPED;
        private float queuedSpeed;
        private int customValidationCountdown;

        public Output(final BlockEntityType<?> type, final ExtraBlockPos pos, final BlockState state, final TorsionSpringBlockEntity parentBlockEntity) {
            super(type, pos, state);
            this.parent = parentBlockEntity;
        }

        @Override
        public Component getKey() {
            // SimLang (catnip LangBuilder) is client-only on Create Fly
            return Component.translatable("simulated.extra_kinetics.torsion_output");
        }

        @Override
        public void initialize() {
            super.initialize();
            this.reActivateSource = true;
            this.updateSpeed = true;
        }

        @Override
        public void tick() {
            // The built in kinetic BE validation will cook us (when our speed is not zero, but our generated speed is 0
            // on the tick we are stopping rotation)
            // Let's run a custom one instead
            ((KineticBlockEntityExtension) this).simulated$setValidationCountdown(Integer.MAX_VALUE);

            if (this.customValidationCountdown-- <= 0) {
                this.customValidationCountdown = AllConfigs.server().kinetics.kineticValidationFrequency.get();
                this.customValidateKinetics();
            }

            this.generatedSpeed = this.queuedSpeed;

            super.tick();

            this.oldAngle = this.angle;
            if (this.rotationDurationTicks >= 0 && this.rotationProgressTicks <= this.rotationDurationTicks) {
                this.rotationProgressTicks++;

                float angularSpeed = KineticBlockEntity.convertToAngular(this.speed);

                if (this.sequencedAngleLimit >= 0)
                    angularSpeed = (float) Mth.clamp(angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);

                if (this.sequencedAngleLimit >= 0)
                    this.sequencedAngleLimit = Math.max(0, this.sequencedAngleLimit - Math.abs(angularSpeed));

                this.angle += angularSpeed;

                this.level.updateNeighborsAt(this.getBlockPos(), this.getParentBlockEntity().getBlockState().getBlock());

                if (this.rotationProgressTicks == this.rotationDurationTicks) {
                    this.sequenceContext = null;
                    this.rotationProgressTicks = -1;
                    this.rotationDurationTicks = -1;
                    this.queuedSpeed = 0;

                    this.reActivateSource = true;
                    this.updateSpeed = true;
                    this.currentState = State.STOPPED;
                }
            }

            final boolean powered = this.getBlockState().getValue(TorsionSpringBlock.POWERED);
            final boolean parentStopped = this.parent.getSpeed() == 0;

            if (this.currentState == State.TURNING && parentStopped) {
                if (this.targetAngle != 0 || powered)
                    this.stopTurning();
            } else if (this.currentState == State.STOPPED && parentStopped && !powered) {
                if (this.targetAngle != 0.0) { // return back to 0 angle when not powered
                    this.beginTurnTo(0.0);
                    SimAdvancements.REWIND_TIME.awardToNearby(this.parent.getBlockPos(), this.parent.getLevel());
                }
            } else if (this.currentState == State.TURNING) {
                // if the parent speed changes direction, stop turning so next tick
                final double targetAngle = this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed());

                if (this.targetAngle != targetAngle || this.lastSpringSpeed != this.generatedSpeed) {
                    this.stopTurning();
                }
            } else if (!parentStopped && this.currentState == State.STOPPED) {
                // start rotating if the parent is, and we're stopped
                final double targetAngle = this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed());
                this.beginTurnTo(targetAngle);
            }
        }

        private void customValidateKinetics() {
            if (this.hasSource()) {
                if (!this.hasNetwork()) {
                    this.removeSource();
                    return;
                }

                if (!this.level.isLoaded(this.source))
                    return;

                BlockEntity blockEntity = this.level.getBlockEntity(this.source);

                if (blockEntity instanceof final ExtraKinetics ek && ((KineticBlockEntityExtension) this).simulated$getConnectedToExtraKinetics()) {
                    blockEntity = ek.getExtraKinetics();
                }

                final KineticBlockEntity sourceBE =
                        blockEntity instanceof KineticBlockEntity ? (KineticBlockEntity) blockEntity : null;
                if (sourceBE == null || sourceBE.getTheoreticalSpeed() == 0) {
                    this.removeSource();
                    this.detachKinetics();
                }
            }
        }

        private void updateParentSpeed(final float previousSpeed, final float newParentSpeed) {
            if (newParentSpeed != 0) {
                this.lastSpringSpeed = newParentSpeed;
            } else if (previousSpeed != 0) {
                this.lastSpringSpeed = previousSpeed;
            }
        }

        private void stopTurning() {
            this.sequenceContext = null;
            this.rotationProgressTicks = -1;
            this.rotationDurationTicks = -1;
            this.sequencedAngleLimit = -1;
            this.targetAngle = Double.MAX_VALUE;

            this.reActivateSource = true;
            this.updateSpeed = true;
            this.queuedSpeed = 0;

            this.currentState = State.STOPPED;
        }

        private void beginTurnTo(final double targetAngle) {
            double relativeAngle = targetAngle - this.angle;

            if (relativeAngle == 0) return;
            if (this.currentState == State.TURNING && this.targetAngle == targetAngle) return;

            this.lastSpringSpeed = (float) (Math.abs(this.lastSpringSpeed) * Math.signum(relativeAngle));

            if (this.parent.sequencedAngleLimit >= 0) {
                relativeAngle = (float) Mth.clamp(relativeAngle, -this.parent.sequencedAngleLimit, this.parent.sequencedAngleLimit);
            }

            this.detachKinetics();
            this.targetAngle = targetAngle;
            this.sequenceContext = new SequencedGearshiftBlockEntity.SequenceContext(SequencerInstructions.TURN_ANGLE, relativeAngle / this.lastSpringSpeed);

            final double degreesPerTick = KineticBlockEntity.convertToAngular(Math.abs(this.lastSpringSpeed));
            this.rotationDurationTicks = (int) Math.ceil(Math.abs(relativeAngle) / degreesPerTick) + 2;
            this.rotationProgressTicks = 0;
            this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue(this.lastSpringSpeed);
            this.currentState = State.TURNING;
            this.queuedSpeed = this.lastSpringSpeed;
            this.generatedSpeed = this.queuedSpeed;

            this.reActivateSource = true;
            this.updateSpeed = true;
        }

        @Override
        public float getGeneratedSpeed() {
            return this.generatedSpeed;
        }

        @Override
        public float calculateStressApplied() {
            return 0;
        }

        @Override
        protected void write(final ValueOutput view, final boolean clientPacket) {
            super.write(view, clientPacket);
            view.putDouble("OldAngle", this.oldAngle);
            view.putDouble("Angle", this.angle);
            view.putDouble("TargetAngle", this.targetAngle);
            view.putFloat("LastSpringSpeed", this.lastSpringSpeed);
            view.putInt("CurrentState", this.currentState.ordinal());
            view.putInt("RotationProgressTicks", this.rotationProgressTicks);
            view.putInt("RotationDurationTicks", this.rotationDurationTicks);
            view.putFloat("GeneratedSpeed", this.generatedSpeed);
            view.putFloat("QueuedSpeed", this.queuedSpeed);

            if (this.sequencedAngleLimit >= 0)
                view.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
        }

        @Override
        protected void read(final ValueInput view, final boolean clientPacket) {
            super.read(view, clientPacket);
            this.oldAngle = view.getDoubleOr("OldAngle", 0);
            this.angle = view.getDoubleOr("Angle", 0);
            this.targetAngle = view.getDoubleOr("TargetAngle", 0);
            this.lastSpringSpeed = view.getFloatOr("LastSpringSpeed", 0);
            this.sequencedAngleLimit = view.getDoubleOr("SequencedAngleLimit", -1);
            this.rotationProgressTicks = view.getIntOr("RotationProgressTicks", 0);
            this.rotationDurationTicks = view.getIntOr("RotationDurationTicks", 0);
            this.generatedSpeed = view.getFloatOr("GeneratedSpeed", 0);
            this.queuedSpeed = view.getFloatOr("QueuedSpeed", 0);

            view.getInt("CurrentState").ifPresent(state -> this.currentState = State.values()[state]);
        }

        @Override
        public KineticBlockEntity getParentBlockEntity() {
            return this.parent;
        }

        private enum State {
            STOPPED,
            TURNING
        }
    }
}
