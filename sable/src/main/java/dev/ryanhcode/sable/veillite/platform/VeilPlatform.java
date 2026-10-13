/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Common platform features (Fabric only).
 */
public final class VeilPlatform {

    public static final VeilPlatform INSTANCE = new VeilPlatform();

    private VeilPlatform() {
    }

    public PlatformType getPlatformType() {
        return PlatformType.FABRIC;
    }

    public boolean isModLoaded(final String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public boolean hasErrors() {
        return false;
    }

    /**
     * Checks if the specified packet listener has the requested payload channel.
     */
    public boolean hasChannel(final PacketListener listener, final CustomPacketPayload.Type<?> type) {
        return switch (listener.flow().getOpposite()) {
            case SERVERBOUND -> ClientPlayNetworking.canSend(type);
            case CLIENTBOUND -> listener instanceof final ServerGamePacketListenerImpl impl &&
                    ServerPlayNetworking.canSend(impl, type);
        };
    }

    public enum PlatformType {
        NEOFORGE("NeoForge", "forge"),
        FABRIC("Fabric", "fabric");

        private final String platformName;
        private final String mixinPackageName;

        PlatformType(final String platformName, final String mixinPackageName) {
            this.platformName = platformName;
            this.mixinPackageName = mixinPackageName;
        }

        public String getPlatformName() {
            return this.platformName;
        }

        public String getMixinPackageName() {
            return this.mixinPackageName;
        }
    }
}
