package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Objects;

/**
 * Renders the rope strand owned by a rope holder.
 * <p>
 * 1.21.11 render-state split of the upstream {@code render(be, holder, partialTick, poseStack, bufferSource)}: everything
 * that reads the block entity, the client strand or the level happens in {@link #extract}, on the render thread before
 * submission; {@link #submit} only queues the recorded segments.
 */
public class RopeStrandRenderer {
    public record RopeRenderPoint(Quaternionf orientation, Vector3d position) { }

    /**
     * One rope segment: a knot (except for the first segment) and a middle piece stretched to {@code length}
     */
    private record Segment(Vector3d renderPos, Quaternionf orientation, float length, int light, boolean knot) { }

    /**
     * Everything needed to draw one strand, recorded during extraction
     */
    public static class RopeRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        private final BlockPos ownerPos;
        private final SuperByteBuffer middle;
        private final SuperByteBuffer knot;
        private final List<Segment> segments;
        @Nullable
        private final OutlineRenderer outline;

        private RopeRenderState(final BlockPos ownerPos, final SuperByteBuffer middle, final SuperByteBuffer knot, final List<Segment> segments, @Nullable final OutlineRenderer outline) {
            this.ownerPos = ownerPos;
            this.middle = middle;
            this.knot = knot;
            this.segments = segments;
            this.outline = outline;
        }

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            final PoseStack ps = new PoseStack();
            ps.last().set(pose);

