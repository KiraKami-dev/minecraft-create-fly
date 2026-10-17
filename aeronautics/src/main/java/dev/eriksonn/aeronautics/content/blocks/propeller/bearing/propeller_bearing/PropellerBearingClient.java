package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.content.contraptions.bearing.BearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity.ThrustDirection;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerTooltipBehaviours.PropellerBearingTooltipBehaviour;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only parts of the propeller bearings, split off the block entities the way Create Fly splits its mechanical
 * bearing: the thrust direction value box and the goggle tooltip.
 */
public final class PropellerBearingClient {
    private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.thrust_direction").component();

    private PropellerBearingClient() {
    }

    /**
     * The tooltip behaviour replaces the default kinetic tooltip behaviour (same behaviour type), so this must run after
     * the default kinetic behaviours of these block entity types are registered.
     */
    public static void registerClient() {
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.PROPELLER_BEARING, ThrustDirectionScrollBehaviour::new);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.PROPELLER_BEARING, PropellerBearingTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING, ThrustDirectionScrollBehaviour::new);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING, PropellerBearingTooltipBehaviour::new);
    }

    /**
     * The value box of {@link PropellerBearingBlockEntity#getThrustDirectionOption()} (upstream: a {@code ScrollOptionBehaviour}
     * created by the block entity on its {@code getMovementModeSlot()})
     */
    public static class ThrustDirectionScrollBehaviour extends ScrollOptionBehaviour<ThrustDirection> {
        public ThrustDirectionScrollBehaviour(final PropellerBearingBlockEntity be) {
            super(ThrustDirectionIcon.class, ThrustDirectionIcon::from, SCROLL_OPTION_TITLE, be, getSlot(be));
        }

        private static ValueBoxTransform getSlot(final PropellerBearingBlockEntity be) {
            if (be instanceof GyroscopicPropellerBearingBlockEntity) {
                return new GyroscopicPropellerValueBoxTransform();
            }

            return getMovementModeSlot();
        }
    }

    /**
     * Icons for {@link ThrustDirection}, in the same order
     */
    private enum ThrustDirectionIcon implements INamedIconOptions {
        RIGHT_HANDED(AllIcons.I_REFRESH, ThrustDirection.RIGHT_HANDED),
        LEFT_HANDED(AllIcons.I_ROTATE_CCW, ThrustDirection.LEFT_HANDED);

        private final AllIcons icon;
        private final ThrustDirection direction;

        ThrustDirectionIcon(final AllIcons icon, final ThrustDirection direction) {
            this.icon = icon;
            this.direction = direction;
        }

        public static ThrustDirectionIcon from(final ThrustDirection direction) {
            return values()[direction.ordinal()];
        }

        @Override
        public AllIcons getIcon() {
            return this.icon;
        }

        @Override
        public String getTranslationKey() {
            return this.direction.getTranslationKey();
        }
    }

    /**
     * Upstream {@code GyroscopicPropellerBearingBlockEntity.GyroscopicPropellerValueBoxTransform}
     */
    private static class GyroscopicPropellerValueBoxTransform extends DirectionalExtenderScrollOptionSlot {

        public GyroscopicPropellerValueBoxTransform() {
            super((state, d) -> {
                final Direction.Axis axis = d.getAxis();
                final Direction.Axis bearingAxis = state.getValue(BearingBlock.FACING).getAxis();
                return bearingAxis != axis;
            });
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            return super.getLocalOffset(state).add(Vec3.atLowerCornerOf((state.getValue(BlockStateProperties.FACING)).getUnitVec3i()).scale(-0.125));
        }
    }
}
