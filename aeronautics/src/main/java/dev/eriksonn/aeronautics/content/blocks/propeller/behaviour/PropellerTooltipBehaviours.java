package dev.eriksonn.aeronautics.content.blocks.propeller.behaviour;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.MechanicalBearingTooltipBehaviour;
import com.zurrtum.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroActorBehaviour;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Client-only goggle tooltips of the propellers (upstream: {@code PropellerActorBehaviour#addToGoggleTooltip}, called
 * from the block entities' {@code addToGoggleTooltip}). Create Fly reads goggle information from
 * {@link com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour}s, so these replace the
 * default kinetic tooltip behaviours of the propeller block entity types.
 */
public final class PropellerTooltipBehaviours {
    private PropellerTooltipBehaviours() {
    }

    /**
     * Upstream {@code PropellerActorBehaviour#addToGoggleTooltip} (+ {@code GyroActorBehaviour#additionalTooltipInfo})
     */
    public static boolean addPropellerInfo(final PropellerActorBehaviour behaviour, final List<Component> tooltip, final boolean isPlayerSneaking) {
        final BlockEntityPropeller propeller = behaviour.getPropeller();
        if (propeller.isActive()) {
            AeroLang.emptyLine(tooltip);
            AeroLang.blockName(behaviour.blockEntity.getBlockState()).text(":")
                    .forGoggles(tooltip);

            // We use updaters here to ensure we have proper, client side information at hand, as this can be called multiple times per tick
            final MutableComponent thrustComponent = AeroLang.pixelNewton(Math.abs(propeller.getScaledThrust()))
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("propeller.thrust", thrustComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            final MutableComponent airflowComponent = AeroLang.translate("unit.meters_per_second", String.format("%.2f", Math.abs(propeller.getAirflow())))
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("propeller.airflow", airflowComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            if (behaviour instanceof GyroActorBehaviour<?>) {
                addGyroInfo(behaviour, propeller, tooltip);
            }

            return true;
        }

        return false;
    }

    /**
     * Upstream {@code GyroActorBehaviour#additionalTooltipInfo}
     */
    private static void addGyroInfo(final PropellerActorBehaviour behaviour, final BlockEntityPropeller propeller, final List<Component> tooltip) {
        final double gravStrength = DimensionPhysicsData.getGravity(behaviour.getLevel(), JOMLConversion.toJOML(behaviour.getPos().getCenter())).length();

        final MutableComponent canLiftComponent = AeroLang.kilopixelGram(Math.abs(propeller.getScaledThrust()) / gravStrength)
                .style(ChatFormatting.AQUA)
                .component();

        AeroLang.translate("propeller.can_lift", canLiftComponent)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
    }

    /**
     * Andesite, wooden and smart propellers (upstream {@code BasePropellerBlockEntity#addToGoggleTooltip})
     */
    public static class SmallPropellerTooltipBehaviour<T extends BasePropellerBlockEntity> extends KineticTooltipBehaviour<T> {
        public SmallPropellerTooltipBehaviour(final T be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
            if (!super.addToGoggleTooltip(tooltip, isPlayerSneaking))
                return false;

            return addPropellerInfo(this.blockEntity.prop, tooltip, isPlayerSneaking);
        }
    }

    /**
     * Propeller and gyroscopic propeller bearings (upstream {@code PropellerBearingBlockEntity#addToGoggleTooltip} on top of
     * the mechanical bearing's tooltip and assembly exceptions)
     */
    public static class PropellerBearingTooltipBehaviour extends MechanicalBearingTooltipBehaviour {
        public PropellerBearingTooltipBehaviour(final MechanicalBearingBlockEntity be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
            if (!super.addToGoggleTooltip(tooltip, isPlayerSneaking)) {
                return false;
            }

            return addPropellerInfo(((PropellerBearingBlockEntity) this.blockEntity).getPropellerBehaviour(), tooltip, isPlayerSneaking);
        }
    }
}
