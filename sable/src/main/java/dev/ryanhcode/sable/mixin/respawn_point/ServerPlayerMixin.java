package dev.ryanhcode.sable.mixin.respawn_point;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.mixinterface.respawn_point.ServerPlayerRespawnExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerRespawnExtension {

    @Shadow
    public ServerGamePacketListenerImpl connection;
    @Shadow
    private ServerPlayer.@Nullable RespawnConfig respawnConfig;
    @Unique
    @Nullable
    private UUID sable$respawnPoint = null;
    @Unique
    private Pair<UUID, Vector3d> sable$queuedFreeze = null;

    @Shadow
    private static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(final ServerLevel serverLevel, final ServerPlayer.RespawnConfig respawnConfig, final boolean bl) {
        return null;
    }

    @Shadow
    public abstract ServerLevel level();

    @Shadow
    public abstract void sendSystemMessage(Component component);

    @Override
    public @Nullable UUID sable$getRespawnPoint() {
        return this.sable$respawnPoint;
    }

    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void sable$setRespawnPosition(@Nullable final ServerPlayer.RespawnConfig config, final boolean sendMessage, final CallbackInfo ci) {
        final ServerLevel playerLevel = this.level();
        final SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(playerLevel);

        if (this.sable$respawnPoint != null) {
            data.removeTrackingPoint(this.sable$respawnPoint);
            this.sable$respawnPoint = null;
        }

        if (config != null) {
            final BlockPos blockPos = config.respawnData().pos();
            final SubLevel trackingSubLevel = Sable.HELPER.getContaining(playerLevel, blockPos);

            if (trackingSubLevel instanceof final ServerSubLevel serverSubLevel) {
                this.sable$respawnPoint = data.generateTrackingPoint(Vec3.atCenterOf(blockPos), serverSubLevel);

                if (this.sable$respawnPoint != null) {
                    final boolean theSame = config.isSamePosition(this.respawnConfig);
                    if (sendMessage && !theSame) {
                        this.sendSystemMessage(Component.translatable("block.minecraft.set_spawn"));
                    }

                    this.respawnConfig = config;
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sable$addRespawnPoint(final ValueOutput valueOutput, final CallbackInfo ci) {
        if (this.sable$respawnPoint != null) {
            valueOutput.store("RespawnPoint", UUIDUtil.CODEC, this.sable$respawnPoint);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sable$readRespawnPoint(final ValueInput valueInput, final CallbackInfo ci) {
        valueInput.read("RespawnPoint", UUIDUtil.CODEC).ifPresent(uuid -> this.sable$respawnPoint = uuid);
    }

    /**
     * Respawning on sub-levels: copy the sub-level respawn point without going through
     * {@link ServerPlayer#setRespawnPosition}, which would generate a new tracking point.
     */
    @Inject(method = "copyRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void sable$copyRespawnPosition(final ServerPlayer serverPlayer, final CallbackInfo ci) {
        if (serverPlayer.getRespawnConfig() != null) {
            this.sable$respawnPoint = ((ServerPlayerRespawnExtension) serverPlayer).sable$getRespawnPoint();
            this.respawnConfig = serverPlayer.getRespawnConfig();
        } else {
            this.sable$respawnPoint = null;
            this.respawnConfig = null;
        }
        ci.cancel();
    }

    @Override
    public void sable$takeQueuedFreezeFrom(final ServerPlayer oldPlayer) {
        final ServerPlayerRespawnExtension extension = (ServerPlayerRespawnExtension) oldPlayer;
        final Pair<UUID, Vector3d> queuedFreeze = extension.sable$getQueuedFreeze();

        if (queuedFreeze != null) {
            ((PlayerFreezeExtension) this).sable$freezeTo(queuedFreeze.first(), queuedFreeze.second());
            this.connection.send(new ClientboundCustomPayloadPacket(new ClientboundFreezePlayerPacket(queuedFreeze.first(), queuedFreeze.second())));
        }
    }

    @Override
    public @Nullable Pair<UUID, Vector3d> sable$getQueuedFreeze() {
        return this.sable$queuedFreeze;
    }

    /**
     * @author RyanH
     * @reason Respawning on sub-levels
     */
    @Redirect(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)Ljava/util/Optional;"))
    private Optional<ServerPlayer.RespawnPosAngle> sable$findRespawnPosition(final ServerLevel level, final ServerPlayer.RespawnConfig config, final boolean useSpawnBlock) {
        final SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(level);

        if (this.sable$respawnPoint != null) {
            final SubLevelTrackingPointSavedData.TakenLoginPoint point = data.take(this.sable$respawnPoint, false);

            if (point == null) {
                this.sable$respawnPoint = null;
                return Optional.empty();
            }

            // TODO: do validation here

            if (point.subLevelId() != null) {
                this.sable$queuedFreeze = Pair.of(point.subLevelId(), point.localAnchor());
            }

            return Optional.of(new ServerPlayer.RespawnPosAngle(JOMLConversion.toMojang(point.position()), config.respawnData().yaw(), config.respawnData().pitch()));
        }

        return findRespawnAndUseSpawnBlock(level, config, useSpawnBlock);
    }
}
