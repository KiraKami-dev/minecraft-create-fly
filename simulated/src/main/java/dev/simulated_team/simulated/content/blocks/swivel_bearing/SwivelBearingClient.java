package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.content.contraptions.IDisplayAssemblyExceptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.content.contraptions.AssemblyException;
import com.zurrtum.create.content.contraptions.bearing.BearingBlock;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity.LockingSetting;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiPredicate;

import static net.minecraft.ChatFormatting.GOLD;

/**
 * Client-only parts of the swivel bearing, split off its block entity the way Create Fly splits its own: the
 * locking-setting value box and the hover tooltip (including assembly exceptions).
 */
public final class SwivelBearingClient {
    private static final MutableComponent SCROLL_OPTION_TITLE = Component.translatable(Simulated.MOD_ID + ".scroll_option.swivel_default_locked");

    private SwivelBearingClient() {
    }

    /**
     * Attaches the client behaviours to the swivel bearing block entity type. The tooltip behaviour replaces the default
     * {@link KineticTooltipBehaviour} (same behaviour type), so this must run after the default kinetic behaviours are
     * registered for {@link SimBlockEntityTypes#SWIVEL_BEARING}.
     */
    public static void registerClient() {
        BlockEntityBehaviour.addClient(SimBlockEntityTypes.SWIVEL_BEARING, LockingScrollBehaviour::new);
        BlockEntityBehaviour.addClient(SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingTooltipBehaviour::new);
    }

    /**
     * The value box of {@code SwivelBearingBlockEntity#lockedDefaultOption} (upstream: a {@code ScrollOptionBehaviour}
     * created by the block entity itself)
     */
    public static class LockingScrollBehaviour extends ScrollOptionBehaviour<LockingSetting> {
        public LockingScrollBehaviour(final SwivelBearingBlockEntity be) {
            super(LockingSettingIcon.class, LockingSettingIcon::from, SCROLL_OPTION_TITLE, be, new SelectionModeValueBox(SwivelBearingBlockEntity::isValidForOptionPanel));
        }
    }

    /**
     * Icons for {@link LockingSetting}, in the same order
     * <p>
     * TODO: separate icon for the locked settings
     */
    private enum LockingSettingIcon implements INamedIconOptions {
        LOCKED_ALWAYS(AllIcons.I_CONFIG_LOCKED, LockingSetting.LOCKED_ALWAYS),
        LOCKED_DEFAULT(AllIcons.I_CONFIG_LOCKED, LockingSetting.LOCKED_DEFAULT),
        UNLOCKED_DEFAULT(AllIcons.I_CONFIG_UNLOCKED, LockingSetting.UNLOCKED_DEFAULT),
        UNLOCKED_ALWAYS(AllIcons.I_CONFIG_UNLOCKED, LockingSetting.UNLOCKED_ALWAYS);

        private final AllIcons icon;
        private final LockingSetting setting;

        LockingSettingIcon(final AllIcons icon, final LockingSetting setting) {
            this.icon = icon;
            this.setting = setting;
        }

        public static LockingSettingIcon from(final LockingSetting setting) {
            return values()[setting.ordinal()];
        }

        @Override
        public AllIcons getIcon() {
            return this.icon;
        }

        @Override
        public String getTranslationKey() {
            return this.setting.getTranslationKey();
        }
    }

    private static class SelectionModeValueBox extends CenteredSideValueBoxTransform {
        public SelectionModeValueBox(final BiPredicate<BlockState, Direction> allowedDirections) {
            super(allowedDirections);
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            return super.getLocalOffset(state)
                    .subtract(Vec3.atLowerCornerOf(state.getValue(SwivelBearingBlock.FACING).getUnitVec3i())
                            .scale(5 / 16f));
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.75);
        }

        @Override
        public float getScale() {
            return 0.35f;
        }
    }

    /**
     * Upstream {@code SwivelBearingBlockEntity#addToTooltip} and its {@code IDisplayAssemblyExceptions}
     */
    public static class SwivelBearingTooltipBehaviour extends KineticTooltipBehaviour<SwivelBearingBlockEntity> implements IDisplayAssemblyExceptions {
        public SwivelBearingTooltipBehaviour(final SwivelBearingBlockEntity be) {
            super(be);
        }

        @Override
        public boolean addToTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
            if (super.addToTooltip(tooltip, isPlayerSneaking))
                return true;

            if (isPlayerSneaking)
                return false;

            if (this.blockEntity.getExtraKinetics().getSpeed() == 0)
                return false;

            if (this.blockEntity.isAssembled()) {
                if (this.blockEntity.isTooFast()) {
                    SimLang.translate("swivel_bearing.too_fast")
                            .style(GOLD)
                            .forGoggles(tooltip);

                    final MutableComponent component = SimLang.translate("swivel_bearing.too_fast_error")
                            .component();

                    final List<Component> cutString = TooltipHelper.cutTextComponent(component, FontHelper.Palette.GRAY_AND_WHITE);
                    tooltip.addAll(cutString);

                    return true;
                }

                return false;
            }
            final BlockState state = this.blockEntity.getBlockState();
            if (!(state.getBlock() instanceof SwivelBearingBlock))
                return false;

            final BlockState attachedState = this.blockEntity.getLevel().getBlockState(this.blockEntity.getBlockPos().relative(state.getValue(BearingBlock.FACING)));
            if (attachedState.canBeReplaced())
                return false;
            TooltipHelper.addHint(tooltip, "hint.empty_bearing");
            return true;
        }

        @Override
        public AssemblyException getLastAssemblyException() {
            return this.blockEntity.getLastAssemblyException();
        }
    }
}
