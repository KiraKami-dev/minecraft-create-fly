package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.simulated_team.simulated.index.SimTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Tag keys. The tag contents are shipped as JSON under {@code data/} (upstream generated them from here).
 */
public class AeroTags {

	public static class BlockTags {
		public static final TagKey<Block> AIRTIGHT = create("airtight");
		public static final TagKey<Block> ENVELOPE = create("envelope");
		public static final TagKey<Block> LEVITITE = create("levitite");
		public static final TagKey<Block> LEVITITE_BREAKABLE = create("levitite_breakable");
		public static final TagKey<Block> LEVITITE_CATALYZER = create("levitite_catalyzer");
		public static final TagKey<Block> LEVITITE_SOUL_CATALYZER = create("levitite_soul_catalyzer"); // blocks that start soul crystallization
		public static final TagKey<Block> LEVITITE_ADJACENT_CATALYZER = create("levitite_adjacent_catalyzer"); // blocks that cause soul crystallization to become regular crystallization
		public static final TagKey<Block> LEVITITE_ADJACENT_SOUL_CATALYZER = create("levitite_adjacent_soul_catalyzer"); // blocks that cause regular crystallization to become soul crystallization

		private static TagKey<Block> create(final String path) {
			return TagKey.create(Registries.BLOCK, Aeronautics.path(path));
		}
	}

	public static class ItemTags {
		public static final TagKey<Item> LEATHERS = SimTags.commonItemTag("leathers");
		public static final TagKey<Item> ARMORS = SimTags.commonItemTag("armors");
		public static final TagKey<Item> HEAD_ARMOR = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("head_armor"));
		public static final TagKey<Item> IRON_SHEET = SimTags.commonItemTag("plates/iron");
		public static final TagKey<Item> GOLD_SHEET = SimTags.commonItemTag("plates/gold");
		public static final TagKey<Item> MUSIC_DISCS = SimTags.commonItemTag("music_discs");

		public static final TagKey<Item> ENVELOPE = create("envelope");
		public static final TagKey<Item> SHAFTLESS_ENVELOPE = create("shaftless_envelope");
		public static final TagKey<Item> LEVITITE_CATALYZER = create("levitite_catalyzer");
		public static final TagKey<Item> LEVITITE_SOUL_CATALYZER = create("levitite_soul_catalyzer");
		public static final TagKey<Item> LEVITITE_CATALYZER_NO_CONSUME = create("levitite_catalyzer_no_consume");
		public static final TagKey<Item> LEVITITE = create("levitite");
		public static final TagKey<Item> BURNER_FIRE = create("burner_fire");
		public static final TagKey<Item> CONVERTS_TO_CLOUD_SKIPPER = create("converts_to_cloud_skipper");

		private static TagKey<Item> create(final String path) {
			return TagKey.create(Registries.ITEM, Aeronautics.path(path));
		}
	}

	public static class FluidTags {
		/**
		 * Levitite Blend (still + flowing). Fabric replacement for the NeoForge fluid type: entities track their
		 * immersion in it through {@code Entity#updateFluidHeightAndDoFluidPushing(tag, ...)}.
		 */
		public static final TagKey<Fluid> LEVITITE_BLEND = TagKey.create(Registries.FLUID, Aeronautics.path("levitite_blend"));
	}
}
