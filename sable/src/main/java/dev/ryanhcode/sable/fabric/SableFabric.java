package dev.ryanhcode.sable.fabric;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.SableServerConfig;
import dev.ryanhcode.sable.command.SableCommand;
import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifiers;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;

public final class SableFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Sable.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SableCommand.register(dispatcher, registryAccess);
        });

        SubLevelSelectorModifiers.registerModifiers();

        SableAttributes.PUNCH_STRENGTH = Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Sable.sablePath(SableAttributes.PUNCH_STRENGTH_NAME), SableAttributes.PUNCH_STRENGTH_ATTRIBUTE);
        SableAttributes.PUNCH_COOLDOWN = Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Sable.sablePath(SableAttributes.PUNCH_COOLDOWN_NAME), SableAttributes.PUNCH_COOLDOWN_ATTRIBUTE);
        SableAttributes.register();

        final ResourceLoader loader = ResourceLoader.get(PackType.SERVER_DATA);
        loader.registerReloader(PhysicsBlockPropertiesDefinitionLoader.ID, PhysicsBlockPropertiesDefinitionLoader.INSTANCE);
        loader.registerReloader(DimensionPhysicsData.ReloadListener.ID, DimensionPhysicsData.ReloadListener.INSTANCE);
        loader.registerReloader(FloatingBlockMaterialDataHandler.ReloadListener.ID, FloatingBlockMaterialDataHandler.ReloadListener.INSTANCE);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> SableCommonEvents.syncDataPacket(packet -> player.connection.send(packet)));

        ConfigRegistry.INSTANCE.register(Sable.MOD_ID, ModConfig.Type.COMMON, SableConfig.SPEC);
        ConfigRegistry.INSTANCE.register(Sable.MOD_ID, ModConfig.Type.SERVER, SableServerConfig.SPEC);
    }
}
