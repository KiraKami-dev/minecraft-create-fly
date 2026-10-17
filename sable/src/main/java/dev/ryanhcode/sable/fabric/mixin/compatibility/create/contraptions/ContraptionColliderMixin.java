package dev.ryanhcode.sable.fabric.mixin.compatibility.create.contraptions;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.ContraptionCollider;
import com.zurrtum.create.foundation.collision.Matrix3d;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.contraptions.ContraptionColliderHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes contraptions on sub-levels collide with entities in the world (server half; Create Fly moved the client half
 * into {@code ContraptionColliderClient}, see {@link ContraptionColliderClientMixin}).
 */
@Mixin(ContraptionCollider.class)
public class ContraptionColliderMixin {

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;", remap = true), remap = false)
    private static AABB sable$contraptionBounds(final AbstractContraptionEntity instance, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.contraptionBounds(instance, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;", remap = true), remap = false)
    private static AABB sable$entityQueryBounds(final AABB instance, final double d, final double e, final double f, @Local(argsOnly = true) final AbstractContraptionEntity contraption, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.entityQueryBounds(instance, d, e, f, contraption, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;position()Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$contraptionPosition(final AbstractContraptionEntity instance, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.contraptionPosition(instance, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getPrevPositionVec()Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$getPrevPositionVec(final AbstractContraptionEntity instance, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.getPrevPositionVec(instance, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getAnchorVec()Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$getAnchorVec(final AbstractContraptionEntity instance, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.getAnchorVec(instance, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity$ContraptionRotationState;asMatrix()Lcom/zurrtum/create/foundation/collision/Matrix3d;"), remap = false)
    private static Matrix3d sable$rotationMatrix(final AbstractContraptionEntity.ContraptionRotationState rotationState, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.rotationMatrix(rotationState, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;toLocalVector(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$toLocalVector(final AbstractContraptionEntity instance, final Vec3 localVec, final float partialTicks, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.toLocalVector(instance, localVec, partialTicks, contraptionSubLevel);
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getContactPointMotion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$getContactPointMotion(final AbstractContraptionEntity instance, final Vec3 globalContactPoint, @Share("subLevel") final LocalRef<SubLevel> contraptionSubLevel) {
        return ContraptionColliderHelper.getContactPointMotion(instance, globalContactPoint, contraptionSubLevel);
    }
}
