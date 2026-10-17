package dev.eriksonn.aeronautics.content.blocks.levitite.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zurrtum.create.client.flywheel.lib.util.ShadersModHelper;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;

/**
 * Draws the levitite effect layers of sub-levels (upstream: the {@code levitite} / {@code levitite_ghosts} Veil chunk
 * layers, drawn with a tessellation shader).
 * <p>
 * There is no custom chunk layer on 1.21.11 ({@link ChunkSectionLayer} is a closed enum), so levitite blocks stay in
 * the solid layer (upstream's fallback when the shader was off) and the effect layers draw the compiled solid section
 * buffers of each sub-level again with {@link AeroRenderTypes#LEVITITE} / {@link AeroRenderTypes#LEVITITE_GHOSTS}.
 * Their vertex shader drops every quad that is not textured with a levitite sprite. Hooked into Sable's
 * {@link VanillaSubLevelRenderDispatcher} by {@code mixin/levitite/VanillaSubLevelRenderDispatcherMixin}.
 */
public final class LevititeRenderer {

    private static final List<String> DYNAMIC_UNIFORMS = List.of("ChunkSection", SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, AeroRenderTypes.LEVITITE_UNIFORM);
    private static final Direction[] FACE_SHADE_ORDER = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final Identifier NOISE_TEXTURE = Aeronautics.path("textures/special/noise_composed.png");
    private static final Identifier[] SPRITES = {
            Aeronautics.path("block/levitite"),
            Aeronautics.path("block/levitite_connected"),
            Aeronautics.path("block/pearlescent_levitite"),
            Aeronautics.path("block/pearlescent_levitite_connected")
    };

    public static final int SPRITE_COUNT = 4;
    public static final int LEVITITE_UBO_SIZE;

    static {
        final Std140SizeCalculator calculator = new Std140SizeCalculator().putMat4f().putMat4f().putMat4f()
                .putVec4().putVec4().putVec4().putVec4().putVec4();
        for (int i = 0; i < SPRITE_COUNT; i++) {
            calculator.putVec4();
        }
        LEVITITE_UBO_SIZE = calculator.get();
    }

    private static final List<RenderPass.Draw<Void>> blockDraws = new ObjectArrayList<>();
    private static final List<RenderPass.Draw<Void>> ghostDraws = new ObjectArrayList<>();

    private static @Nullable DynamicUniformStorage<SubLevelRenderPipelines.SubLevelUniform> subLevelUniforms;
    private static @Nullable DynamicUniformStorage<LevititeUniform> levititeUniforms;
    private static @Nullable GpuTextureView atlasTexture;
    private static int maxIndicesRequired;

    private LevititeRenderer() {
    }

