package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Math.*;

public class GimbalSensorBlockEntity extends SmartBlockEntity {

    private static final Pose3dc IDENTITY_POSE = new Pose3d(new Vector3d(), new Quaterniond(), new Vector3d(), new Vector3d(1.0));
    private static final double MAX_ANGLE_X = Math.toRadians(90);
    private static final double MAX_ANGLE_Z = Math.toRadians(90);
    private final EnumMap<Direction, Integer> redstoneMap;

    // Client Animations
    private final Vector3d previousAngles = new Vector3d(0, 0, 0);
    private final Vector3d angleInertia = new Vector3d(110, 110, 34);
    private final Vector3d angleDamping = new Vector3d(0.2, 0.2, 0.2);

    /**
     * should only be false in the ponder for easier rendering
     */
    public boolean updateVisualRotation = true;
    CompassTarget compassTarget = new CompassTarget();
    /**
     * Server half of the angle limits, the value box is {@link GimbalSensorValueBehaviour} on the client
     */
    GimbalSensorScrollValueBehaviour axisBehaviour;
    private Vector3d eulerAngles = new Vector3d(0, 0, 0);
    private Vector3d angleVelocities = new Vector3d(0, 0, 0);
    private Quaterniond lastShellOrientation = null;

    //stored separately for goggle information and display sources
    private double ZAngle;
    private double XAngle;

