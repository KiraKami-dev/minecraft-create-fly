package dev.simulated_team.simulated.index;

import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.item.ItemDescription;
import com.zurrtum.create.client.foundation.item.KineticStats;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.client.foundation.item.TooltipModifier;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

/**
 * Client: item tooltips (upstream: the registrate's tooltip modifier factory, applied to every item it registered,
 * plus {@code ItemDescription.useKey} calls of some block entries).
 */
public class SimItemTooltips {

    /**
     * Create's item description + kinetic stats, with an epic purple palette for epic items.
     */
    public static final Function<Item, TooltipModifier> DEFAULT_FACTORY = item -> {
        final Rarity rarity = item.getDefaultInstance().getRarity();
        FontHelper.Palette color = FontHelper.Palette.STANDARD_CREATE;
        if (rarity == Rarity.EPIC)
            color = new FontHelper.Palette(TooltipHelper.styleFromColor(SimColors.EPIC_OURPLE), TooltipHelper.styleFromColor(rarity.color()));

        return new ItemDescription
                .Modifier(item, color)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
    };

    /**
     * Registers the tooltip modifier for every item of the given registrate.
     */
    public static void registerAll(final SimulatedRegistrate registrate, final Function<Item, TooltipModifier> factory) {
        for (final Item item : registrate.getItems()) {
            TooltipModifier.REGISTRY.register(item, factory.apply(item));
        }
    }

    public static void register(final SimulatedRegistrate registrate) {
        registerAll(registrate, DEFAULT_FACTORY);

        // every handle and nameplate shares one description
        ItemDescription.useKey(SimBlocks.IRON_HANDLE, "block.simulated.handle");
        ItemDescription.useKey(SimBlocks.COPPER_HANDLE, "block.simulated.handle");
        for (final Block handle : SimBlocks.DYED_HANDLES) {
            ItemDescription.useKey(handle, "block.simulated.handle");
        }
        for (final Block nameplate : SimBlocks.NAMEPLATES) {
            ItemDescription.useKey(nameplate, "block.simulated.nameplate");
        }
    }
}
