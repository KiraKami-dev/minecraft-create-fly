package dev.simulated_team.simulated.data;

import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimRecipeSerializers;
import dev.simulated_team.simulated.index.SimTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simulated:portable_engine_dyeing}: one portable engine + one dye gives the engine of that color, keeping its
 * components. Upstream lived in the NeoForge module.
 */
public class PortableEngineDyeingRecipe extends CustomRecipe {

    public PortableEngineDyeingRecipe(final CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        int engines = 0;
        int dyes = 0;

        for (int i = 0; i < input.size(); ++i) {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                    ++engines;
                } else {
                    if (!stack.is(ConventionalItemTags.DYES))
                        return false;
                    ++dyes;
                }

                if (dyes > 1 || engines > 1) {
                    return false;
                }
            }
        }

        return engines == 1 && dyes == 1;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        ItemStack engine = ItemStack.EMPTY;
        DyeColor color = DyeColor.RED;

        for (int i = 0; i < input.size(); ++i) {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                    engine = stack;
                } else {
                    final DyeColor color1 = getColor(stack);
                    if (color1 != null) {
                        color = color1;
                    }
                }
            }
        }

        final ItemStack dyedEngine = new ItemStack(SimBlocks.PORTABLE_ENGINES.get(color));
        if (!engine.isComponentsPatchEmpty()) {
            dyedEngine.applyComponents(engine.getComponentsPatch());
        }

        return dyedEngine;
    }

    /**
     * NeoForge's {@code DyeColor.getColor(ItemStack)}: dye items, then the {@code c:dyes/<color>} tags.
     */
    @Nullable
    private static DyeColor getColor(final ItemStack stack) {
        if (stack.getItem() instanceof final DyeItem dyeItem) {
            return dyeItem.getDyeColor();
        }

        for (final DyeColor color : DyeColor.values()) {
            if (stack.is(SimTags.Items.dyesTag(color))) {
                return color;
            }
        }
        return null;
    }

    @Override
    public RecipeSerializer<PortableEngineDyeingRecipe> getSerializer() {
        return SimRecipeSerializers.PORTABLE_ENGINE_DYEING;
    }

}
