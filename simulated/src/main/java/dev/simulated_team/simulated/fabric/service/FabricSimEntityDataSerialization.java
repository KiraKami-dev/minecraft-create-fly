package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.SimEntityDataSerialization;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.minecraft.network.syncher.EntityDataSerializer;

public class FabricSimEntityDataSerialization implements SimEntityDataSerialization {

    /**
     * Registered by id (not vanilla's integer id), so the serializer ids stay in sync between client and server.
     */
    @Override
    public <A, T extends EntityDataSerializer<A>> void registerDataSerializer(final String name, final T serializer) {
        FabricTrackedDataRegistry.register(Simulated.path(name), serializer);
    }
}
