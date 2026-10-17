package dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.frogports;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers the sub-level of the block entity a Create nameplate render state was extracted for
 */
public interface NameplateRenderStateExtension {
    void sable$setSubLevel(@Nullable ClientSubLevel subLevel);

    @Nullable
    ClientSubLevel sable$getSubLevel();
}
