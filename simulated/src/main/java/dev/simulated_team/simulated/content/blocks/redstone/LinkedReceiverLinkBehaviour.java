package dev.simulated_team.simulated.content.blocks.redstone;

import com.zurrtum.create.client.content.redstone.link.LinkBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.mixin.accessor.LinkBehaviourAccessor;

/**
 * Client half of the linked receivers' frequency slots (Create Fly splits {@code LinkBehaviour} into the server
 * {@code ServerLinkBehaviour} and this client behaviour, which owns the value box positions).
 */
public class LinkedReceiverLinkBehaviour extends LinkBehaviour {

    public LinkedReceiverLinkBehaviour(final SmartBlockEntity be) {
        super(be);
        final LinkBehaviourAccessor accessor = (LinkBehaviourAccessor) this;
        accessor.simulated$setFirstSlot(new LinkedReceiverFrequencySlot(true));
        accessor.simulated$setSecondSlot(new LinkedReceiverFrequencySlot(false));
    }
}
