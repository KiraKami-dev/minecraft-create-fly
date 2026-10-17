package dev.simulated_team.simulated.fabric.menu;

import com.zurrtum.create.foundation.gui.menu.MenuType;
import com.zurrtum.create.infrastructure.items.ItemStackHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Fabric half of the linked typewriter menu: the ghost inventory is a Create Fly {@link ItemStackHandler} (a vanilla
 * container), so the ghost slots are plain slots, like Create Fly's own ghost item menus.
 */
public class LinkedTypewriterMenuImpl extends LinkedTypewriterMenuCommon {

    public LinkedTypewriterMenuImpl(final MenuType<LinkedTypewriterBlockEntity> type, final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
        super(type, id, inv, be);
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
            return LinkedTypewriterMenuImpl.this.slotsActive;
        }
    }
}
