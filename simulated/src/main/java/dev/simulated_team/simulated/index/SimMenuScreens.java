package dev.simulated_team.simulated.index;

import com.zurrtum.create.client.AllMenuScreens;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;

/**
 * Client: menu screens, registered in Create Fly's {@link AllMenuScreens} (opened by Create Fly's open-screen packet).
 */
public class SimMenuScreens {

    public static void register() {
        AllMenuScreens.register(SimMenuTypes.LINKED_TYPEWRITER, LinkedTypewriterScreen::create);
    }
}
