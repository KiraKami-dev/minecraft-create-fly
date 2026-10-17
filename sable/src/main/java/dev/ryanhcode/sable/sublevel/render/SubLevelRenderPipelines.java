package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;

/**
 * The terrain pipelines used to draw compiled sub-level sections.
 * <p>
 * They match the vanilla terrain pipelines, but use {@code sable:core/sub_level_terrain} as vertex shader, which applies
 * the sub-level pose from the {@value #SUB_LEVEL_UNIFORM} uniform block. The section {@code ChunkPosition} is relative to
 * an anchor block of the sub-level instead of the camera, so the (potentially huge) plot coordinates never reach the
 * GPU as floats.
 */
public final class SubLevelRenderPipelines {

    /**
     * The name of the per sub-level uniform block
     */
    public static final String SUB_LEVEL_UNIFORM = "SableSubLevel";

    /**
     * The size of the per sub-level uniform block
     */
    public static final int SUB_LEVEL_UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().get();

    private static final RenderPipeline.Snippet SUB_LEVEL_TERRAIN_SNIPPET = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withVertexShader(Sable.sablePath("core/sub_level_terrain"))
            .withUniform(SUB_LEVEL_UNIFORM, UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public static final RenderPipeline SOLID = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_solid_terrain"))
            .build();

    public static final RenderPipeline CUTOUT = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_cutout_terrain"))
            .withShaderDefine("ALPHA_CUTOUT", 0.5F)
            .build();

    public static final RenderPipeline TRANSLUCENT = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_translucent_terrain"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .build();

    public static final RenderPipeline TRIPWIRE = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_tripwire_terrain"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build();

    /**
     * Depth-only copies of {@link #SOLID} and {@link #CUTOUT}, for the sky light shadow map
     */
    public static final RenderPipeline SHADOW_SOLID = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_shadow_solid"))
            .withColorWrite(false)
            .build();

    public static final RenderPipeline SHADOW_CUTOUT = RenderPipeline.builder(SUB_LEVEL_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/sub_level_shadow_cutout"))
            .withShaderDefine("ALPHA_CUTOUT", 0.5F)
            .withColorWrite(false)
            .build();

    private SubLevelRenderPipelines() {
    }

    /**
     * @param layer The vanilla section layer
     * @return The sub-level pipeline drawing that layer
     */
    public static RenderPipeline get(final ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> SOLID;
            case CUTOUT -> CUTOUT;
            case TRANSLUCENT -> TRANSLUCENT;
            case TRIPWIRE -> TRIPWIRE;
        };
    }

    /**
     * Creates the storage for {@link SubLevelUniform}s. Must be called on the render thread.
     */
    public static DynamicUniformStorage<SubLevelUniform> createUniformStorage() {
        return new DynamicUniformStorage<>("Sable Sub-Level UBO", SUB_LEVEL_UBO_SIZE, 16);
    }

    /**
     * The per sub-level uniform block.
     *
     * @param pose      Anchor-relative plot position to camera-relative world position
     * @param faceShadeA Block face brightness for down, up, north, south
     * @param faceShadeB Block face brightness for west, east
     * @param params    x: dynamic directional shading, y: sky light scale, z: fog, w: unused
     */
    public record SubLevelUniform(Matrix4fc pose, Vector4fc faceShadeA, Vector4fc faceShadeB, Vector4fc params) implements DynamicUniformStorage.DynamicUniform {

        public SubLevelUniform {
            pose = new Matrix4f(pose);
            faceShadeA = new Vector4f(faceShadeA);
            faceShadeB = new Vector4f(faceShadeB);
            params = new Vector4f(params);
        }

        @Override
        public void write(final ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putMat4f(this.pose)
                    .putVec4(this.faceShadeA)
                    .putVec4(this.faceShadeB)
                    .putVec4(this.params);
        }
    }
}
