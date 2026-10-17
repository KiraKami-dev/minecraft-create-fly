package dev.simulated_team.simulated.fabric.events;

import dev.simulated_team.simulated.command.SimCommand;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.fabric.service.FabricSimEntityService;
import dev.simulated_team.simulated.fabric.service.FabricSimItemService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Fabric API events feeding {@link SimulatedCommonEvents} (port of NeoForge's {@code SimNeoForgeCommonEvents}).
 * Registered from {@link SimulatedCommonEvents#register()}.
 */
@ApiStatus.Internal
public final class SimFabricCommonEvents {

    private SimFabricCommonEvents() {
    }

    public static void register() {
        FabricSimEntityService.init();

        // NeoForge's ChunkEvent.Load had an isNewChunk flag; Fabric fires CHUNK_GENERATE right after CHUNK_LOAD for
        // freshly generated chunks, so new chunks are reported twice (not new, then new).
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> SimulatedCommonEvents.onChunkLoad(level, chunk, false));
        ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> SimulatedCommonEvents.onChunkLoad(level, chunk, true));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SimulatedCommonEvents.onPlayerLoggedIn(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> SimCommand.register(dispatcher, buildContext));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> FabricSimItemService.setFuelValues(server.fuelValues()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> FabricSimItemService.setFuelValues(server.fuelValues()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            FabricSimItemService.setFuelValues(null);
            SimulatedCommonEvents.onServerStopped(server);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (final ServerLevel level : server.getAllLevels()) {
                SimulatedCommonEvents.onServerTickEnd(level);
            }
        });

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                EndSeaPhysicsData.syncDataPacket(packet -> player.connection.send(packet)));

        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(EndSeaPhysicsData.ReloadListener.ID, EndSeaPhysicsData.ReloadListener.INSTANCE);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            final InteractionResult result = SimulatedCommonEvents.rightClickBlock(level, hitResult.getBlockPos(), player, player.getItemInHand(hand));
            return result != null ? result : InteractionResult.PASS;
        });

        DefaultItemComponentEvents.MODIFY.register(context -> SimulatedCommonEvents.modifyDefaultComponents((item, modifier) ->
                context.modify(item.asItem(), builder -> {
                    final DataComponentPatch.Builder patch = DataComponentPatch.builder();
                    modifier.accept(patch);
                    applyPatch(patch.build(), builder);
                })));
    }

    /**
     * NeoForge's default component event hands out a patch builder; Fabric's hands out the map builder itself.
     */
    private static void applyPatch(final DataComponentPatch patch, final DataComponentMap.Builder builder) {
        for (final Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            setUnchecked(builder, entry.getKey(), entry.getValue().orElse(null));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void setUnchecked(final DataComponentMap.Builder builder, final DataComponentType<T> type, @Nullable final Object value) {
        builder.set(type, (T) value);
    }
}
