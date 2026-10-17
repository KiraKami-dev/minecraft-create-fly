package dev.simulated_team.simulated.mixin.end_sea;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.simulated_team.simulated.content.end_sea.EndSeaRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    /**
     * Upstream drew the end sea right before {@code renderDebug}, at the end of the level. On 1.21.11 that is the
     * late debug pass of the frame graph, so the end sea gets its own pass right before it.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"))
    public void renderLevel(final GraphicsResourceAllocator allocator,
                            final DeltaTracker deltaTracker,
                            final boolean renderBlockOutline,
                            final Camera camera,
                            final Matrix4f viewMatrix,
                            final Matrix4f projectionMatrix,
                            final Matrix4f cullProjectionMatrix,
                            final GpuBufferSlice fog,
                            final Vector4f fogColor,
                            final boolean renderSky,
                            final CallbackInfo ci,
                            @Local final FrameGraphBuilder frameGraphBuilder) {
        EndSeaRenderer.addPass(frameGraphBuilder, this.targets, camera);
    }

}
