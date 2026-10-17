package dev.ryanhcode.sable.fabric.mixin.compatibility.create.render_fixes;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.outliner.LineOutline;
import com.zurrtum.create.client.catnip.outliner.Outline;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LineOutline.class)
public abstract class LineOutlineMixin extends Outline {

    @Override
    public void bufferCuboidLine(final PoseStack poseStack, final VertexConsumer consumer, final Vec3 camera, final Vector3d lineStart, final Vector3d lineEnd,
                                 final float width, final Vector4f color, final int lightmap, final boolean disableNormals) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final ClientSubLevel startSubLevel = helper.getContainingClient(lineStart);
        final ClientSubLevel endSubLevel = helper.getContainingClient(lineEnd);

        // Transform copies, the passed vectors are the outline's own (plot space) end points
        final Vector3d start = new Vector3d(lineStart);
        final Vector3d end = new Vector3d(lineEnd);
        if (startSubLevel != null) startSubLevel.renderPose().transformPosition(start);
        if (endSubLevel != null) endSubLevel.renderPose().transformPosition(end);

        final Vector3f diff = this.diffPosTemp;
        diff.set((float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z));

        final float length = Mth.sqrt(diff.x() * diff.x() + diff.y() * diff.y() + diff.z() * diff.z());
        final float hAngle = AngleHelper.deg(Mth.atan2(diff.x(), diff.z()));
        final float hDistance = Mth.sqrt(diff.x() * diff.x() + diff.z() * diff.z());
        final float vAngle = AngleHelper.deg(Mth.atan2(hDistance, diff.y())) - 90;

        poseStack.pushPose();
        TransformStack.of(poseStack)
                .translate(start.x - camera.x, start.y - camera.y, start.z - camera.z)
                .rotateYDegrees(hAngle)
                .rotateXDegrees(vAngle);
        this.bufferCuboidLine(poseStack.last(), consumer, new Vector3f(), Direction.SOUTH, length, width, color, lightmap,
                disableNormals);
        poseStack.popPose();
    }

}
