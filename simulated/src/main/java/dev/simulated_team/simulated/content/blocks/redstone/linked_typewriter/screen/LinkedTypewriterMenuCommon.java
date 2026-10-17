package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.zurrtum.create.foundation.gui.menu.GhostItemMenu;
import com.zurrtum.create.infrastructure.items.ItemStackHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.index.SimMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The linked typewriter menu. On 1.21.1 the ghost slots lived in a loader specific subclass
 * ({@code LinkedTypewriterMenuImpl}); Create Fly's {@link GhostItemMenu} works on plain containers, so the menu is
 * a single class now. Opened through {@link LinkedTypewriterBlockEntity#createMenu}.
 */
public class LinkedTypewriterMenuCommon extends GhostItemMenu<LinkedTypewriterBlockEntity> {

    public boolean slotsActive = false;

    public LinkedTypewriterMenuCommon(final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
        super(SimMenuTypes.LINKED_TYPEWRITER, id, inv, be);
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return new ItemStackHandler(2);
    }

    @Override
    protected void addSlots() {
        this.addPlayerSlots(6 + (16 * 2), 11 + (16 * 3));

        for (int i = 0; i < 2; i++) {
            this.addSlot(new GhostSlot(this.ghostInventory, i, 105 + (i * 18), 1));
        }
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

    private class GhostSlot extends Slot {
        public GhostSlot(final Container container, final int index, final int xPosition, final int yPosition) {
            super(container, index, xPosition, yPosition);
        }

        @Override
        public boolean isFake() {
            return true;
        }

        @Override
        public boolean isActive() {
            return LinkedTypewriterMenuCommon.this.slotsActive;
        }
    }
}
