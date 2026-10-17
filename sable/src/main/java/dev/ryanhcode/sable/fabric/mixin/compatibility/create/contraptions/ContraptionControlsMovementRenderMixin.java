package dev.ryanhcode.sable.fabric.mixin.compatibility.create.contraptions;

import com.zurrtum.create.client.content.contraptions.actors.contraptionControls.ContraptionControlsMovementRender;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the rendering for the Contraption Controls to take sublevels into account.
 * <p>
 * Create Fly moved Create's {@code ContraptionControlsRenderer#renderInContraption} into
 * {@link ContraptionControlsMovementRender#getRenderState}, which compares the (world-space) camera position with the
 * actor position to decide whether to show the floor text. The actor position is in plot space on a sub-level, so it
 * is projected out first. (Upstream also swapped {@code ContraptionMatrices#getViewProjection} for the model one; Create
 * Fly renders the text with the actor's own model pose, so that part is not needed.)
 */
@Mixin(ContraptionControlsMovementRender.class)
public class ContraptionControlsMovementRenderMixin {
    @Redirect(method = "getRenderState",
            at = @At(value = "FIELD", target = "Lcom/zurrtum/create/content/contraptions/behaviour/MovementContext;position:Lnet/minecraft/world/phys/Vec3;", opcode = Opcodes.GETFIELD),
            remap = false)
    private Vec3 sable$distanceRemix(final MovementContext instance) {
        if (instance.position == null) {
            return null;
        }

        return Sable.HELPER.projectOutOfSubLevel(instance.world, instance.position);
    }
}