    /**
     * Collects the draws of all visible chunked sub-levels, after Sable prepared its own section draws.
     */
    public static void prepare(final VanillaSubLevelRenderDispatcher dispatcher,
                               final Iterable<ClientSubLevel> sublevels,
                               final Matrix4fc modelView,
                               final double cameraX,
                               final double cameraY,
                               final double cameraZ) {
        blockDraws.clear();
        ghostDraws.clear();
        maxIndicesRequired = 0;

        if (subLevelUniforms == null) {
            subLevelUniforms = SubLevelRenderPipelines.createUniformStorage();
            levititeUniforms = new DynamicUniformStorage<>("Aeronautics Levitite UBO", LEVITITE_UBO_SIZE, 16);
        } else {
            subLevelUniforms.endFrame();
            levititeUniforms.endFrame();
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        // Shader packs replace the terrain pipeline; the solid fallback blocks are drawn by them
        if (level == null || ShadersModHelper.isShaderPackInUse()) {
            return;
        }

        atlasTexture = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        final int atlasWidth = atlasTexture.getWidth(0);
        final int atlasHeight = atlasTexture.getHeight(0);

        final TextureAtlas atlas = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        final Vector4fc[] spriteRects = new Vector4fc[SPRITE_COUNT];
        for (int i = 0; i < SPRITE_COUNT; i++) {
            final TextureAtlasSprite sprite = atlas.getSprite(SPRITES[i]);
            spriteRects[i] = new Vector4f(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
        }

        final Vector4f faceShadeA = new Vector4f(level.getShade(FACE_SHADE_ORDER[0], true), level.getShade(FACE_SHADE_ORDER[1], true), level.getShade(FACE_SHADE_ORDER[2], true), level.getShade(FACE_SHADE_ORDER[3], true));
        final Vector4f faceShadeB = new Vector4f(level.getShade(FACE_SHADE_ORDER[4], true), level.getShade(FACE_SHADE_ORDER[5], true), 0.0f, 0.0f);
        final float dynamicShading = SableDynamicDirectionalShading.isEnabled() ? 1.0f : 0.0f;
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        final List<PendingDraw> pendingDraws = new ObjectArrayList<>();
        final List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ObjectArrayList<>();
        final Matrix4f transformation = new Matrix4f();
        final long now = Util.getMillis();

        for (final ClientSubLevel sublevel : sublevels) {
            if (!(sublevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData chunked) || dispatcher.isCulled(sublevel)) {
                continue;
            }

            final Pose3dc renderPose = sublevel.renderPose();
            final Vector3dc rotationPoint = renderPose.rotationPoint();
            final BlockPos anchor = BlockPos.containing(rotationPoint.x(), rotationPoint.y(), rotationPoint.z());
            final LevititeShaderManager manager = LevititeShaderManager.getInstance(sublevel);
            final boolean ghosts = manager.needsLayers();

            GpuBufferSlice subLevelUniform = null;
            GpuBufferSlice blockUniform = null;
            GpuBufferSlice forwardGhostUniform = null;
            GpuBufferSlice backwardGhostUniform = null;

            for (final SectionRenderDispatcher.RenderSection renderSection : chunked.allRenderSections()) {
                final SectionBuffers buffers = renderSection.getSectionMesh().getBuffers(ChunkSectionLayer.SOLID);
                if (buffers == null) {
                    continue;
                }

                if (subLevelUniform == null) {
                    VanillaSubLevelRenderDispatcher.getAnchoredTransformation(renderPose, anchor, cameraX, cameraY, cameraZ, transformation);
                    final Vector4f params = new Vector4f(dynamicShading, sublevel.getLatestSkyLightScale() / 15.0f, 1.0f, 0.0f);
                    subLevelUniform = subLevelUniforms.writeUniform(new SubLevelRenderPipelines.SubLevelUniform(transformation, faceShadeA, faceShadeB, params));
                    blockUniform = levititeUniforms.writeUniform(manager.createUniform(sublevel, anchor, 0, spriteRects, partialTick));
                    if (ghosts) {
                        forwardGhostUniform = levititeUniforms.writeUniform(manager.createUniform(sublevel, anchor, 1, spriteRects, partialTick));
                        backwardGhostUniform = levititeUniforms.writeUniform(manager.createUniform(sublevel, anchor, -1, spriteRects, partialTick));
                    }
                }

                final BlockPos origin = renderSection.getRenderOrigin();
                final int sectionIndex = sectionInfos.size();
                sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(modelView),
                        origin.getX() - anchor.getX(), origin.getY() - anchor.getY(), origin.getZ() - anchor.getZ(),
                        renderSection.getVisibility(now), atlasWidth, atlasHeight));

                if (buffers.getIndexBuffer() == null && buffers.getIndexCount() > maxIndicesRequired) {
                    maxIndicesRequired = buffers.getIndexCount();
                }

                pendingDraws.add(new PendingDraw(buffers, sectionIndex, subLevelUniform, blockUniform, forwardGhostUniform, backwardGhostUniform));
            }
        }

        if (pendingDraws.isEmpty()) {
            return;
        }

        final GpuBufferSlice[] sectionUniforms = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        for (final PendingDraw pending : pendingDraws) {
            final GpuBufferSlice sectionUniform = sectionUniforms[pending.sectionIndex()];
            blockDraws.add(createDraw(pending.buffers(), sectionUniform, pending.subLevelUniform(), pending.blockUniform()));

            if (pending.forwardGhostUniform() != null) {
                ghostDraws.add(createDraw(pending.buffers(), sectionUniform, pending.subLevelUniform(), pending.forwardGhostUniform()));
                ghostDraws.add(createDraw(pending.buffers(), sectionUniform, pending.subLevelUniform(), pending.backwardGhostUniform()));
            }
        }
    }

