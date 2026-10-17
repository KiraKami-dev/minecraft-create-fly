package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.serialization.Codec;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.zurrtum.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/**
 * Client-only parts live in {@link SteeringWheelScrollValueBehaviour} (value box), {@link SteeringWheelAudioBehaviour}
 * (cranking sounds, was tickAudio) and {@link SteeringWheelRenderer}/{@link SteeringWheelVisual}.
 */
public class SteeringWheelBlockEntity extends GeneratingKineticBlockEntity {
    public static final int RPM = 16;

    public boolean held = false;
    private int inUse = 0;

    public AngleLimitBehaviour angleInput;

    public float targetAngle = 0;
    public float targetAngleToUpdate = 0;
    private float angle = 0;

    private float clientAngle = 0;
    private float oldClientAngle = 0;

    private double sequencedAngleLimit = 0;

    // output shaft rotation
    float generatedSpeed = 0;
    // internal angle rotation
    float logicalSpeed = 0;

    public BlockState material = Blocks.SPRUCE_PLANKS.defaultBlockState();

    // SimLang (catnip LangBuilder) is client-only on Create Fly
    private static final MutableComponent ROTATE_TIP = Component.translatable("simulated.gui.hold_tip.hold_to_rotate");

    public SteeringWheelBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.effects = new NoParticleKineticEffectHandler(this);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        behaviours.add(new HoldTipBehaviour(this, SteeringWheelBlockEntity::holdTipGetter));
        this.angleInput = new AngleLimitBehaviour(this);
        this.angleInput.between(1, 360);
        this.angleInput.setValue(180);
        behaviours.add(this.angleInput);
    }

    public static MutableComponent holdTipGetter(final Player player, final BlockPos pos, final BlockState state) {
        if (SteeringWheelBlock.lookingAtWheel(player, pos, 1, state)) {
            return ROTATE_TIP;
        }
        return null;
    }

    public void startHolding() {
        this.held = true;
        this.notifyUpdate();
    }

    public void stopHolding() {
        this.held = false;
        this.notifyUpdate();
    }

    public float directionConvert(final float val) {
        return -KineticBlockEntity.convertToDirection(val, this.getBlockState().getValue(SteeringWheelBlock.FACING));
    }

    // a lot of this is black magic stolen from the torsion spring
    public void updateTargetAngle(float absoluteTarget) {
        absoluteTarget = Mth.clamp(absoluteTarget, -this.angleInput.getValue(), this.angleInput.getValue());

        if (this.targetAngle == absoluteTarget)
            return;

        this.targetAngle = absoluteTarget;
        final float relativeAngle = absoluteTarget - this.angle;

        if (Math.abs(relativeAngle) < 0.001 && this.inUse <= 0) {
            this.generatedSpeed = 0;
            this.updateGeneratedRotation();
            return;
        }

        final float rotationSpeed = RPM * Math.signum(relativeAngle);
        if (rotationSpeed == 0)
            return;

        final float relativeValue = relativeAngle / rotationSpeed;
        if (relativeValue <= 0 && this.inUse <= 0) {
            this.generatedSpeed = 0;
            this.updateGeneratedRotation();
            return;
        }

        final double degreesPerTick = KineticBlockEntity.convertToAngular(rotationSpeed);
        this.inUse = (int) Math.ceil(relativeAngle / degreesPerTick) + 2;

        this.sequenceContext = new SequencedGearshiftBlockEntity.SequenceContext(SequencerInstructions.TURN_ANGLE, relativeValue);
        this.sequencedAngleLimit = Math.abs(relativeAngle);

        this.logicalSpeed = rotationSpeed;
        final Direction facing = this.getBlockState().getValue(SteeringWheelBlock.FACING);
        final boolean floor = this.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
        if ((facing == Direction.NORTH || facing == Direction.WEST) == floor) {
            this.generatedSpeed = -this.logicalSpeed;
        } else {
            this.generatedSpeed = this.logicalSpeed;
        }

        this.updateGeneratedRotation();
    }

    @Override
    protected void copySequenceContextFrom(final KineticBlockEntity sourceBE) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide()) {
            this.oldClientAngle = this.clientAngle;
            if (SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
                this.clientAngle = this.targetAngleToUpdate;
            } else {
                this.clientAngle = this.clientAngle + (this.targetAngleToUpdate - this.clientAngle) * 0.25f;
            }
        }

        if (this.getGeneratedSpeed() != 0) {
            this.integrateAngle();
        }

        if (this.inUse > 0) {
            this.inUse--;

            if (this.inUse == 0 && !this.level.isClientSide()) {
                this.sequenceContext = null;
                this.generatedSpeed = 0;
                this.updateGeneratedRotation();
            }
        } else if(!this.level.isClientSide()) {
            this.updateTargetAngle(this.targetAngleToUpdate);
        }
    }

    /**
     * linearly updates angle, limited in change by sequencedAngleLimit
     */
    private void integrateAngle() {
        float angularSpeed = this.getAngularSpeed();

        if (this.sequencedAngleLimit >= 0) {
            angularSpeed = (float) Mth.clamp(angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
            this.sequencedAngleLimit = Math.max(0, this.sequencedAngleLimit - Math.abs(angularSpeed));
        }

        this.angle = this.angle + angularSpeed;
    }

    public float getAngle() {
        return this.angle;
    }

    public float getAngularSpeed() {
        float speed = convertToAngular(this.getLogicalSpeed());
        if (this.getSpeed() == 0 || this.getLogicalSpeed() == 0) {
            speed = 0;
        }

        return speed;
    }

    public float getLogicalSpeed() {
        return this.inUse == 0 ? 0 : this.logicalSpeed;
    }

    @Override
    public float getGeneratedSpeed() {
        return this.inUse == 0 ? 0 : this.generatedSpeed;
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        view.putFloat("Angle", this.angle);
        view.putFloat("TargetAngle", this.targetAngle);
        if (this.targetAngleToUpdate != this.targetAngle) {
            view.putFloat("TargetAngleToUpdate", this.targetAngleToUpdate);
        }

        if (clientPacket) {
            view.putBoolean("Held", this.held);
        }

        view.store("Material", BlockState.CODEC, this.material);
    }

    @Override
    public void writeSafe(final ValueOutput view) {
        super.writeSafe(view);
        view.store("Material", BlockState.CODEC, this.material);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);
        // the angle limit is clamped to [1, 360] by AngleLimitBehaviour#read

        this.angle = Math.clamp(view.getFloatOr("Angle", 0), -360, 360);
        if (clientPacket) {
            this.held = view.getBooleanOr("Held", false);
        }

        if (!clientPacket || !SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
            this.updateTargetAngle(Math.clamp(view.getFloatOr("TargetAngle", 0), -360, 360));
            final Optional<Float> toUpdate = view.read("TargetAngleToUpdate", Codec.FLOAT);
            if (toUpdate.isPresent()) {
                this.targetAngleToUpdate = Math.clamp(toUpdate.get(), -360, 360);
            } else {
                this.targetAngleToUpdate = this.targetAngle;
            }
        }

        final BlockState prevMaterial = this.material;
        final Optional<BlockState> material = view.read("Material", BlockState.CODEC);
        if (material.isEmpty())
            return;

        this.material = material.get();
        if (this.material.isAir())
            this.material = Blocks.SPRUCE_PLANKS.defaultBlockState();

        if (clientPacket && prevMaterial != this.material)
            this.redraw();
    }

    public boolean shouldRenderShaft() {
        return true;
    }

    @Override
    protected Block getStressConfigKey() {
        return SimBlocks.STEERING_WHEEL;
    }

    public float getRenderAngle(final float partialTicks) {
        // todo add ponder angle support please :3
        final float renderAngle = Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
        final Direction facing = this.getBlockState().getValue(SteeringWheelBlock.FACING);
        if (facing == Direction.NORTH || facing == Direction.WEST) {
            return (float) Math.toRadians(-renderAngle);
        } else {
            return (float) Math.toRadians(renderAngle);
        }
    }

    public float getInteractionAngle(final float partialTicks) {
        return Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(0.4);
    }

    private void redraw() {
        // NeoForge's requestModelDataUpdate is not needed: the wheel is drawn by SteeringWheelRenderer/Visual,
        // which key their models on the material
        if (this.hasLevel()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
            this.level.getChunkSource()
                    .getLightEngine()
                    .checkBlock(this.worldPosition);
        }
    }

    public boolean isMaterialValid(final ItemStack stack) {
        if (!(stack.getItem()instanceof final BlockItem blockItem))
            return false;
        final BlockState material = blockItem.getBlock()
                .defaultBlockState();
        if (material == this.material)
            return false;
        return material.is(BlockTags.PLANKS);
    }

    public InteractionResult applyMaterialIfValid(final ItemStack stack) {
        if (this.isMaterialValid(stack) && (stack.getItem()instanceof final BlockItem blockItem)) {
            if (this.level.isClientSide() && !this.isVirtual())
                return InteractionResult.SUCCESS;
            this.material = blockItem.getBlock().defaultBlockState();
            this.notifyUpdate();
            this.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, this.worldPosition, Block.getId(this.material));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * The angle limit (1-360 degrees). Server half of the value box; the client half is
     * {@link SteeringWheelScrollValueBehaviour}.
     */
    public static class AngleLimitBehaviour extends ServerScrollValueBehaviour {
        public AngleLimitBehaviour(final SmartBlockEntity be) {
            super(be);
        }

        @Override
        public void read(final ValueInput view, final boolean clientPacket) {
            super.read(view, clientPacket);
            // todo pr create to validate this for all scroll value behaviours
            this.value = Mth.clamp(this.value, 1, 360);
        }
    }
}
