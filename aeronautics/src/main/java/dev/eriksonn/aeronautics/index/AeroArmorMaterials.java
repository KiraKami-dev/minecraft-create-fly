package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Map;

/**
 * Armor materials. Since 1.21.2 they are plain records (no registry) and armor is an item with an
 * {@link Equippable} component whose asset ({@code assets/aeronautics/equipment/aviators_goggles.json}) points at the
 * layer textures (upstream: {@code ArmorMaterial.Layer(aeronautics:aviators_goggles)}).
 */
public class AeroArmorMaterials {
	public static final ResourceKey<EquipmentAsset> AVIATORS_GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Aeronautics.path("aviators_goggles"));

	/**
	 * Upstream: helmet defense 1, enchantability 15, leather equip sound, repaired with leather, no toughness or
	 * knockback resistance. The goggles have no durability (upstream registered them without one).
	 */
	public static final ArmorMaterial AVIATORS_GOGGLES = new ArmorMaterial(
			0,
			Map.of(ArmorType.HELMET, 1),
			15,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			0.0f,
			0.0f,
			ItemTags.REPAIRS_LEATHER_ARMOR,
			AVIATORS_GOGGLES_ASSET
	);

	/**
	 * Item properties of a helmet made of the given material (what upstream's {@code ArmorItem} did).
	 */
	public static Item.Properties helmet(final ArmorMaterial material) {
		return new Item.Properties()
				.stacksTo(1)
				.attributes(material.createAttributes(ArmorType.HELMET))
				.enchantable(material.enchantmentValue())
				.repairable(material.repairIngredient())
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
						.setEquipSound(material.equipSound())
						.setAsset(material.assetId())
						.build());
	}

	public static void register() {}
}
