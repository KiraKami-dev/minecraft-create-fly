package dev.simulated_team.simulated.index;

import com.zurrtum.create.foundation.gui.menu.MenuType;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;

/**
 * Menu types, using Create Fly's menu system (menus extend {@code MenuBase<H>}, are opened with Create Fly's
 * {@code MenuProvider#openHandledScreen} and their screens are registered in {@code AllMenuScreens}).
 * Screens are registered on the client in {@code SimMenuScreens}.
 */
public class SimMenuTypes {

    public static final MenuType<LinkedTypewriterBlockEntity> LINKED_TYPEWRITER = register("linked_typewriter", LinkedTypewriterMenuCommon::create);

    private static <H> MenuType<H> register(final String name, final MenuType<H> type) {
        return Simulated.getRegistrate().menu(name, type);
    }

    public static void register() {}
}
