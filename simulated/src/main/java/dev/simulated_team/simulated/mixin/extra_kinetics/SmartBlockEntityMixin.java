package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.zurrtum.create.api.registry.SimpleRegistry;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Extra-kinetics inner block entities share their parent's {@code BlockEntityType}; Create Fly's per-type behaviour
 * registries ({@code FIRST_READ_REGISTRY}, {@code CLIENT_REGISTRY}) would apply the parent's behaviour factories to
 * them (ClassCastException). Inner BEs get no registry behaviours, as upstream.
 */
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityMixin {

    @Redirect(method = "initialize", at = @At(value = "INVOKE",
            target = "Lcom/zurrtum/create/api/registry/SimpleRegistry$Multi;get(Ljava/lang/Object;)Ljava/util/List;"),
            require = 2)
    private List<?> simulated$skipRegistryBehavioursForExtraKinetics(final SimpleRegistry.Multi<Object, ?> registry, final Object type) {
        if (this instanceof ExtraKinetics.ExtraKineticsBlockEntity) {
            return List.of();
        }
        return registry.get(type);
    }
}
