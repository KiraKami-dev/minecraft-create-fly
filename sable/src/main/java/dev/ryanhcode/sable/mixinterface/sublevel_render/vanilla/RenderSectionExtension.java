package dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface RenderSectionExtension {

    void sable$addDirtyListener(DirtyListener listener);

    void sable$setListening(boolean listening);

    /**
     * Sets the camera position used for sorting translucent geometry, in the coordinate space of the section.
     * Sub-level sections live in plot space, so the world camera position can't be used for them.
     *
     * @param position The camera position in section space, or <code>null</code> to use the world camera
     */
    void sable$setSortCameraPosition(@Nullable Vec3 position);

    /**
     * @return The camera position used for sorting translucent geometry, or <code>null</code> to use the world camera
     */
    @Nullable
    Vec3 sable$getSortCameraPosition();

    @FunctionalInterface
    interface DirtyListener {

        void markDirty(SectionRenderDispatcher.RenderSection section);
    }
}
