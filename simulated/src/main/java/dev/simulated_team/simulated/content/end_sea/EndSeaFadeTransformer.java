package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.resources.Identifier;

/**
 * Fades sub-level blocks below the end sea towards the sea color.
 * <p>
 * Upstream this was a Veil shader pre-processor that patched the vanilla chunk shaders and read an
 * {@code EndSeaCameraY} uniform. On 1.21.11 sub-levels are drawn with Sable's {@code sable:core/sub_level_terrain}
 * vertex shader, so that source is patched while shaders load
 * ({@link dev.simulated_team.simulated.mixin.end_sea.ShaderManagerMixin}), and the camera offset is passed in the
 * unused {@code SubLevelParams.w} component of Sable's per sub-level uniform block
 * ({@link dev.simulated_team.simulated.mixin.end_sea.VanillaSubLevelRenderDispatcherMixin}); 0 disables the fade.
 */
// PORT-TODO(1.21.11): single-block sub-levels are drawn by Sable as vanilla moving blocks (shared block shader), so they
//  don't fade into the end sea; upstream patched every chunk layer shader and faded them too.
public class EndSeaFadeTransformer {

    /**
     * The file of the sub-level terrain vertex shader
     */
    public static final Identifier SUB_LEVEL_TERRAIN = Sable.sablePath("shaders/core/sub_level_terrain.vsh");

    private static final String FADE = """

                // Simulated: fade towards the end sea below it (SubLevelParams.w = camera y - end sea start y, 0 = off)
                if (SubLevelParams.w != 0.0) {
                    vertexColor.rgb = mix(vertexColor.rgb, vec3(0.086, 0.078, 0.109) * 2.0, clamp((-pos.y - SubLevelParams.w) / 30.0, 0.0, 1.0));
                }
            """;

    /**
     * @param location the file location of the shader source
     * @param type     the shader type
     * @param source   the preprocessed shader source
     * @return the source to compile
     */
    public static String transform(final Identifier location, final ShaderType type, final String source) {
        if (type != ShaderType.VERTEX || !SUB_LEVEL_TERRAIN.equals(location)) {
            return source;
        }

        // The fade needs the camera relative position and the sub-level uniform block of Sable's shader
        final int end = source.lastIndexOf('}');
        if (end < 0 || !source.contains("SubLevelParams") || !source.contains("vec3 pos =") || !source.contains("vertexColor")) {
            Simulated.LOGGER.warn("Unexpected source in {}, sub-levels will not fade into the end sea", location);
            return source;
        }

        return source.substring(0, end) + FADE + source.substring(end);
    }
}
