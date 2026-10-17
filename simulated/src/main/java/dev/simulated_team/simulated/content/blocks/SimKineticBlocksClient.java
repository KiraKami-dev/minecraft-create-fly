package dev.simulated_team.simulated.content.blocks;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.AllExtensions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RotationDirectionScrollBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.GeneratingKineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineValueBoxTransform;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelAudioBehaviour;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringScrollValueBehaviour;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;

/**
 * Client-only registrations of the kinetic/control blocks (steering wheel, torsion spring, auger shaft, analog
 * transmission, throttle lever, portable engine): value boxes, goggle tooltips and audio, which Create Fly attaches
 * as client behaviours, plus the steering wheel's big outline.
 * <p>
 * Call after the default kinetic client behaviours (SimBlockEntityBehaviours): for the steering wheel, portable engine,
 * auger shaft and analog transmission the audio/tooltip behaviours registered here replace the defaults (same
 * behaviour type, the last registration wins).
 */
public class SimKineticBlocksClient {

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <T extends SmartBlockEntity> void add(final BlockEntityType<T> type, final Function<T, BlockEntityBehaviour<?>>... factories) {
        for (final Function<T, BlockEntityBehaviour<?>> factory : factories) {
            BlockEntityBehaviour.CLIENT_REGISTRY.add(type, (Function<SmartBlockEntity, BlockEntityBehaviour<?>>) factory);
        }
    }

    public static void registerClient() {
        add(SimBlockEntityTypes.STEERING_WHEEL,
                SteeringWheelScrollValueBehaviour::new,
                SteeringWheelAudioBehaviour::new,
                GeneratingKineticTooltipBehaviour::new);

        add(SimBlockEntityTypes.TORSION_SPRING,
                TorsionSpringScrollValueBehaviour::new);

        add(SimBlockEntityTypes.PORTABLE_ENGINE,
                be -> new RotationDirectionScrollBehaviour(be, CreateLang.translateDirect("contraptions.windmill.rotation_direction"), new PortableEngineValueBoxTransform()),
                KineticAudioBehaviour::new,
                PortableEngineTooltipBehaviour::new);

        add(SimBlockEntityTypes.AUGER_SHAFT,
                KineticAudioBehaviour::new,
                AugerShaftTooltipBehaviour::new);

        // the analog transmission's block entity type is SIMPLE_BE
        add(SimBlockEntityTypes.SIMPLE_BE,
                KineticAudioBehaviour::new,
                AnalogTransmissionTooltipBehaviour::new);

        add(SimBlockEntityTypes.THROTTLE_LEVER,
                ThrottleLeverTooltipBehaviour::new);

        // was IHaveBigOutline: the wheel's interaction shape reaches outside the block
        AllExtensions.BIG_OUTLINE.add(SimBlocks.STEERING_WHEEL);
    }
}
