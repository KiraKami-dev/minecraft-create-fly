package dev.ryanhcode.sable.mixin.sublevel_render;

import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Lets the sub-level renderer swap the camera position used by block entity renderers while it extracts sub-level block
 * entities, since those live in plot space.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin implements BlockEntityRenderDispatcherExtension {

    @Shadow
    private Vec3 cameraPos;

    @Unique
    private @Nullable Vec3 sable$worldCameraPos;

    @Override
    public void sable$setCameraPosition(@Nullable final Vec3 pos) {
        if (pos != null) {
            if (this.sable$worldCameraPos == null) {
                this.sable$worldCameraPos = this.cameraPos;
            }
            this.cameraPos = pos;
        } else if (this.sable$worldCameraPos != null) {
            this.cameraPos = this.sable$worldCameraPos;
            this.sable$worldCameraPos = null;
        }
    }
}
