package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector3i;
import org.lwjgl.system.NativeResource;

import java.util.Iterator;
import java.util.List;

public class HeatedCulledRenderRegion implements NativeResource {
    private static final Vector4f COLOR = new Vector4f(0.85f, 0.85f, 0.85f, 1.0f);

    private Balloon balloon;
    private boolean built = false;
    @Nullable
    private GpuBuffer buffer;
    private int indexCount;
    private Vec3 origin;
    private final LevelAccelerator accelerator;

    public HeatedCulledRenderRegion(final LevelAccelerator accelerator, final Balloon balloon) {
        this.accelerator = accelerator;
        this.balloon = balloon;
    }

    /**
     * Draws the region into the given pass (pipeline, textures and the {@code HotAirInfo} block are bound by the
     * caller). Upstream drew a {@code VertexBuffer} with the current Veil shader instance.
     *
     * @param modelView The world model view matrix (camera rotation)
     * @return the index count needed from the shared quad index buffer, or 0 if nothing was drawn
     */
    public int render(final RenderPass renderPass, final Matrix4fc modelView, final RenderSystem.AutoStorageIndexBuffer indices) {
        if (!this.built) {
            this.build();
        }

        if (this.buffer == null || this.indexCount == 0) {
            return 0;
        }

        final Minecraft client = Minecraft.getInstance();
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(this.origin);

        Vec3 globalOrigin = this.origin;
        final Quaternionf globalOrientation = new Quaternionf();

        if (subLevel != null) {
            final Pose3dc renderPose = subLevel.renderPose();
            globalOrigin = renderPose.transformPosition(globalOrigin);
            globalOrientation.set(renderPose.orientation());
        }

        final Vec3 relativePos = globalOrigin.subtract(client.gameRenderer.getMainCamera().position());

        final Matrix4f modelViewMatrix = new Matrix4f(modelView)
                .setTranslation(0.0f, 0.0f, 0.0f)
                .translate((float) relativePos.x, (float) relativePos.y, (float) relativePos.z)
                .rotate(globalOrientation);

        final GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, COLOR, new Vector3f(), new Matrix4f());
        renderPass.setUniform("DynamicTransforms", transform);
        renderPass.setVertexBuffer(0, this.buffer);
        renderPass.setIndexBuffer(indices.getBuffer(this.indexCount), indices.type());
        renderPass.drawIndexed(0, 0, this.indexCount, 1);
        return this.indexCount;
    }

    public void build() {
        final BoundingBox3ic bounds = this.balloon.getBounds();
        final Vector3i minBlock = new Vector3i(bounds.minX(), bounds.minY(), bounds.minZ());
        final Vector3i maxBlock = new Vector3i(bounds.maxX(), bounds.maxY(), bounds.maxZ());

        int gridSize = maxBlock.x() - minBlock.x() + 1;
        gridSize = Math.max(gridSize, maxBlock.y() - minBlock.y() + 1);
        gridSize = Math.max(gridSize, maxBlock.z() - minBlock.z() + 1);

        final BlockPos originBlock = new BlockPos(minBlock.x(), minBlock.y(), minBlock.z());
        this.origin = Vec3.atLowerCornerOf(originBlock);

        final SimpleCulledRenderRegionBuilder builder = this.createMeshBuilder(gridSize);

        final BalloonLayerGraph graph = this.balloon.getGraph();


        for (int y = graph.getMinY(); y <= graph.getMaxY(); y++) {
            final List<BalloonLayerData> layers = graph.getLayersAtY(y);

            for (final BalloonLayerData layer : layers) {
                final Iterator<BlockPos> layerBlocks = layer.blockIterator();

                while (layerBlocks.hasNext()) {
                    final BlockPos blockPos = layerBlocks.next();

                    builder.add(blockPos.getX() - originBlock.getX(),
                            blockPos.getY() - originBlock.getY(),
                            blockPos.getZ() - originBlock.getZ());
                }
            }
        }

        builder.buildNoGreedy();

        try (final ByteBufferBuilder byteBuffer = new ByteBufferBuilder(this.getVertexFormat().getVertexSize() * 256)) {
            final BufferBuilder bufferBuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, this.getVertexFormat());
            builder.render(new Matrix4f(), bufferBuilder);

            this.balloon = null;

            try (final MeshData builtData = bufferBuilder.build()) {
                if (builtData != null) {
                    this.buffer = RenderSystem.getDevice().createBuffer(() -> "Aeronautics hot air region", GpuBuffer.USAGE_VERTEX, builtData.vertexBuffer());
                    this.indexCount = builtData.drawState().indexCount();
                } else {
                    this.buffer = null;
                    this.indexCount = 0;
                }
            }
        }

        this.built = true;
    }

    public Vec3 getOrigin() {
        return this.origin;
    }

    public SimpleCulledRenderRegionBuilder createMeshBuilder(final int gridSize) {
        return new HeatedCulledRenderRegionBuilder(BlockPos.containing(this.getOrigin()), this.accelerator, gridSize);
    }

    public VertexFormat getVertexFormat() {
        return DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
    }

    @Override
    public void free() {
        if (this.built && this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
    }
}
