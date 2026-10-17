package dev.simulated_team.simulated.index;

import com.zurrtum.create.client.content.kinetics.base.OrientedRotatingVisual;
import com.zurrtum.create.client.content.kinetics.base.SingleAxisRotatingVisual;
import com.zurrtum.create.client.content.kinetics.transmission.SplitShaftVisual;
import com.zurrtum.create.client.flywheel.api.visual.BlockEntityVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorRenderer;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionRenderer;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionVisual;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogVisual;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftRenderer;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftRenderer;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorVisual;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueRenderer;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableVisual;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerRenderer;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkVisual;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorVisual;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;
import dev.simulated_team.simulated.content.blocks.spring.SpringRenderer;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelVisual;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingVisual;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockRenderer;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverRenderer;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverVisual;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringRenderer;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringVisual;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Predicate;

/**
 * Client: block entity renderers and Flywheel visuals (Create Fly's {@code AllBlockEntityRenders}).
 * <p>
 * Upstream {@code .visual(factory)} defaulted to {@code renderNormally = true}, i.e. the visual does not replace the
 * renderer: that is {@link #normal}; {@code .visual(factory, false)} would be {@link #visual}.
 */
public class SimBlockEntityRenders {

    public static <T extends BlockEntity, P extends T, S extends BlockEntityRenderState> void visual(
            final BlockEntityType<P> type,
            final BlockEntityRendererProvider<T, S> rendererFactory,
            final SimpleBlockEntityVisualizer.Factory<P> visualizerFactory
    ) {
        visual(type, rendererFactory, visualizerFactory, blockEntity -> true);
    }

    public static <T extends BlockEntity, P extends T, S extends BlockEntityRenderState> void normal(
            final BlockEntityType<P> type,
            final BlockEntityRendererProvider<T, S> rendererFactory,
            final SimpleBlockEntityVisualizer.Factory<P> visualizerFactory
    ) {
        visual(type, rendererFactory, visualizerFactory, blockEntity -> false);
    }

    public static <T extends BlockEntity, P extends T, S extends BlockEntityRenderState> void visual(
            final BlockEntityType<P> type,
            final BlockEntityRendererProvider<T, S> rendererFactory,
            final SimpleBlockEntityVisualizer.Factory<P> visualizerFactory,
            final Predicate<P> skipVanillaRender
    ) {
        BlockEntityRenderers.register(type, rendererFactory);
        SimpleBlockEntityVisualizer.builder(type).factory(visualizerFactory).skipVanillaRender(skipVanillaRender).apply();
    }

    public static <T extends BlockEntity, P extends T, S extends BlockEntityRenderState> void render(
            final BlockEntityType<P> type,
            final BlockEntityRendererProvider<T, S> rendererFactory
    ) {
        BlockEntityRenderers.register(type, rendererFactory);
    }

    private static BlockEntityVisual<? super AugerShaftBlockEntity> augerVisual(final VisualizationContext ctx, final AugerShaftBlockEntity blockEntity,
                                                                              final float partialTick) {
        if (blockEntity.getBlockState().getBlock() instanceof AugerCogBlock) {
            return new AugerCogVisual(ctx, blockEntity, partialTick);
        }
        return SingleAxisRotatingVisual.shaft(ctx, blockEntity, partialTick);
    }

    public static void register() {
        normal(SimBlockEntityTypes.SIMPLE_BE, AnalogTransmissionRenderer::new, AnalogTransmissionVisual::new);
        normal(SimBlockEntityTypes.TORSION_SPRING, TorsionSpringRenderer::new, TorsionSpringVisual::new);
        render(SimBlockEntityTypes.PHYSICS_ASSEMBLER, PhysicsAssemblerRenderer::new);
        render(SimBlockEntityTypes.ROPE_WINCH, RopeWinchRenderer::new);
        render(SimBlockEntityTypes.ROPE_CONNECTOR, RopeConnectorRenderer::new);
        render(SimBlockEntityTypes.DOCKING_CONNECTOR, DockingConnectorRenderer::new);
        render(SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorRenderer::new);
        normal(SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorRenderer::new, GimbalSensorVisual::new);
        render(SimBlockEntityTypes.PORTABLE_ENGINE, PortableEngineRenderer::new);
        normal(SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingRenderer::new, SwivelBearingVisual::new);
        normal(SimBlockEntityTypes.STEERING_WHEEL, SteeringWheelRenderer::new, SteeringWheelVisual::new);
        render(SimBlockEntityTypes.SPRING, SpringRenderer::new);
        normal(
                SimBlockEntityTypes.SWIVEL_BEARING_LINK_BLOCK,
                SwivelBearingPlateBlockRenderer::new,
                OrientedRotatingVisual.<SwivelBearingPlateBlockEntity>of(SimPartialModels.SHAFT_SIXTEENTH)
        );
        render(SimBlockEntityTypes.LINKED_TYPEWRITER, LinkedTypewriterRenderer::new);

        render(SimBlockEntityTypes.OPTICAL_SENSOR, OpticalSensorRenderer::new);
        render(SimBlockEntityTypes.LASER_POINTER, LaserPointerRenderer::new);
        render(SimBlockEntityTypes.LASER_SENSOR, SmartBlockEntityRenderer::new);

        render(SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorRenderer::new);
        render(SimBlockEntityTypes.DIRECTIONAL_LINKED_RECEIVER, DirectionalLinkedReceiverRenderer::new);
        render(SimBlockEntityTypes.NAMEPLATE, NameplateRenderer::new);
        normal(SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, ModulatingLinkedReceiverRenderer::new, ModulatingLinkVisual::new);
        render(SimBlockEntityTypes.REDSTONE_ACCUMULATOR, RedstoneAccumulatorRenderer::new);
        normal(SimBlockEntityTypes.NAVIGATION_TABLE, NavTableRenderer::new, NavTableVisual::new);
        normal(SimBlockEntityTypes.REDSTONE_INDUCTOR, RedstoneInductorRenderer::new, RedstoneInductorVisual::new);
        normal(SimBlockEntityTypes.THROTTLE_LEVER, ThrottleLeverRenderer::new, ThrottleLeverVisual::new);
        normal(SimBlockEntityTypes.AUGER_SHAFT, AugerShaftRenderer::new, SimBlockEntityRenders::augerVisual);
        render(SimBlockEntityTypes.MERGING_GLUE, MergingGlueRenderer::new);
        normal(SimBlockEntityTypes.DIRECTIONAL_GEARSHIFT, DirectionalGearshiftRenderer::new, SplitShaftVisual::new);
    }
}
