package dev.ryanhcode.sable.debug;

import com.mojang.blaze3d.platform.Window;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import java.lang.Math;


/**
 * Handler for debug client gizmos
 */
public class SableClientGizmoHandler {

    private Vec3 mouseDir = Vec3.ZERO;
    private boolean enabled = false;
    private @Nullable GizmoSelection selection;

    public void init() {
        // Gizmos are emitted from DebugRendererMixin, together with the vanilla debug gizmos
    }

    public static Vec3 getRay(final Matrix4fc projectionMatrix, final float normalizedMouseX, final float normalizedMouseY) {
        final Vector4f clipCoords = new Vector4f(-normalizedMouseX, -normalizedMouseY, -1.0F, 0.0F);
        final Vector4f eyeSpace = toEyeCoords(projectionMatrix, clipCoords);
        return new Vec3(eyeSpace.x, eyeSpace.y, eyeSpace.z).normalize();
    }

    private static Vector4f toEyeCoords(final Matrix4fc projectionMatrix, final Vector4fc clipCoords) {
        final Matrix4f inverse = (projectionMatrix).invert(new Matrix4f());
        final Vector4f result = new Vector4f(clipCoords.x(), clipCoords.y(), clipCoords.z(), clipCoords.w());
        result.mul(inverse);
        result.set(result.x(), result.y(), 1.0F, 0.0F);
        return result;
    }

    /**
     * Gets the current mouse hover selection
     * @return the current gizmo selection, or null if none is found
     */
    public @Nullable GizmoSelection getSelection() {
        return this.selection;
    }


    /**
     * Emits the gizmos of all sub-levels. Called while the level renderer emits its debug gizmos.
     *
     * @param partialTicks The partial tick
     */
    public void emitGizmos(final float partialTicks) {
        if (!this.enabled) return;

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        this.updateMouseDir(minecraft, partialTicks);
        this.updateSelection();

        for (final SubLevel subLevel : container.getAllSubLevels()) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;

            final Pose3dc renderPose = clientSubLevel.renderPose();
            final Vector3dc renderPos = renderPose.position();

            Gizmos.cuboid(new AABB(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).inflate(0.1d).move(renderPos.x(), renderPos.y(), renderPos.z()), GizmoStyle.fill(ARGB.colorFromFloat(0.4f, 1.0f, 1.0f, 1.0f)));

            for (final Direction.Axis axis : Direction.Axis.VALUES) {
                final Direction dir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
                final Vec3i normal = dir.getUnitVec3i();

                float r = (float) (Math.max(normal.getX(), 0.2) * 0.8);
                float g = (float) (Math.max(normal.getY(), 0.2) * 0.8);
                float b = (float) (Math.max(normal.getZ(), 0.2) * 0.8);

                final Vec3 normalD = new Vec3(normal.getX(), normal.getY(), normal.getZ());
                final Vec3 expandDir = normalD
                        .scale(2.0f);

                final float inflation = 0.04f;
                final AABB bb = new AABB(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).inflate(inflation).move(normalD.scale(0.125)).expandTowards(expandDir);

                if (this.selection != null && this.selection.subLevel().equals(clientSubLevel.getUniqueId()) && this.selection.axis() == axis) {
                    r = Math.min(r * 1.2f, 1.0f);
                    g = Math.min(g * 1.2f, 1.0f);
                    b = Math.min(b * 1.2f, 1.0f);
                }

                Gizmos.cuboid(bb.move(renderPos.x(), renderPos.y(), renderPos.z()), GizmoStyle.fill(ARGB.colorFromFloat(0.9f, r, g, b)));
            }
        }
    }

    private void updateSelection() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        for (final SubLevel subLevel : container.getAllSubLevels()) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;

            final Pose3dc renderPose = clientSubLevel.renderPose();
            final Vector3d renderPos = renderPose.position().sub(cameraPos.x, cameraPos.y, cameraPos.z, new Vector3d());

            for (final Direction.Axis axis : Direction.Axis.VALUES) {
                final Direction dir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
                final Vec3i normal = dir.getUnitVec3i();

                final Vec3 normalD = new Vec3(normal.getX(), normal.getY(), normal.getZ());
                final Vec3 expandDir = normalD
                        .scale(2.0f);

                final float inflation = 0.04f;
                final AABB bb = new AABB(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).inflate(inflation).move(normalD.scale(0.125)).expandTowards(expandDir);

                if (bb.move(renderPos.x, renderPos.y, renderPos.z).inflate(0.1f).clip(Vec3.ZERO, this.mouseDir.scale(100.0)).isPresent()) {
                    this.selection = new GizmoSelection(clientSubLevel.getUniqueId(), axis);
                    return;
                }
            }
        }

        // No selection found
        this.selection = null;
    }

    private void updateMouseDir(final Minecraft minecraft, final float partialTicks) {
        final LocalPlayer player = minecraft.player;

        final Window window = minecraft.getWindow();
        final MouseHandler mouseHandler = minecraft.mouseHandler;

        final double xPos = mouseHandler.xpos() / (double) window.getScreenWidth() * 2.0 - 1.0;
        final double yPos = mouseHandler.ypos() / (double) window.getScreenHeight() * 2.0 - 1.0;

        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final float fov = gameRenderer.getFov(gameRenderer.getMainCamera(), partialTicks, true);
        final Matrix4f proj = gameRenderer.getProjectionMatrix(fov);

        final float yaw = player.getViewYRot(partialTicks);
        final float pitch = player.getViewXRot(partialTicks);

        this.mouseDir = getRay(proj, (float) xPos, (float) yPos).xRot((float) -Math.toRadians(pitch)).yRot((float) -Math.toRadians(yaw));
    }

    public void start() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GizmoScreen());
        this.enabled = true;
    }

    public void stop() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GizmoScreen) {
            minecraft.setScreen(null);
        }
        this.enabled = false;
    }

    public Vec3 getMouseDir() {
        return this.mouseDir;
    }
}
