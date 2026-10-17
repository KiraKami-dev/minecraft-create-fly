package dev.ryanhcode.sable.fabric.mixin.compatibility.create.render_fixes;


import com.zurrtum.create.client.catnip.placement.PlacementClient;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlacementClient.class)
public class PlacementClientMixin {

    @Redirect(method = "drawDirectionIndicator",
            at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"),
            remap = false)
    private static Vec3 sable$projectLastTargetedPos(final Vec3i pos) {
        return JOMLConversion.toMojang(Sable.HELPER.projectOutOfSubLevel(Minecraft.getInstance().level, JOMLConversion.atCenterOf(pos)));
    }
}
