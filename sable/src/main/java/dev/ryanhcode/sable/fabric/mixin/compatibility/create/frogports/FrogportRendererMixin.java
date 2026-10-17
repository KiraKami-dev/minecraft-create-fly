package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.logistics.packagePort.frogport.FrogportRenderer;
import com.zurrtum.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.zurrtum.create.content.logistics.packagePort.PackagePortTarget;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.frogports.FrogportMixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes frogports face & extend the proper amount when picking up and depositing cross-sub-level
 * <br>
 * Create Fly computes the frogport pose while extracting the render state (upstream: {@code renderSafe}).
 */
@Mixin(FrogportRenderer.class)
public class FrogportRendererMixin {

    @WrapOperation(method = "extractRenderState(Lcom/zurrtum/create/content/logistics/packagePort/frogport/FrogportBlockEntity;Lcom/zurrtum/create/client/content/logistics/packagePort/frogport/FrogportRenderer$FrogportRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/logistics/packagePort/PackagePortTarget;getExactTargetLocation(Lcom/zurrtum/create/content/logistics/packagePort/PackagePortBlockEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 sable$getExactTargetLocation(final PackagePortTarget instance,
                                             final PackagePortBlockEntity packagePortBlockEntity,
                                             final LevelAccessor levelAccessor,
                                             final BlockPos blockPos,
                                             final Operation<Vec3> original) {
        return FrogportMixinHelper.getExactTargetLocation(instance, packagePortBlockEntity, levelAccessor, blockPos, original);
    }

}
