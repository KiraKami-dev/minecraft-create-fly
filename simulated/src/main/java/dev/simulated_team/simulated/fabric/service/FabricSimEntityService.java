package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.api.entity.FakePlayerHandler;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.service.SimEntityService;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class FabricSimEntityService implements SimEntityService {

	/**
	 * Replacement for NeoForge's {@code Entity#getPersistentData()}: a persistent compound attached to the entity.
	 */
	public static final AttachmentType<CompoundTag> CUSTOM_DATA = AttachmentRegistry.create(Simulated.path("custom_data"),
			builder -> builder.persistent(CompoundTag.CODEC).initializer(CompoundTag::new));

	/**
	 * Attachment types must exist before any world is loaded, so this is called during mod initialisation.
	 */
	public static void init() {
	}

	@Override
	public CompoundTag getCustomData(final Entity player) {
		return player.getAttachedOrCreate(CUSTOM_DATA);
	}

	@Override
	public double getPlayerReach(final Player player) {
		return player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
	}

	@Override
	public boolean isFake(final Player player) {
		return FakePlayerHandler.has(player);
	}

	@Override
	public <T extends Entity> EntityType.Builder<T> loaderEntityTransform(final EntityType.Builder<T> builder, final SimEntityTypes.EntityLoaderData data) {
		EntityType.Builder<T> result = builder;
		if (data.immuneToFire()) {
			result = result.fireImmune();
		}

		return result.clientTrackingRange(data.clientTrackingRange())
				.updateInterval(data.updateFrequency())
				.sized(data.width(), data.height())
				.alwaysUpdateVelocity(data.sendVelocity());
	}
}
