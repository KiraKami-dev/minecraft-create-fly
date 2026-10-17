package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Renders sub-levels with vanilla section meshes.
 * <p>
 * Sections are compiled through the vanilla {@link SectionRenderDispatcher} and drawn with the
 * {@link SubLevelRenderPipelines}, one draw per section and layer, with the vanilla {@code ChunkSection} uniform block
 * per section and a {@value SubLevelRenderPipelines#SUB_LEVEL_UNIFORM} uniform block per sub-level.
 */
public class VanillaSubLevelRenderDispatcher implements SubLevelRenderDispatcher {

    private static final List<String> DYNAMIC_UNIFORMS = List.of("ChunkSection", SubLevelRenderPipelines.SUB_LEVEL_UNIFORM);
    private static final Direction[] FACE_SHADE_ORDER = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * Sub-levels outside the culling frustum this frame
     */
    private final Set<ClientSubLevel> culledSubLevels = new ReferenceOpenHashSet<>();

    /**
     * The draws collected by {@link #prepareSections}, per layer
     */
    private final EnumMap<ChunkSectionLayer, List<RenderPass.Draw<Void>>> draws = new EnumMap<>(ChunkSectionLayer.class);

    /**
     * The block entities extracted by {@link #extractBlockEntities}
     */
    private final List<ExtractedBlockEntity> blockEntities = new ObjectArrayList<>();

    private @Nullable DynamicUniformStorage<SubLevelRenderPipelines.SubLevelUniform> uniformStorage;
    private @Nullable GpuTextureView atlasTexture;
    private int maxIndicesRequired;
    private boolean hasDraws;

    public VanillaSubLevelRenderDispatcher() {
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            this.draws.put(layer, new ArrayList<>());
        }
    }

    /**
     * Checks if this sub-level is a single block, and therefore can use simpler batched rendering
     */
    public static boolean isSingleBlock(final ClientSubLevel subLevel) {
        final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        final boolean isSingle = bounds != null && bounds.minX() == bounds.maxX() && bounds.minY() == bounds.maxY() && bounds.minZ() == bounds.maxZ();
        if (!isSingle) {
            return false;
        }

        final BlockState blockState = subLevel.getLevel().getBlockState(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()));
        return !blockState.is(SableTags.ALWAYS_CHUNK_RENDERING);
    }

    /**
     * Computes the transform from anchor-relative plot positions to camera-relative world positions.
     *
     * @param pose   The render pose of the sub-level
     * @param anchor The anchor block, in plot space
     * @param camX   The x position of the camera
     * @param camY   The y position of the camera
     * @param camZ   The z position of the camera
     * @param store  The matrix to store the result in
     * @return The stored matrix
     */
    public static Matrix4f getAnchoredTransformation(final Pose3dc pose, final BlockPos anchor, final double camX, final double camY, final double camZ, final Matrix4f store) {
        final Vector3dc pos = pose.position();
        final Vector3dc scale = pose.scale();
        final Vector3dc rotationPoint = pose.rotationPoint();

        return store.translation((float) (pos.x() - camX), (float) (pos.y() - camY), (float) (pos.z() - camZ))
                .rotate(new Quaternionf(pose.orientation()))
                .scale((float) scale.x(), (float) scale.y(), (float) scale.z())
                .translate((float) (anchor.getX() - rotationPoint.x()), (float) (anchor.getY() - rotationPoint.y()), (float) (anchor.getZ() - rotationPoint.z()));
    }

    @Override
    public void onResourceManagerReload(@NotNull final ResourceManager resourceManager) {
    }

    @Override
    public SubLevelRenderData resize(final ClientSubLevel subLevel, final SubLevelRenderData renderData) {
        if (renderData instanceof VanillaSingleSubLevelRenderData ^ isSingleBlock(subLevel)) {
            renderData.close();

            // Force-rebuild the data
            final SubLevelRenderData data = this.createRenderData(subLevel);
            if (data instanceof VanillaChunkedSubLevelRenderData) {
                data.compileSections(PrioritizeChunkUpdates.NEARBY, new RenderRegionCache(), Minecraft.getInstance().gameRenderer.getMainCamera());
            }

            return data;
        }

        if (renderData instanceof final VanillaChunkedSubLevelRenderData chunkedRenderData) {
            chunkedRenderData.resize();
            chunkedRenderData.compileSections(PrioritizeChunkUpdates.NEARBY, new RenderRegionCache(), Minecraft.getInstance().gameRenderer.getMainCamera());
        }
        return renderData;
    }

    @Override
    public SubLevelRenderData createRenderData(final ClientSubLevel subLevel) {
        if (isSingleBlock(subLevel)) {
            return new VanillaSingleSubLevelRenderData(subLevel);
        }

        final SectionRenderDispatcher sectionRenderDispatcher = Minecraft.getInstance().levelRenderer.getSectionRenderDispatcher();
        return new VanillaChunkedSubLevelRenderData(subLevel, sectionRenderDispatcher);
    }

    @Override
    public void compileSections(final Iterable<ClientSubLevel> sublevels, final RenderRegionCache renderRegionCache, final Camera camera) {
        final Minecraft minecraft = Minecraft.getInstance();
        final SectionRenderDispatcher sectionRenderDispatcher = minecraft.levelRenderer.getSectionRenderDispatcher();
        if (sectionRenderDispatcher == null) {
            return;
        }

        final PrioritizeChunkUpdates chunkUpdates = minecraft.options.prioritizeChunkUpdates().get();
        final Vec3 cameraPosition = camera.position();
        final ProfilerFiller profiler = Profiler.get();
        profiler.push("sable_sub_level_sections");

        for (final ClientSubLevel sublevel : sublevels) {
            final SubLevelRenderData data = sublevel.getRenderData();
            if (data == null) {
                continue;
            }

            if (data instanceof final VanillaChunkedSubLevelRenderData chunked) {
                chunked.updateSortCamera(cameraPosition);
            }

            data.compileSections(chunkUpdates, renderRegionCache, camera);
        }

        // Sections compiled synchronously above are uploaded through the same queue as vanilla sections
        sectionRenderDispatcher.uploadAllPendingUploads();

        profiler.popPush("sable_sub_level_translucent_resort");
        for (final ClientSubLevel sublevel : sublevels) {
            if (sublevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData chunked && !this.culledSubLevels.contains(sublevel)) {
                chunked.scheduleTranslucencyResort(sectionRenderDispatcher);
            }
        }
        profiler.pop();
    }

    @Override
    public void updateCulling(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final Frustum frustum, final boolean isSpectator) {
        this.culledSubLevels.clear();

        for (final ClientSubLevel sublevel : sublevels) {
            final BoundingBox3dc bounds = sublevel.boundingBox();
            if (bounds == null) {
                continue;
            }

            // The swept bounds can lag behind the interpolated render pose by a little, so be generous
            final AABB box = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()).inflate(2.0);
            if (!frustum.isVisible(box)) {
                this.culledSubLevels.add(sublevel);
            }
        }
    }

    /**
     * @return If the sub-level was culled this frame
     */
    public boolean isCulled(final ClientSubLevel subLevel) {
        return this.culledSubLevels.contains(subLevel);
    }

    @Override
    public void extractBlockEntities(final Iterable<ClientSubLevel> sublevels, final Camera camera, final float partialTick, final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress) {
        this.blockEntities.clear();

        final Minecraft minecraft = Minecraft.getInstance();
        final BlockEntityRenderDispatcher blockEntityRenderDispatcher = minecraft.getBlockEntityRenderDispatcher();
        final BlockEntityRenderDispatcherExtension extension = (BlockEntityRenderDispatcherExtension) blockEntityRenderDispatcher;
        final Vec3 cameraPosition = camera.position();
        final Matrix4f transformation = new Matrix4f();
        final PoseStack poseStack = new PoseStack();
        final List<BlockEntity> renderable = new ObjectArrayList<>();

        try {
            for (final ClientSubLevel sublevel : sublevels) {
                if (this.culledSubLevels.contains(sublevel)) {
                    continue;
                }

                final SubLevelRenderData data = sublevel.getRenderData();
                renderable.clear();

                if (data instanceof final VanillaChunkedSubLevelRenderData chunkedRenderData) {
                    for (final SectionRenderDispatcher.RenderSection renderSection : chunkedRenderData.allRenderSections()) {
                        renderable.addAll(renderSection.getSectionMesh().getRenderableBlockEntities());
                    }
                } else if (data instanceof final VanillaSingleSubLevelRenderData singleRenderData) {
                    final BlockEntity renderBlockEntity = singleRenderData.getRenderBlockEntity();
                    if (renderBlockEntity != null) {
                        renderable.add(renderBlockEntity);
                    }
                }

                if (renderable.isEmpty()) {
                    continue;
                }

                this.extractSubLevelBlockEntities(sublevel, renderable, extension, blockEntityRenderDispatcher, cameraPosition, partialTick, destructionProgress, transformation, poseStack);
            }

            // Globally rendered block entities (beacons, ...) are tracked by the level, not the sections
            final ClientLevel level = minecraft.level;
            if (level != null) {
                for (final BlockEntity blockEntity : level.getGloballyRenderedBlockEntities()) {
                    if (blockEntity.isRemoved()) {
                        continue;
                    }

                    final ClientSubLevel sublevel = Sable.HELPER.getContainingClient(blockEntity);
                    if (sublevel == null || this.culledSubLevels.contains(sublevel)) {
                        continue;
                    }

                    renderable.clear();
                    renderable.add(blockEntity);
                    this.extractSubLevelBlockEntities(sublevel, renderable, extension, blockEntityRenderDispatcher, cameraPosition, partialTick, destructionProgress, transformation, poseStack);
                }
            }
        } finally {
            extension.sable$setCameraPosition(null);
        }
    }

    private void extractSubLevelBlockEntities(final ClientSubLevel sublevel,
                                              final List<BlockEntity> renderable,
                                              final BlockEntityRenderDispatcherExtension extension,
                                              final BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                                              final Vec3 cameraPosition,
                                              final float partialTick,
                                              final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress,
                                              final Matrix4f transformation,
                                              final PoseStack poseStack) {
        final Pose3dc renderPose = sublevel.renderPose();
        final Vector3dc rotationPoint = renderPose.rotationPoint();

        // Block entity renderers check distances against the camera, which has to be in plot space for them
        extension.sable$setCameraPosition(renderPose.transformPositionInverse(cameraPosition));
        sublevel.getRenderData().getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z, transformation);

        for (final BlockEntity blockEntity : renderable) {
            final BlockPos blockPos = blockEntity.getBlockPos();
            final Matrix4f pose = new Matrix4f(transformation).translate((float) (blockPos.getX() - rotationPoint.x()), (float) (blockPos.getY() - rotationPoint.y()), (float) (blockPos.getZ() - rotationPoint.z()));

            final SortedSet<BlockDestructionProgress> progress = destructionProgress.get(blockPos.asLong());
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay = null;
            if (progress != null && !progress.isEmpty()) {
                poseStack.pushPose();
                poseStack.mulPose(pose);
                crumblingOverlay = new ModelFeatureRenderer.CrumblingOverlay(progress.last().getProgress(), poseStack.last().copy());
                poseStack.popPose();
            }

            final BlockEntityRenderState state = blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, partialTick, crumblingOverlay);
            if (state != null) {
                state.lightCoords = sublevel.scaleLightColor(state.lightCoords);
                this.blockEntities.add(new ExtractedBlockEntity(state, pose));
            }
        }
    }

    @Override
    public void prepareSections(final Iterable<ClientSubLevel> sublevels, final Matrix4fc modelView, final double cameraX, final double cameraY, final double cameraZ) {
        for (final List<RenderPass.Draw<Void>> list : this.draws.values()) {
            list.clear();
        }
        this.hasDraws = false;
        this.maxIndicesRequired = 0;

        if (this.uniformStorage == null) {
            this.uniformStorage = SubLevelRenderPipelines.createUniformStorage();
        } else {
            this.uniformStorage.endFrame();
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        final ProfilerFiller profiler = Profiler.get();
        profiler.push("sable_prepare_sub_level_sections");

        this.atlasTexture = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        final int atlasWidth = this.atlasTexture.getWidth(0);
        final int atlasHeight = this.atlasTexture.getHeight(0);

        // Sub-levels owning the water occlusion volume the camera is in have their fog disabled
        final Vec3 cameraPosition = new Vec3(cameraX, cameraY, cameraZ);
        ClientSubLevel fogFreeSubLevel = null;
        final WaterOcclusionContainer<?> waterOcclusion = WaterOcclusionContainer.getContainer(level);
        if (waterOcclusion != null) {
            final var occludingRegion = waterOcclusion.getOccludingRegion(cameraPosition);
            if (occludingRegion != null) {
                fogFreeSubLevel = Sable.HELPER.getContainingClient(occludingRegion.getVolume().getMinBlockPos());
            }
        }

        final Vector4f faceShadeA = new Vector4f(level.getShade(FACE_SHADE_ORDER[0], true), level.getShade(FACE_SHADE_ORDER[1], true), level.getShade(FACE_SHADE_ORDER[2], true), level.getShade(FACE_SHADE_ORDER[3], true));
        final Vector4f faceShadeB = new Vector4f(level.getShade(FACE_SHADE_ORDER[4], true), level.getShade(FACE_SHADE_ORDER[5], true), 0.0f, 0.0f);
        final float dynamicShading = SableDynamicDirectionalShading.isEnabled() ? 1.0f : 0.0f;

        final List<PendingDraw> pendingDraws = new ObjectArrayList<>();
        final List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ObjectArrayList<>();
        final Matrix4f transformation = new Matrix4f();
        final Vector4f params = new Vector4f();
        final Vector3d sectionCenter = new Vector3d();
        final long now = Util.getMillis();

        for (final ClientSubLevel sublevel : sublevels) {
            if (!(sublevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData chunked) || this.culledSubLevels.contains(sublevel)) {
                continue;
            }

            final Pose3dc renderPose = sublevel.renderPose();
            final Vector3dc rotationPoint = renderPose.rotationPoint();
            final BlockPos anchor = BlockPos.containing(rotationPoint.x(), rotationPoint.y(), rotationPoint.z());

            GpuBufferSlice subLevelUniform = null;

            for (final SectionRenderDispatcher.RenderSection renderSection : chunked.allRenderSections()) {
                final SectionMesh mesh = renderSection.getSectionMesh();
                final BlockPos origin = renderSection.getRenderOrigin();
                int sectionIndex = -1;

                for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                    final SectionBuffers buffers = mesh.getBuffers(layer);
                    if (buffers == null) {
                        continue;
                    }

                    if (subLevelUniform == null) {
                        getAnchoredTransformation(renderPose, anchor, cameraX, cameraY, cameraZ, transformation);
                        params.set(dynamicShading, sublevel.getLatestSkyLightScale() / 15.0f, sublevel == fogFreeSubLevel ? 0.0f : 1.0f, 0.0f);
                        subLevelUniform = this.uniformStorage.writeUniform(new SubLevelRenderPipelines.SubLevelUniform(transformation, faceShadeA, faceShadeB, params));
                    }

                    if (sectionIndex == -1) {
                        sectionIndex = sectionInfos.size();
                        sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(modelView),
                                origin.getX() - anchor.getX(), origin.getY() - anchor.getY(), origin.getZ() - anchor.getZ(),
                                renderSection.getVisibility(now), atlasWidth, atlasHeight));
                    }

                    if (buffers.getIndexBuffer() == null && buffers.getIndexCount() > this.maxIndicesRequired) {
                        this.maxIndicesRequired = buffers.getIndexCount();
                    }

                    double distance = 0.0;
                    if (layer == ChunkSectionLayer.TRANSLUCENT) {
                        renderPose.transformPosition(sectionCenter.set(origin.getX() + 8.0, origin.getY() + 8.0, origin.getZ() + 8.0));
                        distance = sectionCenter.distanceSquared(cameraX, cameraY, cameraZ);
                    }

                    pendingDraws.add(new PendingDraw(layer, buffers, sectionIndex, subLevelUniform, distance));
                }
            }
        }

        if (!pendingDraws.isEmpty()) {
            final GpuBufferSlice[] sectionUniforms = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));

            // Translucent geometry is drawn back to front
            pendingDraws.sort(Comparator.comparingDouble(PendingDraw::distance).reversed());

            for (final PendingDraw pending : pendingDraws) {
                final SectionBuffers buffers = pending.buffers();
                final GpuBufferSlice sectionUniform = sectionUniforms[pending.sectionIndex()];
                final GpuBufferSlice subLevelUniform = pending.subLevelUniform();
                final BiConsumer<Void, RenderPass.UniformUploader> uploader = (unused, uniformUploader) -> {
                    uniformUploader.upload("ChunkSection", sectionUniform);
                    uniformUploader.upload(SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, subLevelUniform);
                };

                // Sections without their own index buffer use the shared sequential quad index buffer and its index type
                final GpuBuffer indexBuffer = buffers.getIndexBuffer();
                this.draws.get(pending.layer()).add(new RenderPass.Draw<>(
                        0,
                        buffers.getVertexBuffer(),
                        indexBuffer,
                        indexBuffer == null ? null : buffers.getIndexType(),
                        0,
                        buffers.getIndexCount(),
                        uploader
                ));
            }

            this.hasDraws = true;
        }

        profiler.pop();
    }

    /**
     * Renders the depth of every opaque and cutout sub-level section into a shadow map target.
     *
     * @param sublevels  The sub-levels to render
     * @param target     The target to render into, which must be cleared by this call
     * @param view       The model-view matrix of the shadow camera, relative to {@code origin}
     * @param projection The projection uniform of the shadow camera
     * @param origin     The world position of the shadow camera
     * @return If anything was rendered
     */
    public boolean renderShadowMap(final Iterable<ClientSubLevel> sublevels, final RenderTarget target, final Matrix4fc view, final GpuBufferSlice projection, final Vector3dc origin) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (this.uniformStorage == null) {
            return false;
        }

        final GpuTextureView atlas = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        final List<PendingDraw> pendingDraws = new ObjectArrayList<>();
        final List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ObjectArrayList<>();
        final Matrix4f transformation = new Matrix4f();
        final Vector4f params = new Vector4f();
        final Vector4f faceShade = new Vector4f(1.0f);
        int maxIndices = 0;

        for (final ClientSubLevel sublevel : sublevels) {
            if (!(sublevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData chunked)) {
                continue;
            }

            final Pose3dc renderPose = sublevel.renderPose();
            final Vector3dc rotationPoint = renderPose.rotationPoint();
            final BlockPos anchor = BlockPos.containing(rotationPoint.x(), rotationPoint.y(), rotationPoint.z());

            GpuBufferSlice subLevelUniform = null;

            for (final SectionRenderDispatcher.RenderSection renderSection : chunked.allRenderSections()) {
                final SectionMesh mesh = renderSection.getSectionMesh();
                final BlockPos sectionOrigin = renderSection.getRenderOrigin();
                int sectionIndex = -1;

                for (final ChunkSectionLayer layer : new ChunkSectionLayer[]{ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT}) {
                    final SectionBuffers buffers = mesh.getBuffers(layer);
                    if (buffers == null) {
                        continue;
                    }

                    if (subLevelUniform == null) {
                        getAnchoredTransformation(renderPose, anchor, origin.x(), origin.y(), origin.z(), transformation);
                        params.set(0.0f, 1.0f, 0.0f, 0.0f);
                        subLevelUniform = this.uniformStorage.writeUniform(new SubLevelRenderPipelines.SubLevelUniform(transformation, faceShade, faceShade, params));
                    }

                    if (sectionIndex == -1) {
                        sectionIndex = sectionInfos.size();
                        sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(view),
                                sectionOrigin.getX() - anchor.getX(), sectionOrigin.getY() - anchor.getY(), sectionOrigin.getZ() - anchor.getZ(),
                                1.0f, atlas.getWidth(0), atlas.getHeight(0)));
                    }

                    if (buffers.getIndexBuffer() == null && buffers.getIndexCount() > maxIndices) {
                        maxIndices = buffers.getIndexCount();
                    }

                    pendingDraws.add(new PendingDraw(layer, buffers, sectionIndex, subLevelUniform, 0.0));
                }
            }
        }

        if (pendingDraws.isEmpty()) {
            return false;
        }

        final GpuBufferSlice[] sectionUniforms = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        final EnumMap<ChunkSectionLayer, List<RenderPass.Draw<Void>>> shadowDraws = new EnumMap<>(ChunkSectionLayer.class);

        for (final PendingDraw pending : pendingDraws) {
            final SectionBuffers buffers = pending.buffers();
            final GpuBufferSlice sectionUniform = sectionUniforms[pending.sectionIndex()];
            final GpuBufferSlice subLevelUniform = pending.subLevelUniform();
            final BiConsumer<Void, RenderPass.UniformUploader> uploader = (unused, uniformUploader) -> {
                uniformUploader.upload("ChunkSection", sectionUniform);
                uniformUploader.upload(SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, subLevelUniform);
            };

            final GpuBuffer indexBuffer = buffers.getIndexBuffer();
            shadowDraws.computeIfAbsent(pending.layer(), k -> new ArrayList<>()).add(new RenderPass.Draw<>(
                    0, buffers.getVertexBuffer(), indexBuffer, indexBuffer == null ? null : buffers.getIndexType(), 0, buffers.getIndexCount(), uploader));
        }

        final RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        final GpuBuffer indexBuffer = maxIndices == 0 ? null : sequentialBuffer.getBuffer(maxIndices);
        final VertexFormat.IndexType indexType = maxIndices == 0 ? null : sequentialBuffer.type();

        try (final RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Sable sky light shadow map", target.getColorTextureView(), OptionalInt.of(0), target.getDepthTextureView(), OptionalDouble.of(1.0))) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("Projection", projection);
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.bindTexture("Sampler0", atlas, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

            for (final var entry : shadowDraws.entrySet()) {
                renderPass.setPipeline(entry.getKey() == ChunkSectionLayer.SOLID ? SubLevelRenderPipelines.SHADOW_SOLID : SubLevelRenderPipelines.SHADOW_CUTOUT);
                renderPass.drawMultipleIndexed(entry.getValue(), indexBuffer, indexType, DYNAMIC_UNIFORMS, null);
            }
        }

        return true;
    }

    @Override
    public void renderSectionLayerGroup(final ChunkSectionLayerGroup group, final GpuSampler sampler) {
        if (!this.hasDraws || this.atlasTexture == null) {
            return;
        }

        boolean anyDraws = false;
        for (final ChunkSectionLayer layer : group.layers()) {
            anyDraws |= !this.draws.get(layer).isEmpty();
        }

        if (anyDraws) {
            final Minecraft minecraft = Minecraft.getInstance();
            final RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            final GpuBuffer indexBuffer = this.maxIndicesRequired == 0 ? null : sequentialBuffer.getBuffer(this.maxIndicesRequired);
            final VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : sequentialBuffer.type();
            final RenderTarget renderTarget = group.outputTarget();

            try (final RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "Sable sub-level section layers for " + group.label(),
                            renderTarget.getColorTextureView(),
                            OptionalInt.empty(),
                            renderTarget.getDepthTextureView(),
                            OptionalDouble.empty()
                    )) {
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

                for (final ChunkSectionLayer layer : group.layers()) {
                    final List<RenderPass.Draw<Void>> list = this.draws.get(layer);
                    if (list.isEmpty()) {
                        continue;
                    }

                    renderPass.setPipeline(SubLevelRenderPipelines.get(layer));
                    renderPass.bindTexture("Sampler0", this.atlasTexture, sampler);
                    renderPass.drawMultipleIndexed(list, indexBuffer, indexType, DYNAMIC_UNIFORMS, null);
                }
            }
        }

        // The draws reference section buffers, which may be freed before the next frame
        if (group == ChunkSectionLayerGroup.TRIPWIRE) {
            for (final List<RenderPass.Draw<Void>> list : this.draws.values()) {
                list.clear();
            }
            this.hasDraws = false;
        }
    }

    @Override
    public void submit(final Iterable<ClientSubLevel> sublevels, final PoseStack poseStack, final SubmitNodeCollector submitCollector, final CameraRenderState cameraState) {
        final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        for (final ExtractedBlockEntity blockEntity : this.blockEntities) {
            poseStack.pushPose();
            poseStack.mulPose(blockEntity.pose());
            blockEntityRenderDispatcher.submit(blockEntity.state(), poseStack, submitCollector, cameraState);
            poseStack.popPose();
        }
        this.blockEntities.clear();

        final Vec3 cameraPosition = cameraState.pos;
        for (final ClientSubLevel sublevel : sublevels) {
            if (sublevel.getRenderData() instanceof final VanillaSingleSubLevelRenderData singleRenderData && !this.culledSubLevels.contains(sublevel)) {
                singleRenderData.submitSingleBlock(poseStack, submitCollector, cameraPosition.x, cameraPosition.y, cameraPosition.z);
            }
        }
    }

    @Override
    public void addDebugInfo(final Consumer<String> consumer) {
        int drawCount = 0;
        for (final List<RenderPass.Draw<Void>> list : this.draws.values()) {
            drawCount += list.size();
        }
        consumer.accept("Sub-level renderer: vanilla, %d section draws, %d culled sub-levels".formatted(drawCount, this.culledSubLevels.size()));
    }

    @Override
    public void free() {
        for (final List<RenderPass.Draw<Void>> list : this.draws.values()) {
            list.clear();
        }
        this.hasDraws = false;
        this.blockEntities.clear();
        this.culledSubLevels.clear();

        if (this.uniformStorage != null) {
            this.uniformStorage.close();
            this.uniformStorage = null;
        }
    }

    private record PendingDraw(ChunkSectionLayer layer, SectionBuffers buffers, int sectionIndex, GpuBufferSlice subLevelUniform, double distance) {
    }

    private record ExtractedBlockEntity(BlockEntityRenderState state, Matrix4fc pose) {
    }
}
