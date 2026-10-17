package dev.simulated_team.simulated.mixin.accessor;

import com.zurrtum.create.client.content.redstone.link.LinkBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Create Fly's client {@link LinkBehaviour} hard-codes the redstone link frequency slots;
 * the linked receivers swap in their own slots through this accessor.
 */
@Mixin(LinkBehaviour.class)
public interface LinkBehaviourAccessor {

    @Accessor(value = "firstSlot", remap = false)
    void simulated$setFirstSlot(ValueBoxTransform slot);

    @Accessor(value = "secondSlot", remap = false)
    void simulated$setSecondSlot(ValueBoxTransform slot);

}
