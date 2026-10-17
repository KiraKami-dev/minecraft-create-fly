package dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

public record LodestoneInformation(UUID id, Vector3d projectedPos) {
	public CompoundTag saveAsCompound() {
		final CompoundTag data = new CompoundTag();
		data.store("trackerID", UUIDUtil.CODEC, this.id);

		return data;
	}

	/**
	 * @return the loaded information, or null if the tag has no (valid) tracker id
	 */
	@Nullable
	public static LodestoneInformation loadFromCompound(final CompoundTag tag) {
		return tag.read("trackerID", UUIDUtil.CODEC)
				.map(id -> new LodestoneInformation(id, new Vector3d()))
				.orElse(null);
	}
}
