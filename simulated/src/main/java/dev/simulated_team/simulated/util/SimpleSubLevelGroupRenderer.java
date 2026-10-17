package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.Collection;

/**
 * Renders a group of sub-levels into an offscreen target (contraption diagrams, end sea shadow map).
 * <p>
 * Upstream drew into a Veil {@code AdvancedFbo}; on 1.21.11 the target is a vanilla {@link RenderTarget}.
 */
public class SimpleSubLevelGroupRenderer {
    /**
     * True while sub-level sections are drawn for a diagram (read by the diagram sub-level render dispatcher mixin).
     */
    public static boolean RENDERING_SIMPLE = false;

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
     * Renders the given sub-levels (terrain, block entities and their entities) into {@code target}, seen from
     * {@code cameraPosition} with the given orientation and matrices.
     */
    // PORT-TODO(1.21.11): offscreen sub-level rendering is not ported. Upstream re-ran the pre-1.21.5 render path by hand
    //  (RenderType#setupRenderState + ShaderInstance per chunk layer through SubLevelRenderDispatcher#renderSectionLayer,
    //  VanillaSubLevelBlockEntityRenderer, Flywheel embeddings via VisualManagerExtension, LevelRenderer#renderEntity,
    //  Veil's camera matrices and dynamic FBOs). On 1.21.11 sections are drawn through RenderPasses inside the frame graph
    //  and Sable's dispatcher only draws into the main frame (prepareSections/renderSectionLayerGroup), block entities and
    //  entities go through extract/submit. This needs an offscreen entry point in Sable's SubLevelRenderDispatcher (draw a
    //  set of sub-levels into a RenderTarget with custom model-view/projection) plus the feature-renderer submit path for
    //  block entities and entities. Until then the target is only cleared, so diagrams and end sea shadows render empty.
    public static void renderGroup(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final RenderTarget target, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks, final boolean renderPlayers) {
        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (target.getDepthTexture() != null) {
            encoder.clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
        } else {
            encoder.clearColorTexture(target.getColorTexture(), 0);
        }
    }
}
