package dev.eriksonn.aeronautics.content.items;

import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Upstream extended Create's {@code BaseArmorItem} (an {@code ArmorItem} with its own layer texture). Since 1.21.2 armor
 * is plain {@link Item} properties: the registration gives it the helmet armor data of
 * {@code AeroArmorMaterials.AVIATORS_GOGGLES} (attributes, enchantability, equippable with the {@link #TEXTURE} equipment
 * asset, repair item).
 */
public class AviatorsGogglesItem extends Item {
	public static final ArmorType TYPE = ArmorType.HELMET;
	public static final Identifier TEXTURE = Aeronautics.path("aviators_goggles");

	public AviatorsGogglesItem(final Properties properties) {
		super(properties);
		GogglesItem.addIsWearingPredicate(player -> player.getItemBySlot(EquipmentSlot.HEAD).is(AeroItems.AVIATORS_GOGGLES));
	}
}
