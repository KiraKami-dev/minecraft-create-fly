package dev.ryanhcode.sable.debug;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps a debug gizmo so primitives emitted inside a sub-level plot are moved to where the sub-level is rendered.
 * <p>
 * Debug renderers emit gizmos at block positions, which for blocks of sub-levels are in plot space. Each primitive is
 * assigned to the sub-level containing its center, and all of its points are transformed by that sub-level's render pose.
 */
@ApiStatus.Internal
public record PlotSpaceGizmo(Gizmo delegate) implements Gizmo {

    @Override
    public void emit(final GizmoPrimitives primitives, final float alpha) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.delegate.emit(primitives, alpha);
            return;
        }

        this.delegate.emit(new Transforming(primitives, level), alpha);
    }

    private record Transforming(GizmoPrimitives delegate, ClientLevel level) implements GizmoPrimitives {

        private @Nullable ClientSubLevel subLevelAt(final Vec3 center) {
            return (ClientSubLevel) Sable.HELPER.getContaining(this.level, center);
        }

        private static Vec3 transform(@Nullable final ClientSubLevel subLevel, final Vec3 pos) {
            return subLevel != null ? subLevel.renderPose().transformPosition(pos) : pos;
        }

        @Override
        public void addPoint(final Vec3 pos, final int color, final float size) {
            this.delegate.addPoint(transform(this.subLevelAt(pos), pos), color, size);
        }

        @Override
        public void addLine(final Vec3 a, final Vec3 b, final int color, final float width) {
            final ClientSubLevel subLevel = this.subLevelAt(a.add(b).scale(0.5));
            this.delegate.addLine(transform(subLevel, a), transform(subLevel, b), color, width);
        }

        @Override
        public void addTriangleFan(final Vec3[] points, final int color) {
            if (points.length == 0) {
                this.delegate.addTriangleFan(points, color);
                return;
            }

            Vec3 center = Vec3.ZERO;
            for (final Vec3 point : points) {
                center = center.add(point);
            }

            final ClientSubLevel subLevel = this.subLevelAt(center.scale(1.0 / points.length));
            final Vec3[] transformed = new Vec3[points.length];
            for (int i = 0; i < points.length; i++) {
                transformed[i] = transform(subLevel, points[i]);
            }
            this.delegate.addTriangleFan(transformed, color);
        }

        @Override
        public void addQuad(final Vec3 a, final Vec3 b, final Vec3 c, final Vec3 d, final int color) {
            final ClientSubLevel subLevel = this.subLevelAt(a.add(b).add(c).add(d).scale(0.25));
            this.delegate.addQuad(transform(subLevel, a), transform(subLevel, b), transform(subLevel, c), transform(subLevel, d), color);
        }

        @Override
        public void addText(final Vec3 pos, final String text, final TextGizmo.Style style) {
            this.delegate.addText(transform(this.subLevelAt(pos), pos), text, style);
        }
    }
}
