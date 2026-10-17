package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.foundation.gui.menu.MenuBase;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.fabric.menu.LinkedTypewriterMenuImpl;
import dev.simulated_team.simulated.service.SimMenuService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FabricSimMenuService implements SimMenuService {

	@Override
	@SuppressWarnings("unchecked")
	public <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(final MenuType<LinkedTypewriterBlockEntity> type, final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
		return (T) new LinkedTypewriterMenuImpl(type, id, inv, be);
	}

	/**
	 * Create Fly opens its menus with its own open-screen packet, which carries the menu's Create Fly menu type and the
	 * extra data; the client looks up the screen factory registered for that type in {@code AllMenuScreens}.
	 */
	@Override
	public void openScreen(final ServerPlayer player, final MenuProvider factory, final Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
		com.zurrtum.create.foundation.gui.menu.MenuProvider.openHandledScreen(player, new com.zurrtum.create.foundation.gui.menu.MenuProvider() {
			@Override
			public Component getDisplayName() {
				return factory.getDisplayName();
			}

			@Override
			public @Nullable MenuBase<?> createMenu(final int syncId, final Inventory playerInventory, final Player menuPlayer, final RegistryFriendlyByteBuf extraData) {
				final AbstractContainerMenu menu = factory.createMenu(syncId, playerInventory, menuPlayer);
				if (!(menu instanceof final MenuBase<?> menuBase)) {
					if (menu != null) {
						Simulated.LOGGER.error("Cannot open menu {}: Create Fly menus must extend MenuBase", menu.getClass().getName());
					}
					return null;
				}

				extraDataWriter.accept(extraData);
				return menuBase;
			}
		});
	}
}
