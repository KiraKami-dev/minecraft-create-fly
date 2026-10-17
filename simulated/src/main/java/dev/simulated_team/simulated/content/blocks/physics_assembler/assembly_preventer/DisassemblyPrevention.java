package dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer;

import com.zurrtum.create.content.contraptions.AssemblyException;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.service.SimConfigService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

public class DisassemblyPrevention {
    // a plain component instead of SimLang.builder(): LangBuilder is a client class in Create Fly and this runs on the server
    private static final MutableComponent ERR = Component.translatable(Simulated.MOD_ID + ".prevent_disassembly");

    /**
     * Attempts to get a sublevel, and check it for a primary assembler, throwing if one is found and the given pos it not it.
     *
     * @param level   The level to check in.
     * @param toCheck The blockpos to check
     * @throws AssemblyException Whenever the given block position is not the primary assembler
     */
    public static boolean checkSubLevelForPrimary(final Level level, final BlockPos toCheck) throws AssemblyException {
        if (!SimConfigService.INSTANCE.server().assembly.primaryDisassembly.get() || (level == null & toCheck == null)) {
            return true;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, toCheck);
        if (subLevel instanceof final ServerSubLevel ssl) {
            final BlockPos primary = ((PrimaryAssemblerExtension) ssl).simulated$getPrimaryAssembler();

            if (primary != null) {
                if (level.getBlockEntity(primary) instanceof final PhysicsAssemblerBlockEntity psbe) {
                    if (!toCheck.equals(primary) || !psbe.isPrimaryAssembler()) { //if the checking position
                        throw new AssemblyException(ERR);
                    }
                }
            }
        }

        return true;
    }
}
