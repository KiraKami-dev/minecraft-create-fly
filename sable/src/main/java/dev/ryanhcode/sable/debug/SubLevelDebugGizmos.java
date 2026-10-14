package dev.ryanhcode.sable.debug;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import dev.ryanhcode.sable.network.client.ClientSableInterpolationState;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Debug gizmos for sub-levels, shown together with entity hitboxes (F3+B).
 */
@ApiStatus.Internal
public final class SubLevelDebugGizmos {

    private SubLevelDebugGizmos() {
    }

    public static void emit() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(minecraft.level);
        if (container == null) {
            return;
        }

        for (final SubLevel subLevel : container.getAllSubLevels()) {
            final BoundingBox3dc bounds = subLevel.boundingBox();

            Gizmos.cuboid(new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()), GizmoStyle.stroke(ARGB.colorFromFloat(0.7f, 0.5f, 0.5f, 0.5f)));

            final Pose3dc renderPose = ((ClientSubLevel) subLevel).renderPose();
            final BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
            final Vector3dc localCenter = renderPose.rotationPoint();

            final double centerSize = 2.0 / 16.0;
            orientedBox(renderPose,
                    localCenter.x() - centerSize, localCenter.y() - centerSize, localCenter.z() - centerSize,
                    localCenter.x() + centerSize, localCenter.y() + centerSize, localCenter.z() + centerSize,
                    ARGB.colorFromFloat(1.0f, 0.7f, 0.7f, 0.5f));

            if (plotBounds != null) {
                orientedBox(renderPose,
                        plotBounds.minX(), plotBounds.minY(), plotBounds.minZ(),
                        plotBounds.maxX() + 1.0, plotBounds.maxY() + 1.0, plotBounds.maxZ() + 1.0,
                        ARGB.colorFromFloat(1.0f, 0.9f, 0.5f, 0.5f));
            }

            if (ClientSableInterpolationState.RENDER_INTERPOLATION_BOUNDS) {
                final Vector3d boundSize = bounds.size(new Vector3d());
                final SubLevelSnapshotInterpolator interpolator = ((ClientSubLevel) subLevel).getInterpolator();
                for (final SubLevelSnapshotInterpolator.Snapshot buffer : interpolator.buffer) {
                    final Vector3dc position = buffer.pose().position();

                    Gizmos.cuboid(new AABB(
                            position.x() - boundSize.x() / 2.0,
                            position.y() - boundSize.y() / 2.0,
                            position.z() - boundSize.z() / 2.0,
                            position.x() + boundSize.x() / 2.0,
                            position.y() + boundSize.y() / 2.0,
                            position.z() + boundSize.z() / 2.0
                    ), GizmoStyle.stroke(ARGB.colorFromFloat(0.5f, 0.0f, 1.0f, 1.0f)));
                }
            }
        }
    }

    /**
     * Emits the outline of a box in plot space, transformed by a sub-level pose.
     */
    public static void orientedBox(final Pose3dc pose, final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final int color) {
        final Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = pose.transformPosition(new Vec3((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ));
        }

        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                if ((i & bit) == 0) {
                    Gizmos.line(corners[i], corners[i | bit], color, 1.0f);
                }
            }
        }
    }
}
