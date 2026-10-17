package dev.simulated_team.simulated.content.blocks.redstone;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.AllBlockEntityBehaviours;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorMovementRenderBehaviour;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorValueBehaviour;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorFilterBehaviour;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverGuiRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorValueBehaviour;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorValueBehaviour;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimMovementBehaviours;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;

/**
 * Client registration for the redstone and sensor blocks. Create Fly moved value boxes, filter/frequency slots and
 * goggle tooltips out of the block entities into {@link BlockEntityBehaviour#CLIENT_REGISTRY}; the block entities add
 * only the server halves. Also attaches the altitude sensor's contraption render and the modulating receiver's GUI
 * renderer (a picture-in-picture renderer, so it must be registered before the GUI renderer is created).
 */
public class SimRedstoneBlocksClient {

    public static void registerClient() {
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.DIRECTIONAL_LINKED_RECEIVER, LinkedReceiverLinkBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, LinkedReceiverLinkBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.REDSTONE_ACCUMULATOR, RedstoneAccumulatorScrollValueBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.REDSTONE_INDUCTOR, RedstoneInductorValueBehaviour::new);

        AllBlockEntityBehaviours.add(SimBlockEntityTypes.LASER_POINTER, LaserPointerScrollValueBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.LASER_SENSOR, LaserSensorFilterBehaviour::create);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.OPTICAL_SENSOR, LaserSensorFilterBehaviour::create, OpticalSensorScrollValueBehaviour::new);

        AllBlockEntityBehaviours.add(SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorValueBehaviour::new, VelocitySensorTooltipBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorValueBehaviour::new, GimbalSensorTooltipBehaviour::new);
        AllBlockEntityBehaviours.add(SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorTooltipBehaviour::new);

        SimMovementBehaviours.ALTITUDE_SENSOR.attachRender = new AltitudeSensorMovementRenderBehaviour();

        SpecialGuiElementRegistry.register(context -> new ModulatingLinkedReceiverGuiRenderer(context.vertexConsumers()));
    }
}
