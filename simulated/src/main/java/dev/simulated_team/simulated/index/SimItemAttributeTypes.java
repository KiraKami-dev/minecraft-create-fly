package dev.simulated_team.simulated.index;

import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.item_attributes.BlockBouncinessItemAttribute;
import dev.simulated_team.simulated.content.item_attributes.BlockFrictionItemAttribute;
import dev.simulated_team.simulated.content.item_attributes.BlockMassItemAttribute;
import net.minecraft.core.Registry;

public class SimItemAttributeTypes {
	public static final BlockMassItemAttribute.Type BLOCK_MASS = register("block_mass", new BlockMassItemAttribute.Type());
	public static final BlockFrictionItemAttribute.Type BLOCK_FRICTION = register("block_friction", new BlockFrictionItemAttribute.Type());
	public static final BlockBouncinessItemAttribute.Type BLOCK_BOUNCINESS = register("block_bounciness", new BlockBouncinessItemAttribute.Type());

	private static <T extends ItemAttributeType> T register(final String name, final T type) {
		return Registry.register(CreateRegistries.ITEM_ATTRIBUTE_TYPE, Simulated.path(name), type);
	}

	public static void init() {

	}
}
