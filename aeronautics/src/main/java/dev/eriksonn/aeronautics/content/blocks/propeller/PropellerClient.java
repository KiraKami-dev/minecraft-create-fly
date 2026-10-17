package dev.eriksonn.aeronautics.content.blocks.propeller;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingClient;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerTooltipBehaviours.SmallPropellerTooltipBehaviour;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;

/**
 * Client behaviours of every propeller block entity type (value boxes and goggle tooltips that upstream lived on the
 * block entities).
 */
public final class PropellerClient {
    private PropellerClient() {
    }

    /**
     * The tooltip behaviours replace the default kinetic tooltip behaviour (same behaviour type), so this must run after
     * the default kinetic behaviours of the propeller block entity types are registered.
     */
    public static void registerClient() {
        PropellerBearingClient.registerClient();

        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.ANDESITE_PROPELLER, SmallPropellerTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.WOODEN_PROPELLER, SmallPropellerTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.SMART_PROPELLER, SmallPropellerTooltipBehaviour::new);
    }
}
