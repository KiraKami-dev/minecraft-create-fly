package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Structure templates keep kinetic block entity data verbatim, including the absolute {@code Source} position and
 * network id of the world they were saved in. Placed elsewhere, the stale source is found missing, the block entity
 * drops to speed 0 and nothing re-propagates (upstream's GameTests only passed while that stale position sat in an
 * unloaded chunk). Treat placement like Create's schematic placement: {@code warnOfMovement()} makes the kinetic
 * {@code read} skip speed/source/network, so the placed machine propagates from its generators as if freshly built.
 * Extra-kinetics inner block entities get the same treatment.
 */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @Redirect(method = "placeInWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void simulated$resetKineticsOnPlace(final BlockEntity blockEntity, final ValueInput input) {
        if (blockEntity instanceof final KineticBlockEntity kbe) {
            kbe.warnOfMovement();
            if (blockEntity instanceof final ExtraKinetics ek && ek.getExtraKinetics() != null) {
                ek.getExtraKinetics().warnOfMovement();
            }
        }
        blockEntity.loadWithComponents(input);
    }
}
