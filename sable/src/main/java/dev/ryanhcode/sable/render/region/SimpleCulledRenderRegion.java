package dev.ryanhcode.sable.render.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
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
import org.joml.Vector4fc;

import java.util.Collection;

/**
 * A culled voxel mesh of a set of blocks, transformed with the sub-level containing it (if any).
 * <p>
 * On 1.21.11 the mesh lives in a {@link GpuBuffer} and is drawn into a {@link RenderPass} the caller opened with a
 * pipeline using the {@link #getVertexFormat() vertex format} and a {@code DynamicTransforms} uniform block.
 */
@ApiStatus.Internal
public abstract class SimpleCulledRenderRegion {
    private Collection<BlockPos> unbuiltData;
    private boolean built = false;
    private GpuBuffer buffer;
    private int indexCount;
    private Vec3 origin;

    public SimpleCulledRenderRegion(final Collection<BlockPos> blocks) {
        this.unbuiltData = blocks;
    }

    /**
     * Draws this region into the open render pass.
     *
     * @param renderPass The render pass, with the pipeline and default uniforms already set
     * @param modelView  The camera rotation matrix of the level
     * @param color      The color modulator
     */
    public void render(final RenderPass renderPass, final Matrix4fc modelView, final Vector4fc color) {
        if (!this.built) {
            this.build();
        }

        if (this.buffer == null) {
            return;
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

        final GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, color, new Vector3f(), new Matrix4f());
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

        renderPass.setUniform("DynamicTransforms", transform);
        renderPass.setVertexBuffer(0, this.buffer);
        renderPass.setIndexBuffer(indices.getBuffer(this.indexCount), indices.type());
        renderPass.drawIndexed(0, 0, this.indexCount, 1);
    }

    public void build() {
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

        final SimpleCulledRenderRegionBuilder builder = this.createMeshBuilder(gridSize);

        for (final BlockPos blockPos : this.unbuiltData) {
            builder.add(blockPos.getX() - originBlock.getX(), blockPos.getY() - originBlock.getY(), blockPos.getZ() - originBlock.getZ());
        }

        builder.buildNoGreedy();

        try (final ByteBufferBuilder byteBuilder = new ByteBufferBuilder(1 << 16)) {
            final BufferBuilder bufferBuilder = new BufferBuilder(byteBuilder, VertexFormat.Mode.QUADS, this.getVertexFormat());
            builder.render(new Matrix4f(), bufferBuilder);

            try (final MeshData mesh = bufferBuilder.build()) {
                if (mesh != null) {
                    this.indexCount = mesh.drawState().indexCount();
                    this.buffer = RenderSystem.getDevice().createBuffer(() -> "Sable culled render region", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                }
            }
        }

        this.unbuiltData = null;
    }

    public Vec3 getOrigin() {
        return this.origin;
    }

    public abstract SimpleCulledRenderRegionBuilder createMeshBuilder(int gridSize);

    public abstract VertexFormat getVertexFormat();

    public void free() {
        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
    }
}
