package dev.simulated_team.simulated.service;

import dev.simulated_team.simulated.index.SimEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public interface SimEntityService {

    SimEntityService INSTANCE = ServiceUtil.load(SimEntityService.class);

    CompoundTag getCustomData(Entity player);

    double getPlayerReach(Player player);

    boolean isFake(Player player);

    /**
     * Used to allow loader specific changes to entities being registered. <p>
     * Usually used to apply the same property changes to forge and fabric, as their builders are different.
     *
     * @param builder The entity type builder for this entity.
     * @param data    The data to be applied to this builder.
     * @param <T>     The entity type
     * @return the same builder
     */
    <T extends Entity> EntityType.Builder<T> loaderEntityTransform(EntityType.Builder<T> builder, SimEntityTypes.EntityLoaderData data);
}
