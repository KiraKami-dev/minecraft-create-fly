package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.*;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

// todo good rendering for negative physics depthGradient?
/**
 * Renders the end-sea effect
 * <p>
 * 1.21.11 port: the Veil shader program is now the {@link #PIPELINE} render pipeline, drawn in a frame pass that the
 * level renderer adds right before its late debug pass (where upstream hooked in before {@code renderDebug}). The
 * shadow map is rendered in the same pass, right before the layers.
 */
public class EndSeaRenderer {

    private static final Identifier SHADER = Simulated.path("core/end_sea");
    private static final Identifier SKY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/end_portal.png");

    /**
     * Additive, depth tested layers without depth writes or culling
     */
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Simulated.path("pipeline/end_sea"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withSampler("SkySampler")
            .withSampler("ShadowDepthSampler")
            .withSampler("ShadowStrengthSampler")
            .withUniform("EndSeaInfo", UniformType.UNIFORM_BUFFER)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE))
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .build();

    /**
     * ShadowVolumeSize, StartY, CameraY (padded to a vec4)
     */
    private static final int INFO_UBO_SIZE = 16;

    private static final int LAYER_COUNT = 48;

    private static final Vec3[] LAYER_COLORS = new Vec3[]{
            new Vec3(0.022087, 0.098399, 0.110818),
            new Vec3(0.011892, 0.095924, 0.089485),
            new Vec3(0.027636, 0.101689, 0.100326),
            new Vec3(0.046564, 0.109883, 0.114838),
            new Vec3(0.064901, 0.117696, 0.097189),
            new Vec3(0.063761, 0.086895, 0.123646),
            new Vec3(0.084817, 0.111994, 0.166380),
            new Vec3(0.097489, 0.154120, 0.091064),
            new Vec3(0.106152, 0.131144, 0.195191),
            new Vec3(0.097721, 0.110188, 0.187229),
            new Vec3(0.133516, 0.138278, 0.148582),
            new Vec3(0.070006, 0.243332, 0.235792),
            new Vec3(0.196766, 0.142899, 0.214696),
            new Vec3(0.047281, 0.315338, 0.321970),
            new Vec3(0.204675, 0.390010, 0.302066),
            new Vec3(0.080955, 0.314821, 0.661491)
    };

    private static @Nullable MappableRingBuffer infoBuffer;

    public static void tick() {
        EndSeaShadowRenderer.tick();
    }

    /**
     * Adds the end sea frame pass. Called by the level renderer before it adds its late debug pass.
     */
    public static void addPass(final FrameGraphBuilder frameGraphBuilder, final LevelTargetBundle targets, final Camera camera) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || EndSeaPhysicsData.of(minecraft.level) == null) {
            return;
        }

        final FramePass framePass = frameGraphBuilder.addPass("simulated_end_sea");
        targets.main = framePass.readsAndWrites(targets.main);
        final ResourceHandle<RenderTarget> mainTarget = targets.main;

        framePass.executes(() -> {
            EndSeaShadowRenderer.renderShadowMap(camera);
            render(mainTarget.get(), camera);
        });
    }

    public static void render(final RenderTarget target, final Camera camera) {
        final Minecraft minecraft = Minecraft.getInstance();
        final EndSeaPhysics physics = EndSeaPhysicsData.of(minecraft.level);
        if (physics == null) {
            return;
        }

        if (EndSeaShadowRenderer.renderingShadowMap()) {
            return;
        }

        final RenderTarget shadowBuffer = EndSeaShadowRenderer.getShadowsFramebuffer();
        if (shadowBuffer == null) {
            return;
        }

        renderLayers(physics, camera, target, shadowBuffer);
    }

    private static void renderLayers(final EndSeaPhysics physics, final Camera camera, final RenderTarget target, final RenderTarget shadowBuffer) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        for (int i = 0; i < LAYER_COLORS.length; i++) {
            LAYER_COLORS[i] = LAYER_COLORS[i].lerp(LAYER_COLORS[i].normalize(), 1.0);
        }

        final Vector3dc renderOrigin = EndSeaShadowRenderer.getLastRenderOrigin();
        final Vec3 cameraPosition = camera.position();

        final PoseStack poseStack = new PoseStack();
        poseStack.translate(renderOrigin.x() - cameraPosition.x, 0.0, renderOrigin.z() - cameraPosition.z);
        poseStack.scale(EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS, 1.0f, EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS);
        poseStack.translate(0.0, physics.startY() - cameraPosition.y, 0.0);

        final float renderTime = player.tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (int i = 0; i < LAYER_COUNT; i++) {
            final Vec3 layer = LAYER_COLORS[i % LAYER_COLORS.length];
            final float yCoord = -i / 2f;
            final float uvScale = EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * 2.0f / 25.0f;
            final float uvShift = (float) Mth.frac(renderTime / 2000.0) + (float) (renderOrigin.z() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f);
            final float parallelUVShift = (float) ((float) (layer.x + layer.y) + (renderOrigin.x() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f));
            final float alpha = 1.0f;

            final Matrix4f pose = poseStack.last().pose();

            builder.addVertex(pose, -1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, 0.0f + uvShift)
                    .setUv2(0, 0);

            builder.addVertex(pose, 1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, 0.0f + uvShift)
                    .setUv2(1, 0);

            builder.addVertex(pose, 1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, uvScale + uvShift)
                    .setUv2(1, 1);

            builder.addVertex(pose, -1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, uvScale + uvShift)
                    .setUv2(0, 1);
        }

        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        if (infoBuffer == null) {
            infoBuffer = new MappableRingBuffer(() -> "Simulated end sea info", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, INFO_UBO_SIZE);
        }

        try (final GpuBuffer.MappedView view = encoder.mapBuffer(infoBuffer.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                    .putFloat(EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS)
                    .putFloat((float) physics.startY())
                    .putFloat((float) cameraPosition.y);
        }

        final AbstractTexture skyTexture = minecraft.getTextureManager().getTexture(SKY_TEXTURE);
        final GpuSampler shadowSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        final GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector3f(), new Matrix4f());

        try (final MeshData mesh = builder.buildOrThrow()) {
            final GpuBuffer vertexBuffer = PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
            final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            final int indexCount = mesh.drawState().indexCount();

            try (final RenderPass renderPass = encoder.createRenderPass(() -> "Simulated end sea", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                renderPass.setPipeline(PIPELINE);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", transforms);
                renderPass.setUniform("EndSeaInfo", infoBuffer.currentBuffer());
                renderPass.bindTexture("SkySampler", skyTexture.getTextureView(), skyTexture.getSampler());
                renderPass.bindTexture("ShadowDepthSampler", shadowBuffer.getDepthTextureView(), shadowSampler);
                renderPass.bindTexture("ShadowStrengthSampler", shadowBuffer.getColorTextureView(), shadowSampler);
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
                renderPass.drawIndexed(0, 0, indexCount, 1);
            }
        }

        infoBuffer.rotate();
    }
}
