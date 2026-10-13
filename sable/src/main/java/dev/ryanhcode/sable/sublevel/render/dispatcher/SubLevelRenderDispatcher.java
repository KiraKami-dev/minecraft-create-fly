package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fc;
import org.lwjgl.system.NativeResource;

import java.util.function.Consumer;

/**
 * Renders sub-levels into the world.
 * <p>
 * On 1.21.11 the section layers are drawn with the vanilla terrain {@link com.mojang.blaze3d.pipeline.RenderPipeline}s:
 * {@link #prepareSections} runs alongside {@code LevelRenderer#prepareChunkRenders} and collects one draw per compiled
 * sub-level section, with a per-section {@code ChunkSection} uniform block holding the sub-level pose. The draws are
 * then issued by {@link #renderSectionLayerGroup} right after vanilla draws the same layer group.
 */
@ApiStatus.Internal
public interface SubLevelRenderDispatcher extends NativeResource, ResourceManagerReloadListener {

    /**
     * @return The current sub-level renderer instance
     */
    static SubLevelRenderDispatcher get() {
        return SubLevelRenderer.getDispatcher();
    }

    /**
     * Resizes the specified render data.
     *
     * @param subLevel   The sub-level to resize
     * @param renderData The current render data
     * @return The new render data to use
     */
    SubLevelRenderData resize(final ClientSubLevel subLevel, final SubLevelRenderData renderData);

    /**
     * Creates a new render data instance for the specified sub-level.
     *
     * @param subLevel The sub-level to create render data for
     * @return A new render data instance
     */
    SubLevelRenderData createRenderData(final ClientSubLevel subLevel);

    /**
     * Rebuilds the specified sub-levels when F3+A is pressed.
     *
     * @param sublevels The sub-levels to rebuild
     */
    default void rebuild(final Iterable<ClientSubLevel> sublevels) {
        for (final ClientSubLevel sublevel : sublevels) {
            sublevel.getRenderData().rebuild();
        }
    }

    /**
     * Updates the current culling state for all sub-levels.
     *
     * @param sublevels   The sub-levels to update
     * @param cameraX     The x position of the camera
     * @param cameraY     The y position of the camera
     * @param cameraZ     The z position of the camera
     * @param frustum     The current frustum used for culling
     * @param isSpectator Whether the player is in spectator mode
     */
    void updateCulling(final Iterable<ClientSubLevel> sublevels, final double cameraX, final double cameraY, final double cameraZ, final Frustum frustum, boolean isSpectator);

    /**
     * Collects the section draws for this frame. Called from the main frame pass, right after vanilla prepared its own
     * chunk section draws.
     *
     * @param sublevels The sub-levels to render
     * @param modelView The terrain model-view matrix (camera rotation)
     * @param cameraX   The x position of the camera
     * @param cameraY   The y position of the camera
     * @param cameraZ   The z position of the camera
     */
    void prepareSections(final Iterable<ClientSubLevel> sublevels, final Matrix4fc modelView, final double cameraX, final double cameraY, final double cameraZ);

    /**
     * Draws all sub-level sections in the layers of the given group into the group's output target.
     *
     * @param group   The layer group vanilla just finished rendering
     * @param sampler The sampler vanilla used for the block atlas
     */
    void renderSectionLayerGroup(final ChunkSectionLayerGroup group, final GpuSampler sampler);

    /**
     * Collects the block entities of the sub-level that should be extracted for rendering this frame.
     *
     * @param subLevel The sub-level
     * @param consumer The consumer to accept renderable block entities
     */
    void collectRenderableBlockEntities(final ClientSubLevel subLevel, final Consumer<BlockEntity> consumer);

    /**
     * Submits geometry that isn't part of compiled sections (single-block sub-levels) to the feature renderer.
     *
     * @param sublevels       The sub-levels to render
     * @param poseStack       An empty pose stack
     * @param submitCollector The submit collector
     * @param cameraX         The x position of the camera
     * @param cameraY         The y position of the camera
     * @param cameraZ         The z position of the camera
     */
    void submitAfterSections(final Iterable<ClientSubLevel> sublevels, final PoseStack poseStack, final SubmitNodeCollector submitCollector, final double cameraX, double cameraY, double cameraZ);

    void addDebugInfo(final Consumer<String> consumer);

    default void preRenderChunks(final Camera camera) {
    }
}
