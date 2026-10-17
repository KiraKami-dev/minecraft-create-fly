package dev.eriksonn.aeronautics.content.blocks.levitite;

import dev.eriksonn.aeronautics.content.blocks.levitite.render.LevititeRenderer;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.SableMathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tracks the smoothed motion of sub-levels and computes the levitite effect parameters
 * ({@link LevititeRenderer.LevititeUniform}). Upstream set them as uniforms of the Veil levitite shader.
 */
public class LevititeShaderManager {
    private static final double SMOOTHING_SPEED = 0.5;

    private static final Vector3d linearVelocity = new Vector3d();
    private static final Vector3d angularVelocity = new Vector3d();
    private static final Vector3d temp = new Vector3d();
    private static final Vector3d currentPos = new Vector3d();
    private static final Quaterniond currentOrientation = new Quaterniond();
    private static final Vector3d offset = new Vector3d();
    private static final Matrix3f matrix = new Matrix3f();
    private static final Vector3f gravityVector2 = new Vector3f();
    private static boolean enabled = false;

    public static HashMap<ClientSubLevel, LevititeShaderManager> managers = new HashMap<>();

    private final Vector3d smoothedLinearVelocity = new Vector3d();
    private final Vector3d lastSmoothedLinearVelocity = new Vector3d();
    private final Vector3d smoothedAngularVelocity = new Vector3d();
    private final Vector3d lastSmoothedAngularVelocity = new Vector3d();
    private final Vector3d accumulatedPosition = new Vector3d();
    // upstream kept this in a static field shared by all sub-levels, overwritten by every tick and frame
    private final Vector3d gravityVector1 = new Vector3d();

