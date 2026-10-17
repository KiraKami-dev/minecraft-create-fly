package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.ref.WeakReference;
import java.util.List;

public class VelocitySensorBlockEntity extends SmartBlockEntity {

    private float adjustedVelocity;

    private int signedRedstoneStrength;

    private Vector3dc currentNormal;

    private WeakReference<SubLevel> subLevelReference;

    /**
     * Server half of the max speed setting, the value box is {@link VelocitySensorValueBehaviour} on the client
     */
    private VelocitySensorScrollValueBehaviour maxSpeed;

    // fan rendering, clientside
    private final LerpedFloat fanSpeed = LerpedFloat.linear().chase(0, 0.5, LerpedFloat.Chaser.EXP);
    private float fanAngle = 0;
    private float oldFanAngle = 0;

    public VelocitySensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.subLevelReference = new WeakReference<>(null);

        this.adjustedVelocity = 0;
        this.signedRedstoneStrength = 0;

        this.currentNormal = new Vector3d();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> list) {
        this.maxSpeed = new VelocitySensorScrollValueBehaviour(this);
        this.maxSpeed.between(1, 50);
        this.maxSpeed.setValue(10);
        list.add(this.maxSpeed);
    }

    @Override
    public void initialize() {
        super.initialize();

        this.subLevelReference = new WeakReference<>(Sable.HELPER.getContaining(this.getLevel(), this.worldPosition));
    }

    @Override
    public void tick() {
        this.currentNormal = JOMLConversion.toJOML(Vec3.atLowerCornerOf(AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState()).getUnitVec3i()));
        super.tick();

        final SubLevel subLevel = this.subLevelReference.get();
        final int redstoneStrengthBefore = this.signedRedstoneStrength;
        if (!this.level.isClientSide()) {
            if (subLevel != null) {
                final float dot = (float) this.getGlobalVelocity().dot(subLevel.logicalPose().transformNormal(this.currentNormal, new Vector3d()));
                if (Math.abs(dot) > 0.05) {
                    this.adjustedVelocity = dot;
                } else {
                    this.adjustedVelocity = 0;
                }

                // int cast rounds towards 0
                this.signedRedstoneStrength = (int) Math.clamp((this.getAdjustedVelocity() / this.maxSpeed.getValue()) * 15f, -15, 15);
            } else { // sublevel is null, aka on stationary ground
                this.adjustedVelocity = 0;
                this.signedRedstoneStrength = 0;
            }

            if (redstoneStrengthBefore != this.signedRedstoneStrength) {
                final int power;
                if (this.signedRedstoneStrength == 0) {
                    power = 0;
                } else if (this.signedRedstoneStrength < 0) {
                    power = 1;
                } else {
                    power = 2;
                }

                this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(VelocitySensorBlock.POWERED, power));

                final Direction axisDir = AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState());

                this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
                this.level.updateNeighborsAt(this.worldPosition.relative(axisDir), this.getBlockState().getBlock());
                this.level.updateNeighborsAt(this.worldPosition.relative(axisDir.getOpposite()), this.getBlockState().getBlock());
            }

            this.sendData();
        } else { // is clientside
            this.fanSpeed.updateChaseTarget(Mth.clamp(this.getAdjustedVelocity() / this.maxSpeed.getValue(), -1, 1));
            this.fanSpeed.tickChaser();
            this.oldFanAngle = this.fanAngle;
            this.fanAngle += this.fanSpeed.getValue();
        }
    }

    public ServerScrollValueBehaviour getMaxSpeed() {
        return this.maxSpeed;
    }

    public float getFanAngle(final float pt) {
        return Mth.lerp(pt, this.oldFanAngle, this.fanAngle);
    }

    /**
     * @return Whether the sensor is on a sub-level (it only measures velocity on sub-levels)
     */
    public boolean isOnSubLevel() {
        return this.subLevelReference.get() != null;
    }

    private Vector3d getGlobalVelocity() {
        final SubLevel subLevel = this.subLevelReference.get();
        if (subLevel == null) {
            return new Vector3d();
        }

        final Vector3d jomlPos = JOMLConversion.toJOML(this.worldPosition.getCenter());
        return subLevel.logicalPose().transformPosition(jomlPos, new Vector3d()).sub(subLevel.lastPose().transformPosition(jomlPos, new Vector3d()), jomlPos).mul(20.0F);
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        view.putFloat("AdjustedVelocity", this.getAdjustedVelocity());
        view.putInt("SignedRedstoneStrength", this.signedRedstoneStrength);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);

        this.adjustedVelocity = view.getFloatOr("AdjustedVelocity", 0);
        this.signedRedstoneStrength = Mth.clamp(-15, 15, view.getIntOr("SignedRedstoneStrength", 0));
    }

    public Vector3dc getCurrentNormal() {
        return this.currentNormal;
    }

    public int getRedstoneStrength() {
        return Mth.abs(this.signedRedstoneStrength);
    }

    /**
     * Positive is towards axis normal, negative is away
     */
    public float getAdjustedVelocity() {
        return this.adjustedVelocity;
    }

    /**
     * Max speed setting, the row picks the direction (away / towards the axis normal)
     */
    public static class VelocitySensorScrollValueBehaviour extends ServerScrollValueBehaviour {
        private boolean towards;

        public VelocitySensorScrollValueBehaviour(final SmartBlockEntity be) {
            super(be);
        }

        @Override
        public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlDown) {
            super.setValueSettings(player, valueSetting, ctrlDown);
            this.towards = valueSetting.row() == 1;
        }

        @Override
        public int getValue() {
            return super.getValue() * (this.towards ? 1 : -1);
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(this.towards ? 1 : 0, this.value);
        }

        public boolean isTowards() {
            return this.towards;
        }

        @Override
        public void read(final ValueInput view, final boolean clientPacket) {
            this.towards = view.getBooleanOr("ScrollValueTowards", false);
            super.read(view, clientPacket);
        }

        @Override
        public void write(final ValueOutput view, final boolean clientPacket) {
            view.putBoolean("ScrollValueTowards", this.towards);
            super.write(view, clientPacket);
        }
    }
}
