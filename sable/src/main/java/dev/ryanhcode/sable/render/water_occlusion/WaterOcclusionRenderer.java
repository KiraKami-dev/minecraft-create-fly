package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Manages water occlusion rendering for sub-levels.
 * <p>
 * Every frame, right before the translucent terrain layer, the occlusion volumes are rendered depth-only twice:
 * the nearest front faces into the "close" buffer, and the nearest back faces into the "far" buffer.
 * The translucent terrain pipeline is then swapped for {@link #TRANSLUCENT_TERRAIN_OCCLUDED}, which discards any
 * fragment lying between the two depths.
 * <p>
 * GL_DEPTH_CLAMP is not exposed by the blaze3d {@link RenderPipeline}, so it is toggled with raw GL calls around
 * the volume draws, as upstream does. Inside a hull the close depth is additionally cleared to 0.
 */
@ApiStatus.Internal
public class WaterOcclusionRenderer {

    public static final String CLOSE_SAMPLER_NAME = "SableCloseSampler";
    public static final String FAR_SAMPLER_NAME = "SableFarSampler";

    /**
     * Depth-only pass for occlusion volumes
     */
    private static final RenderPipeline DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/water_occlusion_depth"))
            .withVertexShader("core/rendertype_water_mask")
            .withFragmentShader("core/rendertype_water_mask")
            .withColorWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .build();

    /**
     * Vanilla translucent terrain, with water inside occlusion volumes discarded
     */
    public static final RenderPipeline TRANSLUCENT_TERRAIN_OCCLUDED = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/translucent_terrain_occluded"))
            .withFragmentShader(Sable.sablePath("core/terrain_occluded"))
            .withSampler(CLOSE_SAMPLER_NAME)
            .withSampler(FAR_SAMPLER_NAME)
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .build();

    private static boolean isEnabled = false;

    private final Set<WaterOcclusionRenderRegion> regions = new ObjectOpenHashSet<>();
    private final Matrix4f levelModelView = new Matrix4f();
    private RenderTarget closeTarget;
    private RenderTarget farTarget;
    private Level level;
    private boolean active = false;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        WaterOcclusionRenderer.isEnabled = isEnabled;
    }

    @Nullable
    @ApiStatus.Internal
    public WaterOcclusionRenderRegion addRegion(final Collection<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return null;
        }

        final WaterOcclusionRenderRegion region = new WaterOcclusionRenderRegion(blocks);
        this.regions.add(region);
        return region;
    }

    public void removeRegion(@Nullable final WaterOcclusionRenderRegion region) {
        if (region == null) {
            return;
        }

        region.free();
        this.regions.remove(region);
    }

    private void updateTargets(final boolean needed) {
        final RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();

        if (!needed) {
            this.freeTargets();
            return;
        }

        if (this.closeTarget == null || mainTarget.width != this.closeTarget.width || mainTarget.height != this.closeTarget.height) {
            this.freeTargets();
            this.closeTarget = new TextureTarget("Sable water occlusion (close)", mainTarget.width, mainTarget.height, true);
            this.farTarget = new TextureTarget("Sable water occlusion (far)", mainTarget.width, mainTarget.height, true);
        }
    }

    private void freeTargets() {
        if (this.closeTarget != null) {
            this.closeTarget.destroyBuffers();
            this.farTarget.destroyBuffers();
            this.closeTarget = null;
            this.farTarget = null;
        }
    }

    /**
     * Stores the model-view matrix of the level for this frame.
     */
    public void setLevelModelView(final Matrix4fc modelView) {
        this.levelModelView.set(modelView);
    }

    /**
     * Renders the occlusion buffers. Must be called outside of any open render pass, before the translucent terrain layer.
     */
    public void preRenderTranslucent() {
        this.active = false;

        if (!isEnabled() || this.level == null) {
            this.updateTargets(false);
            return;
        }

        final WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);
        final boolean needed = !this.regions.isEmpty() && container != null;

        this.updateTargets(needed);

        if (!needed) {
            return;
        }

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();

        // if we're inside any of the regions, we need to clear the depth to 0
        final boolean cameraOccluded = container.isOccluded(cameraPos);

        this.renderDepth(this.closeTarget, "close", cameraOccluded ? 0.0 : 1.0, this.levelModelView);

        // Mirroring the view in x flips the winding order of every face, so the culled "back" faces become the
        // front faces and the far buffer holds the nearest back faces. The shader samples it mirrored.
        final Matrix4f mirrored = new Matrix4f().scale(-1.0f, 1.0f, 1.0f).mul(this.levelModelView);
        this.renderDepth(this.farTarget, "far", 1.0, mirrored);

        this.active = true;
    }

    private void renderDepth(final RenderTarget target, final String label, final double clearDepth, final Matrix4fc modelView) {
        // uniforms and index buffers can't be uploaded while a render pass is open
        final Map<WaterOcclusionRenderRegion, GpuBufferSlice> transforms = new IdentityHashMap<>();
        for (final WaterOcclusionRenderRegion region : this.regions) {
            final GpuBufferSlice transform = region.prepare(modelView);
            if (transform != null) {
                transforms.put(region, transform);
            }
        }

        try (final RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Sable water occlusion (" + label + ")", target.getColorTextureView(), OptionalInt.of(0), target.getDepthTextureView(), OptionalDouble.of(clearDepth))) {
            renderPass.setPipeline(DEPTH_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);


            // Volumes crossing the near plane would be clipped instead of clamped to depth 0. blaze3d draws
            // immediately and never touches this flag, so a raw GL toggle around the draws is enough.
            GL11C.glEnable(GL32C.GL_DEPTH_CLAMP);
            try {
                transforms.forEach((region, transform) -> region.draw(renderPass, transform));
            } finally {
                GL11C.glDisable(GL32C.GL_DEPTH_CLAMP);
            }
        }
    }

    /**
     * @return if the occluded translucent terrain pipeline should replace the vanilla one this frame
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Binds the occlusion depth buffers to a pass using {@link #TRANSLUCENT_TERRAIN_OCCLUDED}
     */
    public void bindSamplers(final RenderPass renderPass) {
        final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        renderPass.bindTexture(CLOSE_SAMPLER_NAME, this.closeTarget.getDepthTextureView(), sampler);
        renderPass.bindTexture(FAR_SAMPLER_NAME, this.farTarget.getDepthTextureView(), sampler);
    }

    public void update() {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != this.level) {
            this.level = minecraft.level;

            this.regions.forEach(WaterOcclusionRenderRegion::free);
            this.regions.clear();
        }
    }

}