            for (final Segment segment : this.segments) {
                final Vector3d renderPos = segment.renderPos();

                ps.pushPose();
                ps.translate(renderPos.x - (this.ownerPos.getX()), renderPos.y - (this.ownerPos.getY()), renderPos.z - (this.ownerPos.getZ()));
                ps.mulPose(segment.orientation());
                ps.translate(-0.5, -0.5, -0.5);

                if (segment.knot()) {
                    this.knot.light(segment.light())
                            .renderInto(ps.last(), vb);
                }
                ps.translate(0.0, 0.5, 0.0);
                ps.scale(1.0f, segment.length(), 1.0f);

                this.middle.light(segment.light())
                        .renderInto(ps.last(), vb);
                ps.popPose();
            }
        }
    }

    /**
     * The hover outline of a strand ({@code RenderType.lines()} upstream)
     */
    private record OutlineRenderer(float rad, ObjectArrayList<RopeRenderPoint> ropeRenderPoints, BlockPos ownerPos, float lineWidth) implements SubmitNodeCollector.CustomGeometryRenderer {
        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer linesVB) {
            renderOutline(pose, linesVB, this.rad, this.ropeRenderPoints, this.ownerPos, this.lineWidth);
        }
    }

    /**
     * Records the strand owned by the given holder, if it has one to render.
     *
     * @return the recorded strand, or null if there is nothing to render
     */
    public static @Nullable RopeRenderState extract(final SmartBlockEntity be, final RopeStrandHolderBehavior ropeHolder, final float partialTick) {
        final Level level = be.getLevel();
        assert level != null;

        final BlockPos ownerPos = be.getBlockPos();

        final SuperByteBuffer middle = CachedBuffers.partialFacing(SimPartialModels.ROPE, AllBlocks.ROPE.defaultBlockState(), Direction.NORTH);
        final SuperByteBuffer knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_KNOT, AllBlocks.ROPE.defaultBlockState(), Direction.NORTH);

        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        Pose3dc containingPose = null;

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            containingPose = clientSubLevel.renderPose();
        }

        final ClientRopeStrand rope = ropeHolder.getClientStrand();

        final float rad = 3.0f / 16.0f;

        if (ropeHolder.ownsRope() && rope != null && ropeHolder.getClientStrand() != null) {
            final ClientRopeStrand clientStrand = ropeHolder.getClientStrand();

            final List<ClientRopePoint> points = clientStrand.getPoints();

            if (points.size() <= 1) {
                return null;
            }

            final ObjectArrayList<RopeRenderPoint> ropeRenderPoints = buildRenderPoints(partialTick, points);

            if (ropeRenderPoints.isEmpty()) return null;

            final List<Segment> segments = new ObjectArrayList<>(ropeRenderPoints.size());
            for (int i = 1; i < ropeRenderPoints.size(); i++) {
                final RopeRenderPoint renderPoint0 = ropeRenderPoints.get(i - 1);
                final RopeRenderPoint renderPoint1 = ropeRenderPoints.get(i);
                final Vector3d globalRenderPos = new Vector3d(renderPoint0.position());
                final Vector3d renderPos = renderPoint0.position();
                final Quaternionf orientation = renderPoint0.orientation();

                final double length = renderPoint1.position().distance(renderPoint0.position());

                if (containingPose != null) {
                    containingPose.transformPositionInverse(renderPos);
                    orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
                }

                final BlockPos pos = BlockPos.containing(globalRenderPos.x, globalRenderPos.y, globalRenderPos.z);
                final int worldLight = LevelRenderer.getLightColor(level, pos);

                // copies, as the render points are transformed again below for the outline
                segments.add(new Segment(new Vector3d(renderPos), new Quaternionf(orientation), (float) length, worldLight, i > 1));
            }

            final RopeRenderPoint last = ropeRenderPoints.getLast();
            if (containingPose != null) {
                final Vector3d renderPos = last.position();
                final Quaternionf orientation = last.orientation();

                containingPose.transformPositionInverse(renderPos);
                orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
            }

            OutlineRenderer outline = null;
            if (Objects.equals(ZiplineClientManager.hoveringRope, clientStrand.getUuid())) {
                outline = new OutlineRenderer(rad, ropeRenderPoints, ownerPos, Minecraft.getInstance().getWindow().getAppropriateLineWidth());
            }

            return new RopeRenderState(ownerPos, middle, knot, segments, outline);
        }

        return null;
    }

    /**
     * Submits a strand recorded by {@link #extract}
     *
     * @param ps the block entity pose (the same one upstream rendered with)
     */
    public static void submit(@Nullable final RopeRenderState state, final PoseStack ps, final SubmitNodeCollector queue) {
        if (state == null) {
            return;
        }

        queue.submitCustomGeometry(ps, RenderTypes.solidMovingBlock(), state);

        if (state.outline != null) {
            queue.submitCustomGeometry(ps, RenderTypes.lines(), state.outline);
        }
    }

    private static void renderOutline(final PoseStack.Pose poseEntry, final VertexConsumer linesVB, final float rad, final ObjectArrayList<RopeRenderPoint> ropeRenderPoints, final BlockPos ownerPos, final float lineWidth) {
        final Vector3d previousCorner = new Vector3d();
        final Vector3d currentCorner = new Vector3d();
        final Vector3d cornerDiff = new Vector3d();

        final Vector3d[] ropeCorners = {
                new Vector3d(-rad, 0, -rad),
                new Vector3d(-rad, 0, rad),
                new Vector3d(rad, 0, rad),
                new Vector3d(rad, 0, -rad),
        };

        final Matrix4f pose = poseEntry.pose();

        for (int i = 0; i < ropeRenderPoints.size() + 1; i++) {
            final RopeRenderPoint renderPoint0 = ropeRenderPoints.get(Math.max(0, i - 1));
            final RopeRenderPoint renderPoint1 = ropeRenderPoints.get(Math.min(ropeRenderPoints.size() - 1, i));
                final boolean start = i == 0;
                final boolean end = i == ropeRenderPoints.size();

            for (final Vector3d ropeCorner : ropeCorners) {
                renderPoint0.orientation().transform(start ? ropeCorner.rotateY(Math.PI / 2.0, previousCorner) : ropeCorner, previousCorner)
                        .add(renderPoint0.position())
                        .sub(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ());

                renderPoint1.orientation().transform(end ? ropeCorner.rotateY(Math.PI / 2.0, ropeCorner) : ropeCorner, currentCorner)
                        .add(renderPoint1.position())
                        .sub(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ());

                currentCorner.sub(previousCorner, cornerDiff).normalize();

                // 1.21.11 line vertices carry their width; upstream's RenderType.lines() used the window's default
                linesVB.addVertex(pose, (float) previousCorner.x, (float) previousCorner.y, (float) previousCorner.z)
                        .setColor(0f, 0f, 0f, .4f)
                        .setNormal(poseEntry, (float) cornerDiff.x, (float) cornerDiff.y, (float) cornerDiff.z)
                        .setLineWidth(lineWidth);

                linesVB.addVertex(pose, (float) currentCorner.x, (float) currentCorner.y, (float) currentCorner.z)
                        .setColor(0f, 0f, 0f, .4f)
                        .setNormal(poseEntry, (float) cornerDiff.x, (float) cornerDiff.y, (float) cornerDiff.z)
                        .setLineWidth(lineWidth);
            }
        }
    }

    private static @NotNull ObjectArrayList<RopeRenderPoint> buildRenderPoints(final float partialTick, final List<ClientRopePoint> inputPoints) {
        final ObjectArrayList<RopeRenderPoint> ropeRenderPoints = new ObjectArrayList<>();
        final ObjectArrayList<ClientRopePoint> points = new ObjectArrayList<>(inputPoints);

        while (points.size() >= 2 && points.getFirst().position().distanceSquared(points.get(1).position()) < 1e-3) {
            points.removeFirst();
        }

        if (points.size() <= 1) {
            return new ObjectArrayList<>();
        }

        final Vector3dc pointZeroPosition = points.get(0).renderPos(partialTick, new Vector3d());
        final Vector3dc pointOnePosition = points.get(1).renderPos(partialTick, new Vector3d());

        final Vector3d normal = pointOnePosition.sub(pointZeroPosition, new Vector3d()).normalize();

        final Quaternionf runningRotation;
        if (normal.dot(OrientedBoundingBox3d.UP) < 0) {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), normal);
            runningRotation.rotateZ((float) Math.PI);
        } else {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), normal);
        }

        ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), new Vector3d(pointZeroPosition)));

        final Vector3d runningNormal = new Vector3d();

        final Vector3d bPos = new Vector3d();
        final Vector3d aPos = new Vector3d();

        for (int i = 2; i < points.size(); i++) {
            final ClientRopePoint pointA = points.get(i - 1);
            final ClientRopePoint pointB = points.get(i);

            runningNormal.set(pointB.renderPos(partialTick, bPos))
                    .sub(pointA.renderPos(partialTick, aPos))
                    .normalize();

            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), runningNormal));
                runningRotation.rotateZ((float) Math.PI);
            } else {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), runningNormal));
            }

            ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), pointA.renderPos(partialTick, new Vector3d())));
            normal.set(runningNormal);
        }

        ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), points.getLast().renderPos(partialTick, new Vector3d())));
        return ropeRenderPoints;
    }
}
