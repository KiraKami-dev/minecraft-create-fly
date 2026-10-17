package dev.simulated_team.simulated.content.blocks.merging_glue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Upstream extended {@code SmartBlockEntityRenderer} but replaced its rendering without calling super (no filter or
 * link slots), so this is a plain {@link BlockEntityRenderer}. The strands are computed while extracting and replayed
 * into the vertex consumer on submission.
 */
public class MergingGlueRenderer implements BlockEntityRenderer<MergingGlueBlockEntity, MergingGlueRenderer.MergingGlueRenderState> {
    private static final RenderType STRAND_RENDER_TYPE = RenderTypes.entityCutout(Simulated.path("textures/block/merging_glue/strand.png"));

    public MergingGlueRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public MergingGlueRenderState createRenderState() {
        return new MergingGlueRenderState();
    }

    @Override
    public void extractRenderState(final MergingGlueBlockEntity be, final MergingGlueRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        state.vertices.clear();

        if (!be.isController()) {
            return;
        }

        final MergingGlueBlockEntity other = be.getPartnerGlue();
        if (other == null) {
            return;
        }

        final SubLevel otherSubLevel = Sable.HELPER.getContainingClient(other);
        final SubLevel subLevel = Sable.HELPER.getContainingClient(be);

        final BlockPos blockPos = be.getBlockPos();

        final Vector3dc center = be.getCenter(new Vector3d());
        final Vector3d otherCenter = other.getCenter(new Vector3d());

        final BlockState blockState = be.getBlockState();
        final Direction facing = blockState.getValue(SpringBlock.FACING);
        final Direction otherFacing = other.getBlockState().getValue(SpringBlock.FACING);
        final Vector3dc normalA = JOMLConversion.atLowerCornerOf(facing.getUnitVec3i());
        final Vector3d normalB = JOMLConversion.atLowerCornerOf(otherFacing.getUnitVec3i());

        final Pose3dc renderPose = subLevel != null ? ((ClientSubLevel) subLevel).renderPose() : null;
        final Pose3dc otherRenderPose = otherSubLevel != null ? ((ClientSubLevel) otherSubLevel).renderPose() : null;

        // if one is horizontal, both glue must be horizontal
        final boolean horizontal = facing.getAxis().isHorizontal();

        final Vector3dc rightA = horizontal ?
                JOMLConversion.atLowerCornerOf(facing.getClockWise().getUnitVec3i(), new Vector3d()) :
                OrientedBoundingBox3d.FORWARD;

        final Vector3d rightB = horizontal ?
                JOMLConversion.atLowerCornerOf(otherFacing.getCounterClockWise().getUnitVec3i(), new Vector3d()) :
                new Vector3d(OrientedBoundingBox3d.FORWARD);

        final Vector3dc upA = horizontal ?
                new Vector3d(0.0, 1.0, 0.0)
                : OrientedBoundingBox3d.RIGHT;

        final Vector3d upB = horizontal ?
                new Vector3d(0.0, 1.0, 0.0)
                : new Vector3d(OrientedBoundingBox3d.RIGHT);

        if (otherRenderPose != null) {
            otherRenderPose.transformNormal(normalB);
            otherRenderPose.transformNormal(rightB);
            otherRenderPose.transformNormal(upB);
            otherRenderPose.transformPosition(otherCenter);
        }

        if (renderPose != null) {
            renderPose.transformNormalInverse(normalB);
            renderPose.transformNormalInverse(rightB);
            renderPose.transformNormalInverse(upB);
            renderPose.transformPositionInverse(otherCenter);
        }


        final Vector3d strandCenterA = center.sub(JOMLConversion.atLowerCornerOf(blockPos), new Vector3d());
        final Vector3d strandCenterB = otherCenter.sub(JOMLConversion.atLowerCornerOf(blockPos), new Vector3d());
        final Vector3d strandPosA = new Vector3d();
        final Vector3d strandPosB = new Vector3d();

        final Vector2d[] strandPositions = {
          new Vector2d(0.25, 0.25),
          new Vector2d(0.45, 0.3),
          new Vector2d(0.6, 0.6),
          new Vector2d(0.65, 0.7)
        };

        for (int i = 0; i < 2; i++) {
            final Vector2d strandA = strandPositions[i * 2].sub(0.5, 0.5, new Vector2d()).mul(0.75);
            final Vector2d strandB = strandPositions[i * 2 + 1].sub(0.5, 0.5, new Vector2d()).mul(0.75);

            renderGlueCross(
                    strandPosA.set(strandCenterA).fma(strandA.x, rightA).fma(strandA.y, upA),
                    upA,
                    rightA,

                    strandPosB.set(strandCenterB).fma(strandB.x, rightB).fma(strandB.y, upB),
                    upB,
                    rightB,

                    state.vertices);
        }

    }

