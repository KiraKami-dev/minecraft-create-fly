package dev.simulated_team.simulated.util.assembly;

import com.zurrtum.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.SimConfigService;
import net.minecraft.network.chat.Component;

/**
 * Thrown on the server, so the messages are plain components: SimLang's LangBuilder is a client class in Create Fly.
 */
public class SimAssemblyException {

    public static AssemblyException structureTooLarge() {
        return new AssemblyException("structureTooLarge", SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get());
    }

    public static AssemblyException couldNotAlign() {
        return new AssemblyException(Component.translatable(Simulated.MOD_ID + ".gui.assembly.exception.couldNotAlign"));
    }

    public static AssemblyException outOfWorld() {
        return new AssemblyException(Component.translatable(Simulated.MOD_ID + ".gui.assembly.exception.outOfWorld"));
    }

    public static AssemblyException tooFarFromGround() {
        return new AssemblyException(Component.translatable(Simulated.MOD_ID + ".gui.assembly.exception.tooFarFromGround"));
    }

    public static AssemblyException tooFast() {
        return new AssemblyException(Component.translatable(Simulated.MOD_ID + ".gui.assembly.exception.tooFast"));
    }

    public static AssemblyException sameSubLevel() {
        return new AssemblyException(Component.translatable(Simulated.MOD_ID + ".gui.assembly.exception.sameSubLevel"));
    }
}