    private static RenderPass.Draw<Void> createDraw(final SectionBuffers buffers, final GpuBufferSlice sectionUniform, final GpuBufferSlice subLevelUniform, final GpuBufferSlice levititeUniform) {
        final BiConsumer<Void, RenderPass.UniformUploader> uploader = (unused, uniformUploader) -> {
            uniformUploader.upload("ChunkSection", sectionUniform);
            uniformUploader.upload(SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, subLevelUniform);
            uniformUploader.upload(AeroRenderTypes.LEVITITE_UNIFORM, levititeUniform);
        };

        // Sections without their own index buffer use the shared sequential quad index buffer and its index type
        final GpuBuffer indexBuffer = buffers.getIndexBuffer();
        return new RenderPass.Draw<>(0, buffers.getVertexBuffer(), indexBuffer, indexBuffer == null ? null : buffers.getIndexType(), 0, buffers.getIndexCount(), uploader);
    }

    /**
     * Draws the block layer after the opaque section group and the ghost layers after the translucent group.
     */
    public static void render(final ChunkSectionLayerGroup group, final GpuSampler sampler) {
        try {
            if (group == ChunkSectionLayerGroup.OPAQUE && !blockDraws.isEmpty()) {
                draw(AeroRenderTypes.LEVITITE, blockDraws, group.outputTarget(), sampler, false);
            } else if (group == ChunkSectionLayerGroup.TRANSLUCENT && !ghostDraws.isEmpty()) {
                draw(AeroRenderTypes.LEVITITE_GHOSTS, ghostDraws, group.outputTarget(), sampler, true);
            }
        } finally {
            // The draws reference section buffers, which may be freed before the next frame
            if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
                blockDraws.clear();
                ghostDraws.clear();
            }
        }
    }

    private static void draw(final RenderPipeline pipeline, final List<RenderPass.Draw<Void>> draws, final RenderTarget target, final GpuSampler sampler, final boolean ghosts) {
        if (atlasTexture == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        final GpuBuffer indexBuffer = maxIndicesRequired == 0 ? null : sequentialBuffer.getBuffer(maxIndicesRequired);
        final VertexFormat.IndexType indexType = maxIndicesRequired == 0 ? null : sequentialBuffer.type();
        final AbstractTexture noise = minecraft.getTextureManager().getTexture(NOISE_TEXTURE);

        // The ghost layers sample the scene depth instead of testing against it, so their pass has no depth attachment
        try (final RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "Aeronautics levitite " + (ghosts ? "ghost layers" : "layer"),
                        target.getColorTextureView(),
                        OptionalInt.empty(),
                        ghosts ? null : target.getDepthTextureView(),
                        OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setPipeline(pipeline);
            renderPass.bindTexture("Sampler0", atlasTexture, sampler);
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.bindTexture("Noise", noise.getTextureView(), noise.getSampler());
            if (ghosts) {
                renderPass.bindTexture("DepthSampler", target.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            }
            renderPass.drawMultipleIndexed(draws, indexBuffer, indexType, DYNAMIC_UNIFORMS, null);
        }
    }

    public static void free() {
        blockDraws.clear();
        ghostDraws.clear();
        if (subLevelUniforms != null) {
            subLevelUniforms.close();
            subLevelUniforms = null;
        }
        if (levititeUniforms != null) {
            levititeUniforms.close();
            levititeUniforms = null;
        }
    }

    private record PendingDraw(SectionBuffers buffers, int sectionIndex, GpuBufferSlice subLevelUniform, GpuBufferSlice blockUniform,
                               @Nullable GpuBufferSlice forwardGhostUniform, @Nullable GpuBufferSlice backwardGhostUniform) {
    }

    /**
     * The {@code LevititeInfo} uniform block, see {@code aeronautics:include/levitite.glsl}.
     */
    public record LevititeUniform(Matrix4fc orientation, Matrix4fc materialMatrixSlow, Matrix4fc materialMatrixFast,
                                  Vector4fc anchorOffset, Vector4fc linearVelocity, Vector4fc angularVelocity,
                                  Vector4fc subLevelPosition, Vector4fc layerParams,
                                  Vector4fc[] spriteRects) implements DynamicUniformStorage.DynamicUniform {

        @Override
        public void write(final ByteBuffer buffer) {
            final Std140Builder builder = Std140Builder.intoBuffer(buffer)
                    .putMat4f(this.orientation)
                    .putMat4f(this.materialMatrixSlow)
                    .putMat4f(this.materialMatrixFast)
                    .putVec4(this.anchorOffset)
                    .putVec4(this.linearVelocity)
                    .putVec4(this.angularVelocity)
                    .putVec4(this.subLevelPosition)
                    .putVec4(this.layerParams);
            for (int i = 0; i < SPRITE_COUNT; i++) {
                builder.putVec4(this.spriteRects[i]);
            }
        }
    }
}
