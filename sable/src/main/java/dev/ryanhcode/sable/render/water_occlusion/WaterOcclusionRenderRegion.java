package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.Collection;

/**
 * A culled voxel mesh of a water occlusion region, rendered depth-only into the water occlusion buffers.
 */
@ApiStatus.Internal
public class WaterOcclusionRenderRegion {
    private static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    private Collection<BlockPos> unbuiltData;
    private boolean built = false;
    private GpuBuffer vertexBuffer;
    private int indexCount;
    private Vec3 origin;
    private RenderSystem.AutoStorageIndexBuffer indices;
    private GpuBuffer indexBuffer;

    public WaterOcclusionRenderRegion(final Collection<BlockPos> blocks) {
        this.unbuiltData = blocks;
    }

    /**
     * Uploads the transform for a following {@link #draw}. Must be called while no render pass is open, as
     * uploads cannot be done during one.
     *
     * @param modelView the camera rotation matrix of the level
     * @return the transform to pass to {@link #draw}, or null if there is nothing to draw
     */
    public GpuBufferSlice prepare(final Matrix4fc modelView) {
        if (!this.built) {
            this.build();
        }

        if (this.vertexBuffer == null) {
            return null;
        }

        final Minecraft client = Minecraft.getInstance();
        final SubLevel subLevel = Sable.HELPER.getContaining(client.level, this.origin);

        Vec3 globalOrigin = this.origin;
        final Quaternionf globalOrientation = new Quaternionf();

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            final Pose3dc renderPose = clientSubLevel.renderPose();
            globalOrigin = renderPose.transformPosition(globalOrigin);
            globalOrientation.set(renderPose.orientation());
        }

        final Vec3 relativePos = globalOrigin.subtract(client.gameRenderer.getMainCamera().position());

        final Matrix4f modelViewMatrix = new Matrix4f(modelView)
                .setTranslation(0.0f, 0.0f, 0.0f)
                .translate((float) relativePos.x, (float) relativePos.y, (float) relativePos.z)
                .rotate(globalOrientation);

        this.indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.indexBuffer = this.indices.getBuffer(this.indexCount);
        return RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, WHITE, new Vector3f(), new Matrix4f());
    }

    /**
     * Draws this region into the currently open render pass. The pass must already have the water occlusion
     * depth pipeline and default uniforms set.
     *
     * @param transform the result of {@link #prepare}
     */
    public void draw(final RenderPass renderPass, final GpuBufferSlice transform) {
        renderPass.setUniform("DynamicTransforms", transform);
        renderPass.setVertexBuffer(0, this.vertexBuffer);
        renderPass.setIndexBuffer(this.indexBuffer, this.indices.type());
        renderPass.drawIndexed(0, 0, this.indexCount, 1);
    }

    private void build() {
        this.built = true;

        final BlockPos firstBlock = this.unbuiltData.stream().findFirst().orElseThrow();
        final Vector3i minBlock = new Vector3i(firstBlock.getX(), firstBlock.getY(), firstBlock.getZ());
        final Vector3i maxBlock = new Vector3i(firstBlock.getX(), firstBlock.getY(), firstBlock.getZ());
        final Vector3i currentBlock = new Vector3i();

        for (final BlockPos block : this.unbuiltData) {
            currentBlock.set(block.getX(), block.getY(), block.getZ());
            minBlock.min(currentBlock);
            maxBlock.max(currentBlock);
        }

        int gridSize = maxBlock.x() - minBlock.x() + 1;
        gridSize = Math.max(gridSize, maxBlock.y() - minBlock.y() + 1);
        gridSize = Math.max(gridSize, maxBlock.z() - minBlock.z() + 1);

        final BlockPos originBlock = new BlockPos(minBlock.x(), minBlock.y(), minBlock.z());
        this.origin = Vec3.atLowerCornerOf(originBlock);

        final SimpleCulledRenderRegionBuilder builder = new SimpleCulledRenderRegionBuilder(gridSize);

        for (final BlockPos blockPos : this.unbuiltData) {
            builder.add(blockPos.getX() - originBlock.getX(), blockPos.getY() - originBlock.getY(), blockPos.getZ() - originBlock.getZ());
        }

        builder.buildNoGreedy();

        try (final ByteBufferBuilder byteBuilder = new ByteBufferBuilder(1 << 16)) {
            final BufferBuilder bufferBuilder = new BufferBuilder(byteBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.render(new Matrix4f(), bufferBuilder);

            try (final MeshData mesh = bufferBuilder.build()) {
                if (mesh != null) {
                    this.indexCount = mesh.drawState().indexCount();
                    this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Sable water occlusion region", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                }
            }
        }

        this.unbuiltData = null;
    }

    public void free() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }
}
