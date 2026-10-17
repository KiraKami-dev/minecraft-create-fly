package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.zurrtum.create.foundation.gui.menu.GhostItemMenu;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.index.SimMenuTypes;
import dev.simulated_team.simulated.service.SimMenuService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The linked typewriter menu, a Create Fly {@link GhostItemMenu} around the typewriter block entity. The ghost slots
 * are added by the loader half ({@code fabric/menu/LinkedTypewriterMenuImpl}).
 * <p>
 * {@link #create} is the menu factory of {@code SimMenuTypes.LINKED_TYPEWRITER} (a Create Fly
 * {@link MenuType}); the screen factory is {@link LinkedTypewriterScreen#create}.
 */
public abstract class LinkedTypewriterMenuCommon extends GhostItemMenu<LinkedTypewriterBlockEntity> {

    public boolean slotsActive = false;

    public LinkedTypewriterMenuCommon(final MenuType<LinkedTypewriterBlockEntity> type, final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
        super(type, id, inv, be);
    }

    public static LinkedTypewriterMenuCommon create(final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
        return SimMenuService.INSTANCE.getLoaderLinkedTypewriter(SimMenuTypes.LINKED_TYPEWRITER, id, inv, be);
    }

    @Override
    protected void addPlayerSlots(final int x, final int y) {
        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            this.addSlot(new PlayerSlot(this.playerInventory, hotbarSlot, x + hotbarSlot * 18, y + 58));
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new PlayerSlot(this.playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    @Override
    protected void saveData(final LinkedTypewriterBlockEntity be) {}

    @Override
    protected boolean allowRepeats() {
        return false;
    }

    private class PlayerSlot extends Slot {
        public PlayerSlot(final Container pContainer, final int pIndex, final int pX, final int pY) {
            super(pContainer, pIndex, pX, pY);
        }

        @Override
        public boolean isActive() {
            return LinkedTypewriterMenuCommon.this.slotsActive;
        }
    }
}
