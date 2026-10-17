package dev.ryanhcode.sable.fabric.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.outliner.AABBOutline;
import com.zurrtum.create.client.catnip.outliner.ChasingAABBOutline;
import com.zurrtum.create.client.catnip.render.SuperRenderTypeBuffer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChasingAABBOutline.class)
public abstract class ChasingAABBOutlineMixin extends AABBOutline {

    public ChasingAABBOutlineMixin(final AABB bb) {
        super(bb);
    }

    @Inject(method = "render", at = @At(value = "HEAD"), remap = false)
    private void sable$pushPose(final Minecraft mc, final PoseStack ms, final SuperRenderTypeBuffer buffer, final Vec3 camera, final float pt, final CallbackInfo ci){
        ms.pushPose();
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(this.bb.getCenter()), ms);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/catnip/outliner/ChasingAABBOutline;renderBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/zurrtum/create/client/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lorg/joml/Vector4f;IZ)V"), remap = false)
    private AABB sable$moveBB(final AABB box) {
        return box.move(SublevelRenderOffsetHelper.translation(box.getCenter()).scale(-1.0));
    }

    @Inject(method = "render", at = @At(value = "RETURN"), remap = false)
    private void sable$popPose(final Minecraft mc, final PoseStack ms, final SuperRenderTypeBuffer buffer, final Vec3 camera, final float pt, final CallbackInfo ci){
        ms.popPose();
    }
}
