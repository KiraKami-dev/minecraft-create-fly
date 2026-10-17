package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;

/**
 * The gyroscopic bearing's goggle line ("can lift", upstream {@code additionalTooltipInfo}) is added client-side by
 * {@code PropellerTooltipBehaviours}.
 */
public class GyroActorBehaviour<T extends GyroscopicPropellerBearingBlockEntity> extends PropellerActorBehaviour {

    public GyroActorBehaviour(final T be) {
        super(be, be);
    }
}
