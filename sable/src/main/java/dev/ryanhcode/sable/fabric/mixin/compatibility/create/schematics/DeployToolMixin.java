package dev.ryanhcode.sable.fabric.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.schematics.client.tools.DeployTool;
import com.zurrtum.create.client.content.schematics.client.tools.SchematicToolBase;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.renderers.AABBOutlineRenderingOptions;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeployTool.class)
public abstract class DeployToolMixin extends SchematicToolBase {

    @WrapOperation(method = "renderTool", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", remap = true), remap = false)
    public void sable$manualTransformBB(final PoseStack instance, final double x, final double y, final double z, final Operation<Void> original) {
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(this.selectedPos), instance);
        final Vec3 trans = SublevelRenderOffsetHelper.translation(this.selectedPos.getCenter());

        original.call(instance, x - trans.x, y - trans.y, z - trans.z);
        // implemented by render_fixes.AABBOutlineMixin
        if (this.schematicHandler.getOutline() instanceof final AABBOutlineRenderingOptions options) {
            options.sable$shouldTransform(false);
        }
    }
}
