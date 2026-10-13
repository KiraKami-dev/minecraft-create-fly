package dev.ryanhcode.sable.render.sky_light_shadow;

import dev.ryanhcode.sable.veillite.api.client.render.MatrixStack;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4fc;

/**
 * Sub-levels casting shadows on the world's sky light.
 * <p>
 * PORT-TODO(1.21.11): Not ported; hard-disabled regardless of {@code sub_level_skylight_shadows}. Missing pieces:
 * <ol>
 *     <li>The shadow map: a top-down orthographic depth render of every sub-level into a 256x256 target
 *     (formerly {@code VeilLevelPerspectiveRenderer} + a Veil-managed framebuffer). This needs a second, camera-less
 *     pass through the sub-level section renderer with entities and particles suppressed.</li>
 *     <li>The sampling: formerly a glslprocessor injection into every {@code rendertype_*} chunk vertex shader
 *     (see {@link SableDynamicSkyLightShadowPreProcessor}). In 1.21.11 this needs a custom terrain
 *     {@code RenderPipeline} variant (extra sampler and uniform buffer) swapped in the way
 *     {@code WaterOcclusionRenderer} swaps the translucent pipeline.</li>
 * </ol>
 * The old logic is in the git history of this file and in the original mod's sources.
 */
public class SableSkyLightShadows {

    public static final float SHADOW_VOLUME_SIZE = 256f / 2f;

    /**
     * Flip once the shadow map and terrain pipeline above exist
     */
    private static final boolean SUPPORTED = false;

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return SUPPORTED && isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableSkyLightShadows.isEnabled = isEnabled;
    }

    public static void renderShadowMap(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
        // PORT-TODO(1.21.11): see class docs
    }

    public static boolean renderingShadowMap() {
        return false;
    }
}
