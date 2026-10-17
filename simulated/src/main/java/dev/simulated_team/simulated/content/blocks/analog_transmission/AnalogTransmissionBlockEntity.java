package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.simpleRelays.ICogWheel;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The parent BlockEntity class. implements {@link ExtraKinetics ExtraKinetics} to allow multi-kinetic functionality
 * <p>
 * The "too fast" goggle hint (upstream addToTooltip) is added client side by {@link AnalogTransmissionTooltipBehaviour}.
 */
public class AnalogTransmissionBlockEntity extends KineticBlockEntity implements ExtraKinetics {

    /**
     * The ExtraKinetic BlockEntity associated with the AnalogTransmission
     */
    private final AnalogTransmissionCogwheel extraWheel;

    private int signal = 0;


    /**
     * Set whenever the analog transmission disconnects due to overspeeding
     */
    private boolean oversaturated = false;
    boolean alreadySentEffects = false;

    public AnalogTransmissionBlockEntity(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state) {
        super(typeIn, pos, state);

        //set our ExtraKientic BlockEntity and set the proper BlockState
        this.extraWheel = new AnalogTransmissionCogwheel(typeIn, new ExtraBlockPos(pos), state, this);
    }

    /**
     * Required override, as we need our ExtraKinetic BlockEntity to tick
     */
    @Override
    public void tick() {
        final int bestNeighborSignal = this.getLevel().getBestNeighborSignal(this.getBlockPos());

        if (!this.getLevel().isClientSide()) {
            if (bestNeighborSignal != this.signal) {
                //detach our own network, and our ExtraKinetic's
                this.detachKinetics();
                this.extraWheel.detachKinetics();

                //Remove the sources
                this.removeSource();
                this.extraWheel.removeSource();

                this.signal = bestNeighborSignal;
                this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AnalogTransmissionBlock.POWERED, this.signal > 0));

                //Depending on if we are connected to the ExtraKinetic BlockEntity, or vise versa, we need to attach kinetics accordingly
                if (((KineticBlockEntityExtension) this).simulated$getConnectedToExtraKinetics()) {//Attach ours, then ExtraKientic's
                    this.attachKinetics();
                    this.extraWheel.attachKinetics();
                } else { //Attach ExtraKinetic's, then ours
                    this.extraWheel.attachKinetics();
                    this.attachKinetics();
                }
            }
        } else if (this.oversaturated) {
            if (!this.alreadySentEffects) {
                this.alreadySentEffects = true;
                this.effects.triggerOverStressedEffect();
            }
        } else {
            this.alreadySentEffects = false;
        }

        this.extraWheel.tick();
        super.tick();
    }

    @VisibleForTesting
    public float getRotationModifier() {
        return 1 - (this.signal + 1) / 16f;
    }

    /**
     * This propagateRotationTo handles both the AnalogTransmission's modifier towards the ExtraKientic BlockEntity, and vise versa
     */
    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        float gatheredRotationModifier = 0;
        if (this.signal != 15) {
            if (target == this.extraWheel) { //reduce speed
                gatheredRotationModifier = this.signal == 0 ? 1 : this.getRotationModifier();
                if (this.oversaturated) {
                    return 0;
                }
            } else if (target == this) { //increase speed
                gatheredRotationModifier = this.signal == 0 ? 1 : (1 / this.getRotationModifier());

                if (Math.abs(this.extraWheel.getTheoreticalSpeed() * gatheredRotationModifier) > AllConfigs.server().kinetics.maxRotationSpeed.get()) {
                    this.oversaturated = true;
                    return 0;
                } else {
                    this.oversaturated = false;
                }
            }
        } else {
            this.oversaturated = false;
        }

        return gatheredRotationModifier;
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        view.putInt("Signal", this.signal);
        view.putBoolean("Oversaturated", this.oversaturated);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);

        this.signal = view.getIntOr("Signal", 0);
        this.oversaturated = view.getBooleanOr("Oversaturated", false);
    }

    @Override
    public boolean isOverStressed() {
        if (this.level.isClientSide()) {
            return this.oversaturated || this.overStressed;
        }

        return super.isOverStressed();
    }

    /**
     * @return whether the transmission disconnected because its output would spin too fast
     */
    public boolean isOversaturated() {
        return this.oversaturated;
    }

    /**
     * Accesses the ExtraKinetic BlockEntity associated with the AnalogTransmission
     */
    @Override
    public @NotNull KineticBlockEntity getExtraKinetics() {
        return this.extraWheel;
    }

    @Override
    //See javaDoc
    public boolean shouldConnectExtraKinetics() {
        return true;
    }

    @Override
    public String getExtraKineticsSaveName() {
        return "ExtraCogwheel";
    }

    /**
     * The ExtraKinetic BlockEntity for the AnalogTransmission. Extends KineticBlockEntity (Can be any other KBE), and implements ExtraKinetics
     */
    public static class AnalogTransmissionCogwheel extends KineticBlockEntity implements ExtraKineticsBlockEntity {

        public static final ICogWheel EXTRA_COGWHEEL_CONFIG = new ICogWheel() {
            @Override
            public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
                return false;
            }

            @Override
            public Direction.Axis getRotationAxis(final BlockState state) {
                return state.getValue(AnalogTransmissionBlock.AXIS);
            }
        };

        /**
         * Access to the parent BlockEntity to avoid called {@link Level#getBlockEntity(BlockPos) getBlockEntity} unnecessarily
         */
        private final KineticBlockEntity parentBlockEntity;

        /**
         * @param pos An ExtraBlockPos associated with this ExtraKinetic BlockEntity. This is needed to inform the {@link com.zurrtum.create.content.kinetics.RotationPropagator} that this BlockEntity is an ExtraKinetic one.
         */
        public AnalogTransmissionCogwheel(final BlockEntityType<?> typeIn, final ExtraBlockPos pos, final BlockState state, final KineticBlockEntity parentBlockEntity) {
            super(typeIn, pos, state);
            this.parentBlockEntity = parentBlockEntity;
        }

        /**
         * We call the parent's {@link KineticBlockEntity#propagateRotationTo(KineticBlockEntity, BlockState, BlockState, BlockPos, boolean, boolean) propagateRotationTo} here for easier rotation modifier Handling.
         */
        @Override
        public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
            return this.parentBlockEntity.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
        }

        @Override
        protected boolean canPropagateDiagonally(final IRotate block, final BlockState state) {
            return true;
        }

        @Override
        public KineticBlockEntity getParentBlockEntity() {
            return this.parentBlockEntity;
        }
    }
}
