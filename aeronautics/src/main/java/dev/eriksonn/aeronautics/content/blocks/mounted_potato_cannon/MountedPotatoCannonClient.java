package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.content.equipment.potatoCannon.PotatoCannonItem;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client-only parts of the mounted potato cannon, split off its block entity the way Create Fly splits its own: the
 * goggle tooltip.
 */
public final class MountedPotatoCannonClient {
    private MountedPotatoCannonClient() {
    }

    /**
     * The tooltip behaviour replaces the default kinetic tooltip behaviour (same behaviour type), so this must run after
     * the default kinetic behaviours of {@link AeroBlockEntityTypes#MOUNTED_POTATO_CANNON} are registered.
     */
    public static void registerClient() {
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.MOUNTED_POTATO_CANNON, MountedPotatoCannonTooltipBehaviour::new);
    }

    /**
     * Upstream {@code MountedPotatoCannonBlockEntity#addToGoggleTooltip}
     */
    public static class MountedPotatoCannonTooltipBehaviour extends KineticTooltipBehaviour<MountedPotatoCannonBlockEntity> {
        public MountedPotatoCannonTooltipBehaviour(final MountedPotatoCannonBlockEntity be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
            super.addToGoggleTooltip(tooltip, isPlayerSneaking);

            final MountedPotatoCannonBlockEntity be = this.blockEntity;
            AeroLang.emptyLine(tooltip);
            AeroLang.blockName(be.getBlockState()).text(":").forGoggles(tooltip);

            final PotatoCannonItem.Ammo ammo = be.getInventory().getAmmo();
            if (ammo != null) {
                final PotatoCannonProjectileType type = ammo.type();

                final ItemStack currentStack = be.getInventory().slot.getStack();
                AeroLang.translate("potato_cannon.ammo", currentStack.getDisplayName(), currentStack.getCount())
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);

                final float damage = type.damage() * 2.0f;
                AeroLang.translate("potato_cannon.attack_damage", damage)
                        .style(ChatFormatting.DARK_GREEN)
                        .forGoggles(tooltip, 1);

                if (Math.abs(be.getSpeed()) > 0)
                    AeroLang.translate("potato_cannon.reload_ticks", Math.round(1 / be.getChargeUpSpeed()))
                            .style(ChatFormatting.DARK_GREEN)
                            .forGoggles(tooltip, 1);

                AeroLang.translate("potato_cannon.knockback", type.knockback())
                        .style(ChatFormatting.DARK_GREEN)
                        .forGoggles(tooltip, 1);

            } else {
                return false;
            }

            return true;
        }
    }
}
