package dev.eriksonn.aeronautics.content.blocks.hot_air.gust;

import dev.eriksonn.aeronautics.content.particle.AirPoofParticleData;
import dev.eriksonn.aeronautics.content.particle.GustParticleData;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.zurrtum.create.infrastructure.packet.s2c.NbtSpawnPacket;
import dev.ryanhcode.sable.util.SableNBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * The orientation is sent with the spawn packet (upstream: NeoForge {@code IEntityWithComplexSpawn}; here Create Fly's
 * {@link NbtSpawnPacket}).
 */
public class GustEntity extends Entity {
    private static final String ORIENTATION_KEY = "GustOrientation";

    private final Quaterniond orientation = new Quaterniond();
    private boolean spawnedInitialBurst = false;

    public static void addGust(final Level level, final BlockPos pos, final Direction direction) {
        final Quaterniond orientation = new Quaterniond(direction.getRotation());

        final GustEntity gust = new GustEntity(AeroEntityTypes.GUST, level, orientation);
        gust.setPos(pos.getCenter());

        level.addFreshEntity(gust);
    }

    public GustEntity(final EntityType<?> entityType, final Level level) {
        this(entityType, level, JOMLConversion.QUAT_IDENTITY);
    }

    public GustEntity(final EntityType<?> entityType, final Level level, final Quaterniondc orientation) {
        super(entityType, level);
        this.orientation.set(orientation);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.spawnClientEffects();
        } else {
            final SubLevel subLevel = Sable.HELPER.getContaining(this);

            // FIXME: this should be moved to something physics-tick dependant
            //  but we don't have a great way to do that for entities when this is
            //  being written

            if (subLevel instanceof final ServerSubLevel serverSubLevel) {
                final Vector3d forceDir = this.orientation.transform(new Vector3d(0.0, 1.0, 0.0))
                        .mul(-3.0);

                RigidBodyHandle.of(serverSubLevel)
                        .applyImpulseAtPoint(this.position(), JOMLConversion.toMojang(forceDir));
            }

            if (this.tickCount > 5)
                this.remove(RemovalReason.DISCARDED);
        }
    }

    private void spawnClientEffects() {
        final Level level = this.level();

        if (!this.spawnedInitialBurst) {
            final Vec3 soundPos = this.position();
            level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, AeroSoundEvents.GUST.event(), SoundSource.BLOCKS, 0.65f, 0.35f, false);

            final int poofParticleCount = 30;

            for (int i = 0; i < 3; i++) {
                // 10% chance no gustuous action
                if (level.random.nextFloat() < 0.1) {
                    continue;
                }

                final Quaternionf particleOrientation = new Quaternionf(this.orientation);
                particleOrientation.rotateY((float) (Math.PI * 2 / 3 * i   ));
                particleOrientation.rotateZ((float) Math.toRadians(-10.0));

                final float randomRot = (float) Math.toRadians(12f);
                particleOrientation.rotateX(this.random.nextFloat() * randomRot - randomRot / 2);
                particleOrientation.rotateZ(this.random.nextFloat() * randomRot - randomRot / 2);
                particleOrientation.rotateY(this.random.nextFloat() * randomRot - randomRot / 2);

                final Vector3d particlePos = JOMLConversion.toJOML(this.position());

                particlePos.add(particleOrientation.transform(new Vector3d(0.5, 0.5, 0.0)));

                level.addParticle(new GustParticleData(particleOrientation), particlePos.x, particlePos.y, particlePos.z, 0.0f, 0.0f, 0.0f);
            }

            for (int i = 0; i < poofParticleCount; i++) {
                final Vector3d outDir = this.orientation.transform(new Vector3d(0.0, 1.0, 0.0));
                final float velocity = 0.07f + this.random.nextFloat() * 0.1f;

                final double vx = outDir.x * velocity + this.random.nextGaussian() * 0.01;
                final double vy = outDir.y * velocity + this.random.nextGaussian() * 0.01;
                final double vz = outDir.z * velocity + this.random.nextGaussian() * 0.01;

                final float positionalRandomness = 0.35f * 2f;
                final Vec3 particlePos = this.position().subtract(outDir.x, outDir.y, outDir.z).add(
                        positionalRandomness * (this.random.nextFloat() - 0.5),
                        positionalRandomness * (this.random.nextFloat() - 0.5),
                        positionalRandomness * (this.random.nextFloat() - 0.5)
                );

                level.addParticle(AirPoofParticleData.INSTANCE, particlePos.x, particlePos.y, particlePos.z, vx, vy, vz);
            }

            this.spawnedInitialBurst = true;
        }
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    protected @NotNull AABB makeBoundingBox(final Vec3 position) {
        final AABB boundingBox = this.getDimensions(this.getPose()).makeBoundingBox(position);
        return boundingBox.move(0, -boundingBox.getYsize() / 2.0, 0);
    }

    @Override
    protected void defineSynchedData(final @NotNull SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        return false;
    }

    /**
     * Upstream had load and save swapped (load wrote the tag, save read it); saved under the same key the right way round
     */
    @Override
    protected void readAdditionalSaveData(final ValueInput view) {
        view.read(ORIENTATION_KEY, CompoundTag.CODEC).ifPresent(tag -> this.orientation.set(SableNBTUtils.readQuaternion(tag)));
    }

    @Override
    protected void addAdditionalSaveData(final @NotNull ValueOutput view) {
        view.store(ORIENTATION_KEY, CompoundTag.CODEC, SableNBTUtils.writeQuaternion(this.orientation));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity entityTrackerEntry) {
        final CompoundTag spawnData = new CompoundTag();
        spawnData.put(ORIENTATION_KEY, SableNBTUtils.writeQuaternion(this.orientation));
        return new NbtSpawnPacket(this, entityTrackerEntry, spawnData);
    }

    @Override
    public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);

        if (packet instanceof final NbtSpawnPacket spawnPacket && spawnPacket.getNbt() != null) {
            spawnPacket.getNbt().getCompound(ORIENTATION_KEY).ifPresent(tag -> this.orientation.set(SableNBTUtils.readQuaternion(tag)));
        }
    }
}
