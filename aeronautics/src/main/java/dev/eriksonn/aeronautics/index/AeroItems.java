package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.infrastructure.fluids.FlowableFluid;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.content.items.AviatorsGogglesItem;
import dev.simulated_team.simulated.registrate.DyedBlockList;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms.VisibilityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Aeronautics' items, block items included (Create Fly style). Registration order is the order of the Aeronautics
 * section of the Simulated creative tab: block items in {@link AeroBlocks} order, then the plain items, then the
 * bucket (upstream: Registrate accept order).
 */
public class AeroItems {
	private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();

	// Block items

	public static final Map<DyeColor, BlockItem> ENVELOPES = dyedBlockItems(AeroBlocks.DYED_ENVELOPE_BLOCKS, DyeColor.WHITE);
	public static final BlockItem WHITE_ENVELOPE = ENVELOPES.get(DyeColor.WHITE);
	public static final Map<DyeColor, BlockItem> ENVELOPE_ENCASED_SHAFTS = dyedBlockItems(AeroBlocks.ENVELOPE_ENCASED_SHAFTS, null);
	public static final BlockItem HOT_AIR_BURNER = REGISTRATE.blockItem(AeroBlocks.HOT_AIR_BURNER);
	public static final BlockItem STEAM_VENT = REGISTRATE.blockItem(AeroBlocks.STEAM_VENT);
	public static final BlockItem PROPELLER_BEARING = REGISTRATE.blockItem(AeroBlocks.PROPELLER_BEARING);
	public static final BlockItem GYROSCOPIC_PROPELLER_BEARING = REGISTRATE.blockItem(AeroBlocks.GYROSCOPIC_PROPELLER_BEARING);
	public static final BlockItem SMART_PROPELLER = REGISTRATE.blockItem(AeroBlocks.SMART_PROPELLER);
	public static final BlockItem ANDESITE_PROPELLER = REGISTRATE.blockItem(AeroBlocks.ANDESITE_PROPELLER);
	public static final BlockItem WOODEN_PROPELLER = REGISTRATE.blockItem(AeroBlocks.WOODEN_PROPELLER);
	public static final BlockItem MOUNTED_POTATO_CANNON = REGISTRATE.blockItem(AeroBlocks.MOUNTED_POTATO_CANNON);
	public static final BlockItem LEVITITE = REGISTRATE.blockItem(AeroBlocks.LEVITITE, BlockItem::new,
			new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.LEVITITE));
	public static final BlockItem PEARLESCENT_LEVITITE = REGISTRATE.blockItem(AeroBlocks.PEARLESCENT_LEVITITE, BlockItem::new,
			new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.PEARLESCENT_LEVITITE));

	// Items

	public static final AviatorsGogglesItem AVIATORS_GOGGLES = REGISTRATE.item(
			"aviators_goggles",
			AviatorsGogglesItem::new,
			AeroArmorMaterials.helmet(AeroArmorMaterials.AVIATORS_GOGGLES)
	);

	public static final Item MUSIC_DISC_CLOUD_SKIPPER = REGISTRATE.item(
			"music_disc_cloud_skipper",
			Item::new,
			new Item.Properties()
					.stacksTo(1)
					.rarity(Rarity.RARE)
					.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, Aeronautics.path("cloud_skipper")))
					.component(AeroDataComponents.LEVITATING, Levitating.DEFAULT)
	);

	public static final Item ENDSTONE_POWDER = REGISTRATE.item(
			"end_stone_powder",
			Item::new,
			new Item.Properties().component(AeroDataComponents.LEVITATING, Levitating.END_STONE)
	);

	// Buckets (upstream: the NeoForge module's fluid builder, registered after everything else)

	public static final BucketItem LEVITITE_BLEND_BUCKET = bucket(AeroFluids.LEVITITE_BLEND);

	private static BucketItem bucket(final FlowableFluid fluid) {
		final BucketItem bucket = REGISTRATE.item(
				BuiltInRegistries.FLUID.getKey(fluid).getPath() + "_bucket",
				p -> new BucketItem(fluid, p),
				new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
		);
		fluid.getEntry().bucket = bucket;
		return bucket;
	}

	/**
	 * Block items of a dyed block family; every color but {@code visibleColor} is hidden from the creative tab (only
	 * in search, or invisible when {@code visibleColor} is null).
	 */
	private static <B extends Block> Map<DyeColor, BlockItem> dyedBlockItems(final DyedBlockList<B> blocks, @Nullable final DyeColor visibleColor) {
		final Map<DyeColor, BlockItem> items = new EnumMap<>(DyeColor.class);
		for (final DyeColor color : DyeColor.values()) {
			final BlockItem item = REGISTRATE.blockItem(blocks.get(color));
			if (visibleColor == null) {
				VisibilityType.INVISIBLE.apply(item);
			} else if (color != visibleColor) {
				VisibilityType.SEARCH_ONLY.apply(item);
			}
			items.put(color, item);
		}
		return Collections.unmodifiableMap(items);
	}

	public static void register() {}
}