    public static void tick() {
        if (managers.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<ClientSubLevel, LevititeShaderManager>> iterator = managers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ClientSubLevel, LevititeShaderManager> entry = iterator.next();

            ClientSubLevel subLevel = entry.getKey();
            if (subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            entry.getValue().internalTick(subLevel);
        }
    }

    public static LevititeShaderManager getInstance(ClientSubLevel subLevel) {
        managers.putIfAbsent(subLevel, new LevititeShaderManager());
        return managers.get(subLevel);
    }

    /**
     * Upstream disabled the shader while ponder scenes rendered; levitite effects are only drawn for sub-levels by the
     * {@link LevititeRenderer} now, which ponder never reaches. Kept for API compatibility.
     */
    public static void disableShader() {
        enabled = false;
    }

    public static void enableShader() {
        enabled = true;
    }

    private static Matrix3f getGravityMatrix(final Vector3f g, final float verticalDrag, final float horizontalDrag, Matrix3f target) {
        if (g.lengthSquared() > 0.00001) {
            float scale = (horizontalDrag - verticalDrag) / g.dot(g);
            target.m00 = g.x() * g.x() * scale;
            target.m01 = g.y() * g.x() * scale;
            target.m02 = g.z() * g.x() * scale;
            target.m10 = g.x() * g.y() * scale;
            target.m11 = g.y() * g.y() * scale;
            target.m12 = g.z() * g.y() * scale;
            target.m20 = g.x() * g.z() * scale;
            target.m21 = g.y() * g.z() * scale;
            target.m22 = g.z() * g.z() * scale;
        } else
            target.identity();
        target.m00 -= horizontalDrag;
        target.m11 -= horizontalDrag;
        target.m22 -= horizontalDrag;
        return target;
    }

    void internalTick(ClientSubLevel subLevel) {
        lastSmoothedLinearVelocity.set(smoothedLinearVelocity);
        lastSmoothedAngularVelocity.set(smoothedAngularVelocity);

        subLevel.logicalPose().position().sub(subLevel.lastPose().position(), linearVelocity);
        subLevel.logicalPose().rotationPoint().sub(subLevel.lastPose().rotationPoint(), temp);
        DimensionPhysicsData.getGravity(subLevel.getLevel(), subLevel.logicalPose().position(), gravityVector1);
        subLevel.logicalPose().orientation().transform(temp);
        linearVelocity.sub(temp);
        SableMathUtils.getAngularVelocity(subLevel.lastPose().orientation(), subLevel.logicalPose().orientation(), angularVelocity);

        smoothedLinearVelocity.lerp(linearVelocity, SMOOTHING_SPEED);
        smoothedAngularVelocity.lerp(angularVelocity, SMOOTHING_SPEED);
        accumulatedPosition.add(smoothedLinearVelocity);
        accumulatedPosition.set(
                (accumulatedPosition.x % 10000),
                (accumulatedPosition.y % 10000),
                (accumulatedPosition.z % 10000));

    }

    public boolean needsLayers() {
        return (smoothedAngularVelocity.lengthSquared() > 1E-6 || smoothedLinearVelocity.lengthSquared() > 1E-6) && gravityVector1.lengthSquared() > 0.001;
    }

    /**
     * Computes the effect parameters of a sub-level (upstream {@code prepareShaderForSublevel} + {@code setMaterialProperties}).
     *
     * @param subLevel    The sub-level
     * @param anchor      The anchor block its sections are drawn relative to
     * @param layerIndex  0 for the block itself, 1 / -1 for the ghost layers
     * @param spriteRects The block atlas rectangles of the levitite sprites
     * @param partialTick The partial tick
     */
    public LevititeRenderer.LevititeUniform createUniform(final ClientSubLevel subLevel, final BlockPos anchor, final int layerIndex, final Vector4fc[] spriteRects, final float partialTick) {
        final Pose3dc currentPose = subLevel.renderPose(partialTick);
        currentPos.set(currentPose.position());
        currentOrientation.set(currentPose.orientation());

        lastSmoothedLinearVelocity.lerp(smoothedLinearVelocity, partialTick, linearVelocity);
        lastSmoothedAngularVelocity.lerp(smoothedAngularVelocity, partialTick, angularVelocity);

        currentOrientation.transformInverse(linearVelocity);
        currentOrientation.transformInverse(angularVelocity);
        currentOrientation.transformInverse(gravityVector1, temp);
        gravityVector2.set(temp);

        final Vector3dc rotationPoint = currentPose.rotationPoint();
        offset.set(anchor.getX() - rotationPoint.x(), anchor.getY() - rotationPoint.y(), anchor.getZ() - rotationPoint.z());

        final Matrix4f slow = new Matrix4f();
        final Matrix4f fast = new Matrix4f();
        float transitionSpeed = 1.0f;
        final FloatingBlockMaterial material = PhysicsBlockPropertyHelper.getFloatingMaterial(AeroBlocks.LEVITITE.defaultBlockState());
        if (material != null) {
            transitionSpeed = (float) material.transitionSpeed();
            slow.set(getGravityMatrix(gravityVector2, (float) material.slowVerticalFriction(), (float) material.slowHorizontalFriction(), matrix));
            fast.set(getGravityMatrix(gravityVector2, (float) material.fastVerticalFriction(), (float) material.fastHorizontalFriction(), matrix));
        }

        long ticks = subLevel.getLevel().getGameTime() % 100000;
        final float time = ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        return new LevititeRenderer.LevititeUniform(
                new Matrix4f().rotation(currentOrientation.get(new org.joml.Quaternionf())),
                slow,
                fast,
                new Vector4f((float) offset.x, (float) offset.y, (float) offset.z, 0.0f),
                new Vector4f((float) linearVelocity.x * 20, (float) linearVelocity.y * 20, (float) linearVelocity.z * 20, (float) gravityVector1.length()),
                new Vector4f((float) angularVelocity.x * 20, (float) angularVelocity.y * 20, (float) angularVelocity.z * 20, transitionSpeed),
                new Vector4f((float) (currentPos.x % 10000), (float) (currentPos.y % 10000), (float) (currentPos.z % 10000), time),
                new Vector4f(layerIndex, 1.0f, 0.0f, 0.0f),
                spriteRects
        );
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
