package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.LivingEntityStickExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PORT(1.21.11): replaces the 1.21.1 EntityRenderDispatcherMixin#renderHitbox injection. Hitboxes are now drawn
 * as world-space gizmos by {@link EntityHitboxDebugRenderer}, so the oriented collision box is emitted as lines.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @Inject(method = "showHitboxes", at = @At("TAIL"))
    private void sable$showHitboxes(final Entity entity, final float partialTicks, final boolean serverEntity, final CallbackInfo ci) {
        // collision hitbox
        final SubLevel tracking = Sable.HELPER.getTrackingSubLevel(entity);
        if (tracking instanceof final ClientSubLevel clientSubLevel) {
            Quaterniondc customOrientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, partialTicks);
            if (customOrientation == null)
                customOrientation = JOMLConversion.QUAT_IDENTITY;

            final double yaw = SubLevelEntityCollision.getHitBoxYaw(clientSubLevel.renderPose());
            final Quaternionf rotation = new Quaternionf(customOrientation).rotateY((float) yaw);
            final AABB bounds = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
            sable$orientedBox(entity.getPosition(partialTicks), entity.getEyeHeight(), rotation, bounds, ARGB.colorFromFloat(0.4F, 1.0F, 1.0F, 0.0F));
        }

        final EntityStickExtension duck = (EntityStickExtension) entity;
        final Vec3 plotPosition = duck.sable$getPlotPosition();

        if (plotPosition != null) {
            final ClientSubLevel subLevel = (ClientSubLevel) Sable.HELPER.getContaining(entity.level(), plotPosition);

            if (subLevel != null) {
                final Vec3 projectedPos = subLevel.renderPose().transformPosition(plotPosition);
                Gizmos.cuboid(entity.getType().getSpawnAABB(projectedPos.x, projectedPos.y, projectedPos.z), GizmoStyle.stroke(ARGB.colorFromFloat(0.2F, 0.0F, 1.0F, 0.0F)));

                if (entity instanceof final LivingEntityStickExtension livingDuck) {
                    final Vec3 serverProjectedPos = subLevel.renderPose().transformPosition(livingDuck.sable$getLerpTarget());
                    Gizmos.cuboid(entity.getType().getSpawnAABB(serverProjectedPos.x, serverProjectedPos.y, serverProjectedPos.z), GizmoStyle.stroke(ARGB.colorFromFloat(0.2F, 1.0F, 0.0F, 1.0F)));
                }
            }
        }
    }

    /**
     * Draws a box (relative to the entity origin), rotated around the entity eye height
     */
    @Unique
    private static void sable$orientedBox(final Vec3 origin, final float eyeHeight, final Quaternionf rotation, final AABB bounds, final int color) {
        final Vec3[] corners = new Vec3[8];
        final Vector3f temp = new Vector3f();
        for (int i = 0; i < 8; i++) {
            final double x = (i & 1) == 0 ? bounds.minX : bounds.maxX;
            final double y = (i & 2) == 0 ? bounds.minY : bounds.maxY;
            final double z = (i & 4) == 0 ? bounds.minZ : bounds.maxZ;
            temp.set((float) x, (float) (y - eyeHeight), (float) z).rotate(rotation);
            corners[i] = origin.add(temp.x, temp.y + eyeHeight, temp.z);
        }

        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                if ((i & bit) == 0) {
                    Gizmos.line(corners[i], corners[i | bit], color);
                }
            }
        }
    }
}