    @Override
    public void submit(final MergingGlueRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        if (state.vertices.isEmpty()) {
            return;
        }

        queue.submitCustomGeometry(ms, STRAND_RENDER_TYPE, state);
    }

    /**
     * Records one vertex (position + uv); upstream wrote it straight into the buffer
     */
    private static void addVertex(final FloatArrayList vertices, final Vector3dc pos, final float u, final float v) {
        vertices.add((float) pos.x());
        vertices.add((float) pos.y());
        vertices.add((float) pos.z());
        vertices.add(u);
        vertices.add(v);
    }

    private static void renderGlueCross(final Vector3dc posA,
                                        final Vector3dc upA,
                                        final Vector3dc rightA,
                                        final Vector3dc posB,
                                        final Vector3dc upB,
                                        final Vector3dc rightB,
                                        final FloatArrayList buffer) {
        final Vector3d vertex = new Vector3d();

        // vertical plane & backface
        addVertex(buffer, posA.fma(-0.5, upA, vertex), 0.0f, 0.0f);
        addVertex(buffer, posA.fma(0.5, upA, vertex), 0.0f, 1.0f);
        addVertex(buffer, posB.fma(0.5, upB, vertex), 1.0f, 1.0f);
        addVertex(buffer, posB.fma(-0.5, upB, vertex), 1.0f, 0.0f);

        addVertex(buffer, posB.fma(-0.5, upB, vertex), 1.0f, 0.0f);
        addVertex(buffer, posB.fma(0.5, upB, vertex), 1.0f, 1.0f);
        addVertex(buffer, posA.fma(0.5, upA, vertex), 0.0f, 1.0f);
        addVertex(buffer, posA.fma(-0.5, upA, vertex), 0.0f, 0.0f);

        // horizontal plane & backface
        addVertex(buffer, posA.fma(-0.5, rightA, vertex), 0.0f, 0.0f);
        addVertex(buffer, posA.fma(0.5, rightA, vertex), 0.0f, 1.0f);
        addVertex(buffer, posB.fma(0.5, rightB, vertex), 1.0f, 1.0f);
        addVertex(buffer, posB.fma(-0.5, rightB, vertex), 1.0f, 0.0f);

        addVertex(buffer, posB.fma(-0.5, rightB, vertex), 1.0f, 0.0f);
        addVertex(buffer, posB.fma(0.5, rightB, vertex), 1.0f, 1.0f);
        addVertex(buffer, posA.fma(0.5, rightA, vertex), 0.0f, 1.0f);
        addVertex(buffer, posA.fma(-0.5, rightA, vertex), 0.0f, 0.0f);
    }

    public static class MergingGlueRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        /**
         * x, y, z, u, v per vertex, relative to the block entity
         */
        public final FloatArrayList vertices = new FloatArrayList();

        @Override
        public void render(final PoseStack.Pose ms, final VertexConsumer buffer) {
            final Matrix4f pose = ms.pose();
            final float[] data = this.vertices.elements();

            for (int i = 0; i < this.vertices.size(); i += 5) {
                buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                        .setColor(0xffffffff)
                        .setUv(data[i + 3], data[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(this.lightCoords)
                        .setNormal(ms, 0.0f, 1.0f, 0.0f);
            }
        }
    }
}
