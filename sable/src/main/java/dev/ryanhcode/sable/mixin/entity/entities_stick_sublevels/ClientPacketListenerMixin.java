package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    // PORT(1.21.11): position syncs that used to be ClientboundTeleportEntityPacket + Entity#lerpTo are now
    // ClientboundEntityPositionSyncPacket + Entity#moveOrInterpolateTo
    @WrapOperation(method = "handleEntityPositionSync", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"))
    private void sable$handleEntityPositionSync(final Entity instance,
                                                final Vec3 pos,
                                                final float yRot,
                                                final float xRot,
                                                final Operation<Void> original,
                                                @Local(argsOnly = true) final ClientboundEntityPositionSyncPacket packet) {
        this.sable$lerp(instance, pos, Optional.of(yRot), Optional.of(xRot), true, ((Object) packet) instanceof final PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel());
    }

    // PORT(1.21.11): real teleports (ClientboundTeleportEntityPacket) interpolate inside the static setValuesFromPositionPacket.
    // They are never flagged as "actually in sub-level" (only ServerEntity#sendChanges flags packets).
    @WrapOperation(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"))
    private static void sable$handleTeleportEntity(final Entity instance,
                                                   final Vec3 pos,
                                                   final float yRot,
                                                   final float xRot,
                                                   final Operation<Void> original) {
        if (instance.level() instanceof final ClientLevel clientLevel) {
            sable$lerp(clientLevel, instance, pos, Optional.of(yRot), Optional.of(xRot), true, false);
        } else {
            original.call(instance, pos, yRot, xRot);
        }
    }

    @WrapOperation(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"))
    private void sable$handleMoveEntityPosRot(final Entity instance,
                                              final Vec3 pos,
                                              final float yRot,
                                              final float xRot,
                                              final Operation<Void> original,
                                              @Local(argsOnly = true) final ClientboundMoveEntityPacket packet) {
        this.sable$lerp(instance, pos, Optional.of(yRot), Optional.of(xRot), false, packet instanceof final PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel());
    }

    @WrapOperation(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;)V"))
    private void sable$handleMoveEntityPos(final Entity instance,
                                           final Vec3 pos,
                                           final Operation<Void> original,
                                           @Local(argsOnly = true) final ClientboundMoveEntityPacket packet) {
        this.sable$lerp(instance, pos, Optional.empty(), Optional.empty(), false, packet instanceof final PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel());
    }

    @Unique
    private void sable$lerp(final Entity entity,
                            final Vec3 packetPos,
                            final Optional<Float> yRot,
                            final Optional<Float> xRot,
                            final boolean pTeleport,
                            final boolean actuallyInSubLevel) {
        sable$lerp(this.level, entity, packetPos, yRot, xRot, pTeleport, actuallyInSubLevel);
    }

    @Unique
    private static void sable$lerp(final ClientLevel level,
                                   final Entity entity,
                                   final Vec3 packetPos,
                                   final Optional<Float> yRot,
                                   final Optional<Float> xRot,
                                   final boolean pTeleport,
                                   final boolean actuallyInSubLevel) {
        final EntityStickExtension extension = (EntityStickExtension) entity;
        Vec3 pos = packetPos;

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        final Vec3 plotPosition = extension.sable$getPlotPosition();

        if (!actuallyInSubLevel && subLevel == null && container.inBounds(BlockPos.containing(pos))) {
            return;
        }

        if (subLevel != null && !actuallyInSubLevel) {
            if (!(entity instanceof LivingEntity)) {
                pos = subLevel.logicalPose().transformPosition(pos);
                entity.moveOrInterpolateTo(Optional.of(pos), yRot, xRot);
                return;
            }

            if (plotPosition == null) {
                // just jumped on a sub-level
                extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(entity.position()));
            } else {
                final SubLevel existingSubLevel = Sable.HELPER.getContaining(level, plotPosition);

                if (existingSubLevel != null && subLevel != existingSubLevel) {
                    final Vec3 globalPlotPos = existingSubLevel.logicalPose().transformPosition(plotPosition);
                    extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(globalPlotPos));
                }
            }

            // The position is unused in the vanilla interpolation (it is cancelled in LivingEntityMixin#sable$setupLerp
            // while a plot position exists). This makes sure the entity rotation is lerped
            entity.moveOrInterpolateTo(Optional.of(packetPos), yRot, xRot);

            // This does a custom position lerp
            extension.sable$plotLerpTo(pos, InterpolationHandler.DEFAULT_INTERPOLATION_STEPS);
        } else {
            final SubLevel existingSubLevel = Sable.HELPER.getContaining(level, entity.position());

            if (subLevel != null && actuallyInSubLevel && existingSubLevel != subLevel) {
                entity.setPos(subLevel.logicalPose().transformPositionInverse(entity.position()));
            } else if (existingSubLevel != null && subLevel == null) {
                entity.setPos(existingSubLevel.logicalPose().transformPosition(entity.position()));
            }

            entity.moveOrInterpolateTo(Optional.of(packetPos), yRot, xRot);
            extension.sable$setPlotPosition(null);
        }
    }
}
