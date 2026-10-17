package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChainConveyorInteractionHandler.class)
public class ChainConveyorInteractionHandlerMixin {

    @Shadow(remap = false)
    public static BlockPos selectedLift;

    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", remap = true), remap = false)
    private static double sable$addParticleInternal(final Vec3 instance, final Vec3 vec3) {
        return Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, vec3);
    }

    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", ordinal = 0, remap = true), remap = false)
    private static Vec3 sable$fromSubLiftVec(final Vec3 from, final Vec3 liftVec) {
        final SubLevel subLevel = Sable.HELPER.getContainingClient(liftVec);

        if (subLevel != null) {
            return subLevel.logicalPose().transformPositionInverse(from).subtract(liftVec);
        }

        return from.subtract(liftVec);
    }

    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", ordinal = 1, remap = true), remap = false)
    private static Vec3 sable$toSubLiftVec(final Vec3 to, final Vec3 liftVec) {
        final SubLevel subLevel = Sable.HELPER.getContainingClient(liftVec);

        if (subLevel != null) {
            return subLevel.logicalPose().transformPositionInverse(to).subtract(liftVec);
        }

        return to.subtract(liftVec);
    }

    /**
     * Take sub-levels into account when drawing the selected chain outline.
     * <br>
     * Upstream overwrote {@code drawCustomBlockSelection}; redirecting its only translation is equivalent.
     */
    @Redirect(method = "drawCustomBlockSelection", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", remap = true), remap = false)
    private static void sable$translateFactoringSubLevels(final PoseStack ms, final double x, final double y, final double z, @Local(argsOnly = true) final Vec3 camera) {
        Vec3 pos = Vec3.atLowerCornerOf(selectedLift);

        final SubLevel subLevel = Sable.HELPER.getContainingClient(pos);

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            final Pose3dc renderPose = clientSubLevel.renderPose();
            pos = renderPose.transformPosition(pos);
            ms.translate(pos.x() - camera.x, pos.y() - camera.y, pos.z() - camera.z);
            ms.mulPose(new Quaternionf(renderPose.orientation()));
        } else {
            ms.translate(x, y, z);
        }
    }

}
