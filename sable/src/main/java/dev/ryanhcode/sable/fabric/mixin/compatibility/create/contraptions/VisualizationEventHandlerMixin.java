package dev.ryanhcode.sable.fabric.mixin.compatibility.create.contraptions;

import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationEventHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VisualizationEventHandler.class)
public class VisualizationEventHandlerMixin {

    @Inject(method = "onEntityJoinLevel", at = @At("TAIL"), remap = false)
    private static void sable$onEntityJoinLevel(final Level level, final Entity entity, final CallbackInfo ci) {
        final SubLevel subLevel = Sable.HELPER.getContaining(entity);

        if (subLevel != null) {
            FlywheelCompat.createRenderInfo(level, subLevel);
        }
    }
}
