package dev.ryanhcode.sable.fabric.compatibility.create;

import com.zurrtum.create.catnip.levelWrappers.WrappedServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * References Create (Fly) classes, so it must only be loaded when the {@code create} mod is present.
 */
public final class CreateLevelWrappers {
    private CreateLevelWrappers() {
    }

    public static boolean isWrapped(@Nullable final Level level) {
        return level instanceof WrappedServerLevel;
    }
}
