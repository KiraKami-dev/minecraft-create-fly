package dev.simulated_team.simulated.service;

import com.zurrtum.create.foundation.gui.menu.MenuType;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Consumer;

public interface SimMenuService {

	SimMenuService INSTANCE = ServiceUtil.load(SimMenuService.class);

	/**
	 * Creates the loader specific linked typewriter menu. Create Fly menus are always built around their content holder:
	 * on the client the screen factory reads the block entity from the extra data and then calls this.
	 */
	<T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(MenuType<LinkedTypewriterBlockEntity> type, int id, Inventory inv, LinkedTypewriterBlockEntity be);

	/**
	 * Opens a menu whose {@link MenuProvider#createMenu} returns a Create Fly {@link com.zurrtum.create.foundation.gui.menu.MenuBase},
	 * sending the extra data written by {@code extraDataWriter} to the client screen factory.
	 */
	void openScreen(ServerPlayer player, MenuProvider factory, Consumer<RegistryFriendlyByteBuf> extraDataWriter);
}
