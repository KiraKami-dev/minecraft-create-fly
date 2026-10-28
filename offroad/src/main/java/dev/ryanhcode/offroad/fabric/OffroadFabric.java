package dev.ryanhcode.offroad.fabric;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.events.OffroadCommonEvents;
import dev.ryanhcode.offroad.fabric.service.FabricOffroadConfigService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;

/**
 * Fabric main entrypoint (upstream: {@code OffroadNeoForge}). Everything registers here, in {@code onInitialize}
 * (like Simulated and Aeronautics). Offroad's items are shown in the Simulated creative tab, in their own section.
 */
public final class OffroadFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Offroad.init();

        // after Offroad.init(): the stress config entries are collected while registering blocks
        FabricOffroadConfigService.registerCommon();

        // ModifyDefaultComponentsEvent: Create's wheels count as tires
        DefaultItemComponentEvents.MODIFY.register(context -> OffroadCommonEvents.modifyDefaultComponents((item, modifier) ->
                context.modify(item.asItem(), builder -> {
                    // NeoForge's event hands out a patch builder; Fabric's hands out the map builder itself
                    final DataComponentPatch.Builder patch = DataComponentPatch.builder();
                    modifier.accept(patch);
                    patch.build().entrySet().forEach(entry -> setUnchecked(builder, entry.getKey(), entry.getValue().orElse(null)));
                })));

        // LevelTickEvent.Post (server side; the client half is in OffroadFabricClient)
        ServerTickEvents.END_WORLD_TICK.register((final ServerLevel level) -> OffroadCommonEvents.tickLevelEvent(level));
    }

    @SuppressWarnings("unchecked")
    private static <T> void setUnchecked(final DataComponentMap.Builder builder, final DataComponentType<T> type, final Object value) {
        builder.set(type, (T) value);
    }
}
