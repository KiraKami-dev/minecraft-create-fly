package dev.ryanhcode.sable.fabric.mixin.compatibility.create.super_glue;

import com.zurrtum.create.client.content.contraptions.glue.SuperGlueSelectionHandler;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TODO: Instead of projecting the BB outwards, project the ray inwards
 */
@Mixin(SuperGlueSelectionHandler.class)
public class SuperGlueSelectionHandlerMixin {

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/contraptions/glue/SuperGlueEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;", ordinal = 0, remap = true),
            remap = false)
    private AABB sable$projectBoundingBox(final SuperGlueEntity instance) {
        final SubLevel subLevel = Sable.HELPER.getContaining(instance);

        if (subLevel != null) {
            final BoundingBox3d bb = new BoundingBox3d(instance.getBoundingBox());
            return bb.transform(subLevel.logicalPose(), bb).toMojang();
        }

        return instance.getBoundingBox();
    }
}
