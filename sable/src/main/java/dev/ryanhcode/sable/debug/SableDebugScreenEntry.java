package dev.ryanhcode.sable.debug;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sable section of the F3 screen. On 1.21.11 the debug screen is built from {@link DebugScreenEntries}, which can be
 * toggled in the debug options screen.
 */
@ApiStatus.Internal
public class SableDebugScreenEntry implements DebugScreenEntry {

    public static final Identifier ID = Sable.sablePath("sub_levels");

    public static void register() {
        DebugScreenEntries.register(ID, new SableDebugScreenEntry());
    }

    @Override
    public void display(final DebugScreenDisplayer displayer, @Nullable final Level level, @Nullable final LevelChunk clientChunk, @Nullable final LevelChunk serverChunk) {
        final List<String> lines = new ArrayList<>();
        lines.add(ChatFormatting.UNDERLINE + "Sable");

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
        if (container != null) {
            container.addDebugInfo(lines::add);
        }
        SubLevelRenderDispatcher.get().addDebugInfo(lines::add);

        displayer.addToGroup(ID, lines);
    }

    @Override
    public boolean isAllowed(final boolean reducedDebugInfo) {
        return true;
    }
}
