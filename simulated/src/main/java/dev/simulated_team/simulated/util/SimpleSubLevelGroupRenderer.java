package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import dev.simulated_team.simulated.mixin_interface.diagram.LightTextureExtension;
import dev.simulated_team.simulated.mixin_interface.diagram.VisualizationManagerExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Renders a group of sub-levels into an offscreen target, from an arbitrary camera. Used by contraption diagrams and the
 * end sea shadow map.
 * <p>
 * 1.21.11 port: upstream bound a Veil framebuffer and drew through the sub-level render dispatcher. Here the sub-level
 * sections are drawn with Sable's {@link SubLevelRenderPipelines} into the target directly, with uniforms written for
 * this camera (no dynamic directional shading, no fog, no end sea fade, like upstream's "simple" mode), and block
 * entities, single-block sub-levels and entities go through the feature renderer with the output texture overridden.
 */
public class SimpleSubLevelGroupRenderer {
    private static final List<String> DYNAMIC_UNIFORMS = List.of("ChunkSection", SubLevelRenderPipelines.SUB_LEVEL_UNIFORM);
    private static final Direction[] FACE_SHADE_ORDER = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * If sub-level terrain is currently being drawn by this renderer
     */
    public static boolean RENDERING_SIMPLE = false;

    private static @Nullable DynamicUniformStorage<SubLevelRenderPipelines.SubLevelUniform> uniformStorage;
    private static @Nullable PerspectiveProjectionMatrixBuffer projectionBuffer;

    /**
     * @return the chain of sub-levels that should render with a given sub-level into a diagram
     */
    public static Collection<ClientSubLevel> getRenderedChain(final ClientSubLevel subLevel) {
        final ObjectOpenHashSet<ClientSubLevel> visited = new ObjectOpenHashSet<>();
        final ObjectOpenHashSet<ClientSubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final ClientSubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()));

            // Intersecting dependencies
            for (final SubLevel neighbor : intersecting) {
                final ClientSubLevel serverNeighbor = (ClientSubLevel) neighbor;

                if (!visited.contains(serverNeighbor)) {
                    frontier.add(serverNeighbor);
                }
            }
        }

        return visited;
    }

    public static void renderChain(final SubLevel subLevel, final RenderTarget target, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks) {
        final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;
        final ClientLevel level = clientSubLevel.getLevel();
        final Collection<ClientSubLevel> subLevels = SimpleSubLevelGroupRenderer.getRenderedChain(clientSubLevel);

        renderGroup(level, subLevels, target, modelView, projectionMat, cameraPosition, orientation, partialTicks, true);
    }

    /**
     * Renders the given sub-levels into the color and depth textures of the target. The target is not cleared.
     *
     * @param modelView      additional view transform, applied before the camera orientation
     * @param projectionMat  the projection to draw with
     * @param cameraPosition the world position of the camera
     * @param orientation    the view rotation (the inverse of the camera orientation)
     */
    public static void renderGroup(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final RenderTarget target, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3dc cameraPosition, final Quaternionf orientation, final float partialTicks, final boolean renderPlayers) {
        // Finish anything previously being rendered for safety
        final Minecraft minecraft = Minecraft.getInstance();
        final MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        bufferSource.endBatch();

        if (subLevels.isEmpty()) {
            return;
        }

        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final LightTexture lightTexture = gameRenderer.lightTexture();
        final Lighting lighting = gameRenderer.getLighting();
        final FeatureRenderDispatcher featureRenderDispatcher = gameRenderer.getFeatureRenderDispatcher();
        final SubmitNodeCollector collector = featureRenderDispatcher.getSubmitNodeStorage();

        final Matrix4f viewMatrix = new Matrix4f(modelView).rotate(orientation);
        final Vec3 cameraPos = new Vec3(cameraPosition.x(), cameraPosition.y(), cameraPosition.z());

        final CameraRenderState cameraState = new CameraRenderState();
        cameraState.pos = cameraPos;
        cameraState.entityPos = cameraPos;
        cameraState.blockPos = BlockPos.containing(cameraPos);
        cameraState.orientation = orientation.conjugate(new Quaternionf());
        cameraState.initialized = true;

        // State the level / gui renderer relies on
        final GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        final ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        final GpuBufferSlice previousLights = RenderSystem.getShaderLights();
        final GpuTextureView previousColorOverride = RenderSystem.outputColorTextureOverride;
        final GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;

        RenderSystem.setProjectionMatrix(getProjectionBuffer().getBuffer(projectionMat), ProjectionType.ORTHOGRAPHIC);

        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        modelViewStack.mul(viewMatrix);

        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();

        final VisualizationManager visualizationManager = VisualizationManager.get(level);

        try {
            // Lighting.setupNetherLevel()
            lighting.updateLevel(DimensionType.CardinalLightType.NETHER);
            lighting.setupFor(Lighting.Entry.LEVEL);
            ((LightTextureExtension) lightTexture).simulated$makeDiagramLightTexture(0.65f);

            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = true;
            renderSections(level, subLevels, target, viewMatrix, cameraPosition, lightTexture);

            // single block sub-levels
            final PoseStack singleBlockPoseStack = new PoseStack();
            for (final ClientSubLevel sublevel : subLevels) {
                if (sublevel.getRenderData() instanceof final VanillaSingleSubLevelRenderData singleRenderData) {
                    singleRenderData.submitSingleBlock(singleBlockPoseStack, collector, cameraPosition.x(), cameraPosition.y(), cameraPosition.z());
                }
            }
            featureRenderDispatcher.renderAllFeatures();
            bufferSource.endBatch();

            ((LightTextureExtension) lightTexture).simulated$makeDiagramLightTexture(1.0f);
            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = false;

            // Render block-entities with visuals normally
            if (visualizationManager instanceof final VisualizationManagerExtension extension) {
                extension.sable$setDrawingDiagram(true);
            }

            submitBlockEntities(subLevels, collector, cameraState, cameraPos, partialTicks);
            submitEntities(level, subLevels, collector, cameraState, cameraPos, renderPlayers);

            featureRenderDispatcher.renderAllFeatures();
            bufferSource.endBatch();
        } finally {
            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = false;

            if (visualizationManager instanceof final VisualizationManagerExtension extension) {
                extension.sable$setDrawingDiagram(false);
            }

            lighting.updateLevel(level.dimensionType().cardinalLightType());
            RenderSystem.setShaderLights(previousLights);

            RenderSystem.outputColorTextureOverride = previousColorOverride;
            RenderSystem.outputDepthTextureOverride = previousDepthOverride;

            modelViewStack.popMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);

            lightTexture.updateLightTexture(partialTicks);
        }
    }

    /**
     * Draws the compiled sections of all chunked sub-levels into the target
     */
    private static void renderSections(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final RenderTarget target, final Matrix4fc viewMatrix, final Vector3dc cameraPosition, final LightTexture lightTexture) {
        final Minecraft minecraft = Minecraft.getInstance();
        final DynamicUniformStorage<SubLevelRenderPipelines.SubLevelUniform> storage = getUniformStorage();

        final GpuTextureView atlasTexture = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        final int atlasWidth = atlasTexture.getWidth(0);
        final int atlasHeight = atlasTexture.getHeight(0);

        final Vector4f faceShadeA = new Vector4f(level.getShade(FACE_SHADE_ORDER[0], true), level.getShade(FACE_SHADE_ORDER[1], true), level.getShade(FACE_SHADE_ORDER[2], true), level.getShade(FACE_SHADE_ORDER[3], true));
        final Vector4f faceShadeB = new Vector4f(level.getShade(FACE_SHADE_ORDER[4], true), level.getShade(FACE_SHADE_ORDER[5], true), 0.0f, 0.0f);

        final List<PendingDraw> pendingDraws = new ObjectArrayList<>();
        final List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ObjectArrayList<>();
        final Matrix4f transformation = new Matrix4f();
        final Vector4f params = new Vector4f();
        final Vector3d sectionCenter = new Vector3d();
        int maxIndicesRequired = 0;

        for (final ClientSubLevel sublevel : subLevels) {
            if (!(sublevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData chunked)) {
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
                        VanillaSubLevelRenderDispatcher.getAnchoredTransformation(renderPose, anchor, cameraPosition.x(), cameraPosition.y(), cameraPosition.z(), transformation);
                        // x: no dynamic directional shading, y: sky light scale, z: no fog, w: no end sea fade
                        params.set(0.0f, sublevel.getLatestSkyLightScale() / 15.0f, 0.0f, 0.0f);
                        subLevelUniform = storage.writeUniform(new SubLevelRenderPipelines.SubLevelUniform(transformation, faceShadeA, faceShadeB, params));
                    }

                    if (sectionIndex == -1) {
                        sectionIndex = sectionInfos.size();
                        sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(viewMatrix),
                                origin.getX() - anchor.getX(), origin.getY() - anchor.getY(), origin.getZ() - anchor.getZ(),
                                1.0f, atlasWidth, atlasHeight));
                    }

                    if (buffers.getIndexBuffer() == null && buffers.getIndexCount() > maxIndicesRequired) {
                        maxIndicesRequired = buffers.getIndexCount();
                    }

                    double distance = 0.0;
                    if (layer == ChunkSectionLayer.TRANSLUCENT) {
                        renderPose.transformPosition(sectionCenter.set(origin.getX() + 8.0, origin.getY() + 8.0, origin.getZ() + 8.0));
                        distance = sectionCenter.distanceSquared(cameraPosition.x(), cameraPosition.y(), cameraPosition.z());
                    }

                    pendingDraws.add(new PendingDraw(layer, buffers, sectionIndex, subLevelUniform, distance));
                }
            }
        }

        if (pendingDraws.isEmpty()) {
            storage.endFrame();
            return;
        }

        final GpuBufferSlice[] sectionUniforms = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));

        // Translucent geometry is drawn back to front
        pendingDraws.sort(Comparator.comparingDouble(PendingDraw::distance).reversed());

        final EnumMap<ChunkSectionLayer, List<RenderPass.Draw<Void>>> draws = new EnumMap<>(ChunkSectionLayer.class);
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            draws.put(layer, new ArrayList<>());
        }

        for (final PendingDraw pending : pendingDraws) {
            final SectionBuffers buffers = pending.buffers();
            final GpuBufferSlice sectionUniform = sectionUniforms[pending.sectionIndex()];
            final GpuBufferSlice subLevelUniform = pending.subLevelUniform();

            // Sections without their own index buffer use the shared sequential quad index buffer and its index type
            final GpuBuffer indexBuffer = buffers.getIndexBuffer();
            draws.get(pending.layer()).add(new RenderPass.Draw<>(
                    0,
                    buffers.getVertexBuffer(),
                    indexBuffer,
                    indexBuffer == null ? null : buffers.getIndexType(),
                    0,
                    buffers.getIndexCount(),
                    (unused, uniformUploader) -> {
                        uniformUploader.upload("ChunkSection", sectionUniform);
                        uniformUploader.upload(SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, subLevelUniform);
                    }
            ));
        }

        final RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        final GpuBuffer sequentialIndexBuffer = maxIndicesRequired == 0 ? null : sequentialBuffer.getBuffer(maxIndicesRequired);
        final VertexFormat.IndexType sequentialIndexType = maxIndicesRequired == 0 ? null : sequentialBuffer.type();
        final GpuSampler atlasSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);

        try (final RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "Simulated sub-level group sections",
                        target.getColorTextureView(),
                        OptionalInt.empty(),
                        target.getDepthTextureView(),
                        OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("Sampler2", lightTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

            for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                final List<RenderPass.Draw<Void>> list = draws.get(layer);
                if (list.isEmpty()) {
                    continue;
                }

                renderPass.setPipeline(SubLevelRenderPipelines.get(layer));
                renderPass.bindTexture("Sampler0", atlasTexture, atlasSampler);
                renderPass.drawMultipleIndexed(list, sequentialIndexBuffer, sequentialIndexType, DYNAMIC_UNIFORMS, null);
            }
        }

        // The ring buffer is fenced, so rotating per group keeps the next group from overwriting these uniforms
        storage.endFrame();
    }

    /**
     * Submits the block entities of all sub-levels. Unlike the level renderer, this includes block entities that have
     * flywheel visuals, since visualization is disabled while the group renders.
     */
    private static void submitBlockEntities(final Collection<ClientSubLevel> subLevels, final SubmitNodeCollector collector, final CameraRenderState cameraState, final Vec3 cameraPos, final float partialTicks) {
        final BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        final BlockEntityRenderDispatcherExtension extension = (BlockEntityRenderDispatcherExtension) dispatcher;
        final PoseStack poseStack = new PoseStack();
        final Matrix4f transformation = new Matrix4f();

        try {
            for (final ClientSubLevel sublevel : subLevels) {
                final SubLevelRenderData data = sublevel.getRenderData();
                if (data == null) {
                    continue;
                }

                final Pose3dc renderPose = sublevel.renderPose();
                final Vector3dc rotationPoint = renderPose.rotationPoint();

                // Block entity renderers check distances against the camera, which has to be in plot space for them
                extension.sable$setCameraPosition(renderPose.transformPositionInverse(cameraPos));
                data.getTransformation(cameraPos.x, cameraPos.y, cameraPos.z, transformation);

                for (final PlotChunkHolder holder : sublevel.getPlot().getLoadedChunks()) {
                    final LevelChunk chunk = holder.getChunk();
                    if (chunk == null) {
                        continue;
                    }

                    for (final BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity.isRemoved()) {
                            continue;
                        }

                        final BlockEntityRenderState state = dispatcher.tryExtractRenderState(blockEntity, partialTicks, null);
                        if (state == null) {
                            continue;
                        }

                        state.lightCoords = sublevel.scaleLightColor(state.lightCoords);

                        final BlockPos blockPos = blockEntity.getBlockPos();
                        poseStack.pushPose();
                        poseStack.mulPose(new Matrix4f(transformation).translate((float) (blockPos.getX() - rotationPoint.x()), (float) (blockPos.getY() - rotationPoint.y()), (float) (blockPos.getZ() - rotationPoint.z())));
                        dispatcher.submit(state, poseStack, collector, cameraState);
                        poseStack.popPose();
                    }
                }
            }
        } finally {
            extension.sable$setCameraPosition(null);
        }
    }

    /**
     * Submits the entities inside of (or tracking / riding) the sub-levels
     */
    private static void submitEntities(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final SubmitNodeCollector collector, final CameraRenderState cameraState, final Vec3 cameraPos, final boolean renderPlayers) {
        final Minecraft minecraft = Minecraft.getInstance();
        final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        final PoseStack poseStack = new PoseStack();

        for (final ClientSubLevel entitySubLevel : subLevels) {
            final List<Entity> entities = level.getEntitiesOfClass(Entity.class, entitySubLevel.getPlot().getBoundingBox().toAABB().inflate(16.0));

            for (final Entity entity : entities) {
                if (Sable.HELPER.getContaining(entity) != entitySubLevel && Sable.HELPER.getTrackingOrVehicleSubLevel(entity) != entitySubLevel) {
                    continue;
                }

                if (!renderPlayers && entity instanceof Player) {
                    continue;
                }

                final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(!level.tickRateManager().isEntityFrozen(entity));

                // Sable moves and rotates entities on sub-levels while extracting / submitting
                final EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
                // glowing outlines go to the level's outline target, not this one
                state.outlineColor = 0;
                dispatcher.submit(state, cameraState, state.x - cameraPos.x, state.y - cameraPos.y, state.z - cameraPos.z, poseStack, collector);
            }
        }
    }

    private static DynamicUniformStorage<SubLevelRenderPipelines.SubLevelUniform> getUniformStorage() {
        if (uniformStorage == null) {
            uniformStorage = SubLevelRenderPipelines.createUniformStorage();
        }
        return uniformStorage;
    }

    private static PerspectiveProjectionMatrixBuffer getProjectionBuffer() {
        if (projectionBuffer == null) {
            projectionBuffer = new PerspectiveProjectionMatrixBuffer("Simulated sub-level group");
        }
        return projectionBuffer;
    }

    private record PendingDraw(ChunkSectionLayer layer, SectionBuffers buffers, int sectionIndex, GpuBufferSlice subLevelUniform, double distance) {
    }
}
