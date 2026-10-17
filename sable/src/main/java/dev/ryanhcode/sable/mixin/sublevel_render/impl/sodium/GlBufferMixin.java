package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.mojang.blaze3d.opengl.GlBuffer;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.GlBufferAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlBuffer.class)
public class GlBufferMixin implements GlBufferAccessor {

    @Shadow
    protected int handle;

    @Override
    public int sable$getHandle() {
        return this.handle;
    }
}
