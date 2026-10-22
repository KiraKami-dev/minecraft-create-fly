package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.server.AeroBlockConfigs;
import dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.particle.HotAirEmberParticleData;
import dev.eriksonn.aeronautics.index.AeroLiftingGasTypes;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Client-only parts (hot air amount value box, goggle tooltip) are attached by {@code HotAirClient}.
 */
public class HotAirBurnerBlockEntity extends SmartBlockEntity
        implements BlockEntityLiftingGasProvider {

    public GasEmitterRenderHandler renderHandler = new GasEmitterRenderHandler();
    protected ClientBalloonInfo clientBalloonInfo;
    protected boolean powered;
    protected int signalStrength;
    protected HotAirBurnerValueBehaviour hotAirAmountBehaviour;
    protected double lastRenderTime;
    protected double renderTime;
    protected LerpedFloat intensity = LerpedFloat.linear();
    private int maxCapacity;
    private int ticksSinceSync;

    private Balloon currentBalloon;
    private @Nullable BlockPos castPosition;

    public HotAirBurnerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.setLazyTickRate(20);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.updateSignal();
        if (!this.isVirtual() && this.canOutputGas()) {
            this.tickBalloonLogic();
            this.notifyUpdate();
        }
    }

    public int getSignalStrength() {
        return this.signalStrength;
    }

    /**
     * Used exclusively for ponder
     */
    public void setSignalStrength(final int signalStrength) {
        this.signalStrength = signalStrength;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        this.setMaxCapacity(AeroConfig.server().blocks.hotAirBurnerMaxHotAir.get());

        this.hotAirAmountBehaviour = new HotAirBurnerValueBehaviour(this)
                .between(() -> 5, () -> AeroConfig.server().blocks.hotAirBurnerMaxHotAir.get());
        this.hotAirAmountBehaviour.initValue(this.maxCapacity);

        behaviours.add(this.hotAirAmountBehaviour);
    }

    public void updateSignal() {
        final boolean shouldPower = this.level.hasNeighborSignal(this.worldPosition);
        final int newSignalStrength = this.level.getBestNeighborSignal(this.worldPosition);
        if (newSignalStrength != this.signalStrength) {
            if (this.signalStrength == 0 && newSignalStrength != 0) {
                this.level.playSound(null, this.worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS,
                        .125f + this.level.random.nextFloat() * .125f, .75f - this.level.random.nextFloat() * .25f);
            } else if (newSignalStrength == 0) {
                this.level.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                        .125f + this.level.random.nextFloat() * .125f, 1.1f - this.level.random.nextFloat() * .2f);
            }
            this.signalStrength = newSignalStrength;
            this.powered = shouldPower;
            this.sendData();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if (this.level == null)
            return;

        if (this.canOutputGas() && !this.isVirtual()) {
            this.tickBalloonLogic();
        }

        if (!this.level.isClientSide()) {
            if (!this.isVirtual()) {
                this.notifyUpdate();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return AABB.encapsulatingFullBlocks(this.getBlockPos(), this.getBlockPos().above());
    }

    public void tick() {
        super.tick();

        this.ticksSinceSync++;
        if (!this.level.isClientSide()) {
            return;
        }

        final double intensityGoal = Math.max(0, this.getSignalStrength() / 15.0);
        this.intensity.chase(intensityGoal, 0.1, LerpedFloat.Chaser.EXP);
        this.intensity.tickChaser();

        this.lastRenderTime = this.renderTime;
        this.renderTime += (1.0 / 20.0) * (1.0 + (this.intensity.getValue() * this.intensity.getValue() * 1.8));

        this.renderHandler.targetFromRedstoneSignal(this.signalStrength);
        this.renderHandler.tick();
        if (this.isVirtual())
            return;

        // client particles & sounds
        double particleProbability = 0.4D * Math.sqrt(this.getGasOutput() / this.maxCapacity);
        final BlockPos pos = this.getBlockPos();
        final RandomSource random = this.level.getRandom();
        final double speed = this.signalStrength / 15.0f;

        if (particleProbability > random.nextFloat()) {
            this.level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true,
                    pos.getX() + 0.5 + random.nextDouble() / 5.0 * (random.nextBoolean() ? 1 : -1),
                    pos.getY() + (random.nextDouble() + random.nextDouble()) * 0.5 + 0.56,
                    pos.getZ() + 0.5 + random.nextDouble() / 5.0 * (random.nextBoolean() ? 1 : -1), 0.0D, speed * speed * 0.3, 0.0D);
        }

        if (random.nextInt(20) == 0 && this.powered) {
            this.level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.25F + random.nextFloat() * .25f, random.nextFloat() * 0.7F + 0.6F, false);
        }

        particleProbability /= 5;
        if (particleProbability > random.nextFloat()) {
            for (int i = 0; i < random.nextInt(1) + 1; ++i) {
                this.level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5,
                        pos.getY() + 0.5, pos.getZ() + 0.5,
                        random.nextFloat() / 2.0F, 5.0E-5D, random.nextFloat() / 2.0F);
            }
        }

        if (random.nextFloat() < 0.5f * this.intensity.getValue()) {
            this.level.addParticle(new HotAirEmberParticleData(this.getBlockState().getValue(HotAirBurnerBlock.VARIANT) == HotAirBurnerBlock.Variant.SOUL_FIRE),
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5f,
                    pos.getY() + 0.5 + 0.1,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5f,
                    this.intensity.getValue(),
                    this.intensity.getValue(),
                    this.intensity.getValue());
        }

        if (this.canOutputGas()) {
            AeroSoundDistUtil.addPosHotAirBurnerSound(this.getBlockPos());
        } else {
            AeroSoundDistUtil.removePosHotAirBurnerSound(this.getBlockPos());
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (this.level.isClientSide()) {
            AeroSoundDistUtil.removePosHotAirBurnerSound(this.getBlockPos());
        } else {
            this.removeFromBalloon();
        }
    }

    @Override
    public void write(final ValueOutput view, final boolean clientPacket) {
        view.putBoolean("IsPowered", this.powered);
        view.putInt("SignalStrength", this.signalStrength);

        if (clientPacket) {
            ClientBalloonInfo.writeToNBT(view, (ServerBalloon) this.getBalloon());
        }

        super.write(view, clientPacket);
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        this.powered = view.getBooleanOr("IsPowered", false);
        this.signalStrength = view.getIntOr("SignalStrength", 0);

        if (clientPacket) {
            this.ticksSinceSync = 0;
            this.clientBalloonInfo = ClientBalloonInfo.readFromNBT(view);
        }

        super.read(view, clientPacket);
    }

    @Override
    public Balloon getBalloon() {
        return this.currentBalloon;
    }

    @Override
    public void setBalloon(final Balloon balloon) {
        this.currentBalloon = balloon;
    }

    @Override
    public @Nullable BlockPos getCastPosition() {
        return this.castPosition;
    }

    @Override
    public void doRaycast() {
        final BlockPos pos = this.getBlockPos();

        final AeroBlockConfigs blocks = AeroConfig.server().blocks;
        final int range = blocks.hotAirBurnerMaxRange.get();

        this.castPosition = this.getRaycastedPosition(this.level,
                    Vec3.upFromBottomCenterOf(pos, 1.0),
                    Vec3.upFromBottomCenterOf(pos, 1.0 + range));
    }

    @Override
    public double getGasOutput() {
        return (this.hotAirAmountBehaviour.getValue() * this.signalStrength) / 15.0;
    }

    @Override
    public LiftingGasType getLiftingGasType() {
        return AeroLiftingGasTypes.DEFAULT_GAS;
    }

    @Override
    public boolean canOutputGas() {
        return this.signalStrength > 0 & !this.isRemoved();
    }

    @Override
    public BlockPos aeronautics$getBlockPos() {
        return this.getBlockPos();
    }

    @Override
    public Level aeronautics$getLevel() {
        return this.getLevel();
    }

    @Override
    public double getClientPredictedVolume() {
        if (this.clientBalloonInfo == null)
            return 0.0;

        return BlockEntityLiftingGasProvider.getPredictedVolume(this.clientBalloonInfo, this.ticksSinceSync);
    }

    public void setMaxCapacity(final int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public @Nullable ClientBalloonInfo getClientBalloonInfo() {
        return this.clientBalloonInfo;
    }

    @Override
    public int getTicksSinceSync() {
        return this.ticksSinceSync;
    }

    protected float getFlameIntensity(final float partialTicks) {
        return this.intensity.getValue(partialTicks);
    }

    public float getTimeOffset() {
        return (this.getBlockPos().hashCode() % 10);
    }

    public LerpedFloat getClientIntensity() {
        return this.intensity;
    }
}