    public GimbalSensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        //Unknown if this is needed, but better to be safe than sorry - cyvack
        this.redstoneMap = new EnumMap<>(Direction.class);
        for (final Direction dir : Iterate.directions) {
            this.redstoneMap.put(dir, 0);
        }
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        behaviours.add((this.axisBehaviour = new GimbalSensorScrollValueBehaviour(this)).between(-90, 90));
        this.axisBehaviour.primaryValue = 45;
        this.axisBehaviour.secondaryValue = 45;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.randomNudge();

    }

    @Override
    public void tick() {
        super.tick();
        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        final Pose3dc pose = subLevel != null ? subLevel.logicalPose() : IDENTITY_POSE;

        if (this.level.isClientSide())
            this.animateClientRotation(subLevel, pose);
        if (subLevel == null)
            return;

        final Vector3d ld = JOMLConversion.toJOML(Vec3.atLowerCornerOf(Direction.DOWN.getUnitVec3i()));
        subLevel.logicalPose().orientation().transformInverse(ld);

        this.XAngle = ld.y() < 0 || ld.z() * ld.z() > 0.001 ? atan2(ld.z(), -ld.y()) : 0;
        this.ZAngle = ld.y() < 0 || ld.x() * ld.x() > 0.001 ? atan2(ld.x(), -ld.y()) : 0;

        this.setPower(this.ZAngle, Direction.EAST);
        this.setPower(-this.ZAngle, Direction.WEST);

        this.setPower(this.XAngle, Direction.SOUTH);
        this.setPower(-this.XAngle, Direction.NORTH);
    }

    public void randomNudge() {
        final Vec3 v = (VecHelper.offsetRandomly(new Vec3(0, 0, 0), this.level.random, 0.2f));
        this.angleVelocities.set(v.x, v.y, v.z);
        this.eulerAngles.set(0, 0, this.level.random.nextFloat() * Math.PI * 2);
    }

    void animateClientRotation(final SubLevel subLevel, final Pose3dc pose) {

        this.previousAngles.set(this.eulerAngles);

        final Vector3d shellVelocity = this.getShellVelocity(subLevel);

        final Vector3d acceleration = new Vector3d();
        if (this.updateVisualRotation) this.addGravityTorque(pose, acceleration);

        final Vec3 globalPosition = pose.transformPosition(Vec3.atCenterOf(this.getBlockPos()));
        this.compassTarget.update(globalPosition, this.level);
        final Vector3d target = new Vector3d();
        this.compassTarget.getTarget(target);

        this.addCompassTorque(pose, acceleration, target);
        if (this.compassTarget.isRandom())
            acceleration.z += (2 * this.level.random.nextFloat() - 1) * 2.1;

        acceleration.div(this.angleInertia);
        final Vector3d relativeVelocity = this.angleVelocities.add(shellVelocity, new Vector3d());
        final Vector3d currentDamping = relativeVelocity.mul(this.angleDamping);
        this.angleVelocities.add(acceleration).sub(currentDamping);

        final Vector3d totalVelocity = this.angleVelocities.add(shellVelocity, new Vector3d());
        this.eulerAngles.add(totalVelocity);


        this.collide(this.eulerAngles, totalVelocity, 1, Math.abs(Math.toRadians(this.axisBehaviour.primaryValue)));
        this.collide(this.eulerAngles, totalVelocity, 0, Math.abs(Math.toRadians(this.axisBehaviour.secondaryValue)));
        totalVelocity.sub(shellVelocity, this.angleVelocities);
    }

    void addGravityTorque(final Pose3dc pose, final Vector3d torque) {
        final Vector3dc globalPosition = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.atCenterOf(this.getBlockPos()));
        final Vector3d localGravity = new Vector3d(DimensionPhysicsData.getGravity(this.level, globalPosition));
        this.transformBaseInverse(localGravity, pose);

        // transforms to be relative to the gimbal ring
        this.transformPrimaryInverse(localGravity);

        // the down direction of the compass, relative to the gimbal ring
        final Vector3d localDown = new Vector3d(0, -1, 0).rotateX(this.eulerAngles.y);
        final Vector3d localTorque = localDown.cross(localGravity);

        torque.x += localTorque.z;
        torque.y += localTorque.x;
    }

    void addCompassTorque(final Pose3dc pose, final Vector3d torque, final Vector3d target) {
        this.transformBaseInverse(target, pose);
        this.transformPrimaryInverse(target);//from base to gimbal ring
        this.transformSecondaryInverse(target);//from gimbal ring to compass
        this.transformCompassInverse(target);//from compass to needle

        final Vector3d localTorque = new Vector3d(0, 0, -1).cross(target);
        torque.z += localTorque.y;
    }

    private Vector3d getShellVelocity(final SubLevel subLevel) {
        final Vector3d shellVelocity = new Vector3d();
        if (subLevel != null) {
            final Pose3d pose = subLevel.logicalPose();

            if (this.lastShellOrientation == null) {
                this.lastShellOrientation = new Quaterniond(pose.orientation());
            } else {
                final Quaterniond rotationDiff = this.lastShellOrientation.div(pose.orientation(), new Quaterniond());
                final Vector3d angularVelocity = new Vector3d(rotationDiff.x, rotationDiff.y, rotationDiff.z).mul(2);
                this.transformBaseInverse(angularVelocity, pose);
                shellVelocity.x = angularVelocity.z;
                this.transformPrimaryInverse(angularVelocity);
                shellVelocity.y = angularVelocity.x;
                this.transformSecondaryInverse(angularVelocity);
                shellVelocity.z = angularVelocity.y;
                this.lastShellOrientation.set(pose.orientation());
            }
        } else
            this.lastShellOrientation = null;
        return shellVelocity;
    }

    private void collide(final Vector3d position, final Vector3d velocity, final int index, final double limit) {
        double p = position.get(index);
        double v = velocity.get(index);
        final double m = p > 0 ? 1 : -1;
        p *= m;
        v *= m;
        if (p >= limit) {
            p = limit;
            if (v > 0)
                v *= -0.9;
        }
        position.setComponent(index, p * m);
        velocity.setComponent(index, v * m);
    }

    private void setPower(final double angle, final Direction dir) {
        final boolean alongPrimary = (dir.getAxis() == this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
        final float angleLimit = alongPrimary ? this.axisBehaviour.primaryValue : this.axisBehaviour.secondaryValue;

        final int newPower = angleLimit == 0 ? 0 : max(min((int) (14.5 * angle / Math.toRadians(angleLimit) + 0.5), 15), 0);
        if (this.redstoneMap.get(dir) != newPower) {
            this.redstoneMap.put(dir, newPower);
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(dir), this.getBlockState().getBlock());
        }
    }

    public Quaternionf getBaseQuaternion() {
        final Quaternionf Q = new Quaternionf();
        final float angle = Direction.fromAxisAndDirection(this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE).toYRot();
        Q.rotateY((float) Math.toRadians(angle));
        return Q;
    }

    public Quaternionf applyPrimaryQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateZ(this.lerp((float) this.previousAngles.x, (float) this.eulerAngles.x, partialTick));
        return Q;
    }

    public Quaternionf applySecondaryQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateX(this.lerp((float) this.previousAngles.y, (float) this.eulerAngles.y, partialTick));
        return Q;
    }

    public Quaternionf applyCompassQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateY(this.lerp((float) this.previousAngles.z, (float) this.eulerAngles.z, partialTick));
        return Q;
    }

    private Vector3d transformBaseInverse(final Vector3d v, final Pose3dc ctx) {
        final float angle = Direction.fromAxisAndDirection(this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE).toYRot();
        ctx.orientation().transformInverse(v);
        v.rotateY(-Math.toRadians(angle));
        return v;
    }

    private Vector3d transformPrimaryInverse(final Vector3d v) {
        v.rotateZ(-this.eulerAngles.x);
        return v;
    }

    private Vector3d transformSecondaryInverse(final Vector3d v) {
        v.rotateX(-this.eulerAngles.y);
        return v;
    }

    private Vector3d transformCompassInverse(final Vector3d v) {
        v.rotateY(-this.eulerAngles.z);
        return v;
    }

    float lerp(final float a, final float b, final float t) {
        return a * (1 - t) + b * t;
    }

    public int getPower(final Direction dir) {
        return this.redstoneMap.get(dir);
    }

    public double getZAngle() {
        return this.ZAngle;
    }

    public double getXAngle() {
        return this.XAngle;
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        final ValueOutput powers = view.child("Powers");
        for (final Map.Entry<Direction, Integer> entry : this.redstoneMap.entrySet()) {
            powers.putInt(entry.getKey().getName(), entry.getValue());
        }
        if (!clientPacket) {
            view.putFloat("Angle1", (float) this.eulerAngles.x);
            view.putFloat("Angle2", (float) this.eulerAngles.y);
            view.putFloat("Angle3", (float) this.eulerAngles.z);
            view.putFloat("Vel1", (float) this.angleVelocities.x);
            view.putFloat("Vel2", (float) this.angleVelocities.y);
            view.putFloat("Vel3", (float) this.angleVelocities.z);
        }

        view.putDouble("x_angle", this.XAngle);
        view.putDouble("zz_angle", this.ZAngle);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);

        final Optional<ValueInput> powers = view.child("Powers");
        if (powers.isPresent()) {
            for (final Map.Entry<Direction, Integer> entry : this.redstoneMap.entrySet()) {
                entry.setValue(powers.get().getIntOr(entry.getKey().getName(), 0));
            }
        }
        if (!clientPacket) {
            float x = view.getFloatOr("Angle1", 0);
            float y = view.getFloatOr("Angle2", 0);
            float z = view.getFloatOr("Angle3", 0);
            this.eulerAngles = new Vector3d(x, y, z);
            x = view.getFloatOr("Vel1", 0);
            y = view.getFloatOr("Vel2", 0);
            z = view.getFloatOr("Vel3", 0);
            this.angleVelocities = new Vector3d(x, y, z);
        }

        this.XAngle = view.getDoubleOr("x_angle", 0);
        // was read from "z_angle" (never written) on 1.21.1
        this.ZAngle = view.getDoubleOr("zz_angle", 0);
    }

    /**
     * Angle limits of the two axes; the value box side picks the axis ({@link #lastSide} is set by the client value box)
     */
    public static class GimbalSensorScrollValueBehaviour extends ServerScrollValueBehaviour {
        protected Direction lastSide = Direction.NORTH;
        protected int primaryValue;
        protected int secondaryValue;

        public GimbalSensorScrollValueBehaviour(final GimbalSensorBlockEntity be) {
            super(be);
            this.primaryValue = 0;
            this.secondaryValue = 0;
        }

        public void setLastSide(final Direction side) {
            this.lastSide = side;
        }

        public boolean isPrimaryAxis() {
            final Direction.Axis blockAxis = this.blockEntity.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS);
            return this.lastSide.getAxis().isHorizontal() && this.lastSide.getAxis() != blockAxis;
        }

        @Override
        public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlHeld) {
            final int value = Math.max(0, valueSetting.value());
            if (!valueSetting.equals(this.getValueSettings()))
                this.playFeedbackSound(this);
            this.setValue(valueSetting.row() == 0 ? -value : value);
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(this.getValue() < 0 ? 0 : 1, Math.abs(this.getValue()));
        }

        @Override
        public void write(final ValueOutput view, final boolean clientPacket) {
            view.putInt("ScrollValue1", this.primaryValue);
            view.putInt("ScrollValue2", this.secondaryValue);
            super.write(view, clientPacket);
        }

        @Override
        public void read(final ValueInput view, final boolean clientPacket) {
            this.primaryValue = view.getIntOr("ScrollValue1", 0);
            this.secondaryValue = view.getIntOr("ScrollValue2", 0);
            super.read(view, clientPacket);
        }

        @Override
        public boolean writeToClipboard(final ValueOutput view, final Direction side) {
            if(!this.acceptsValueSettings())
                return false;
            view.putInt("ScrollValue1", this.primaryValue);
            view.putInt("ScrollValue2", this.secondaryValue);
            return true;
        }

        @Override
        public boolean readFromClipboard(final ValueInput view, final Player player, final Direction side, final boolean simulate) {
            if(!this.acceptsValueSettings()) return false;
            final Optional<Integer> primary = view.getInt("ScrollValue1");
            final Optional<Integer> secondary = view.getInt("ScrollValue2");
            if(primary.isEmpty() || secondary.isEmpty()) return true;
            if(simulate) return true;
            this.primaryValue = primary.get();
            this.secondaryValue = secondary.get();
            this.blockEntity.setChanged();
            this.blockEntity.sendData();
            return true;
        }

        @Override
        public int getValue() {
            return this.isPrimaryAxis() ? this.primaryValue : this.secondaryValue;
        }

        @Override
        public void setValue(int value) {
            value = Mth.clamp(value, this.min, this.max);
            if (value == this.getValue())
                return;
            if (this.isPrimaryAxis())
                this.primaryValue = value;
            else
                this.secondaryValue = value;
            //callback.accept(value);
            this.blockEntity.setChanged();
            this.blockEntity.sendData();
        }
    }

    static class CompassTarget {
        private final Vector3d target = new Vector3d(0, 0, 0);
        private final Vector3d randomTarget = new Vector3d(0, 0, 0);
        private int randomTargetTimer = 0;
        private double randomTargetLength = 3;
        private boolean isRandom = false;

        public void update(final Vec3 pos, final Level level) {
            this.isRandom = !level.dimensionType().natural();
            if (!this.isRandom) {
                this.target.set(0, 0, -1);
            } else {
                final RandomSource r = level.random;
                if (this.randomTargetTimer-- < 0) {

                    final float radius = 1.0f;
                    this.randomTarget.set(
                            (r.nextFloat() - .5f) * 2 * radius,
                            (r.nextFloat() - .5f) * 2 * radius,
                            (r.nextFloat() - .5f) * 2 * radius);
                    this.randomTargetTimer = level.random.nextInt(5, 15);
                }
                final float nudge = 0.3f;
                this.randomTarget.add(
                        (r.nextFloat() - .5f) * 2 * nudge,
                        (r.nextFloat() - .5f) * 2 * nudge,
                        (r.nextFloat() - .5f) * 2 * nudge);
                this.randomTarget.normalize();
                final double step = 0.5;
                this.target.mul(1 - step).fma(step, this.randomTarget);
                this.target.normalize();
            }

        }

        public boolean isRandom() {
            return this.isRandom;
        }

        public void setRandomTargetLength(final double s) {
            this.randomTargetLength = s;
        }

        public Vector3d getTarget(final Vector3d v) {
            return this.target.mul(this.isRandom() ? this.randomTargetLength : 1, v);
        }
    }
}
