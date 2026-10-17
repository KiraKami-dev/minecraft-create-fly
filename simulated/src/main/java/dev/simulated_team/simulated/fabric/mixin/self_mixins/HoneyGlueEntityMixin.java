package dev.simulated_team.simulated.fabric.mixin.self_mixins;

import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * NeoForge's {@code IEntityWithComplexSpawn}: the glue's bounds are part of its spawn data. On Fabric they are sent as a
 * bounds sync payload bundled with the vanilla add-entity packet, so the client entity never exists without its bounds
 * (payloads inside a bundle are handled in order on the main thread).
 */
@Mixin(HoneyGlueEntity.class)
public abstract class HoneyGlueEntityMixin extends Entity {

    public HoneyGlueEntityMixin(final EntityType<?> type, final Level level) {
        super(type, level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
        final Packet<ClientGamePacketListener> addEntity = super.getAddEntityPacket(serverEntity);
        final HoneyGlueSyncBoundsPacket bounds = new HoneyGlueSyncBoundsPacket(this.getBoundingBox(), this.getId(), null);
        return new ClientboundBundlePacket(List.of(addEntity, new ClientboundCustomPayloadPacket(bounds)));
    }
}
