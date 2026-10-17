package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.google.common.collect.ImmutableList;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentValueBoxTransform;
import dev.eriksonn.aeronautics.data.AeroLang;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only half of the hot air amount value box of burners and steam vents (upstream {@code HotAirBurnerValueBehaviour}
 * was one common class): label, slot, board and formatting. Value, range and stepping live in the block entity's
 * {@link HotAirBurnerValueBehaviour}.
 */
public class HotAirBurnerScrollValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, HotAirBurnerValueBehaviour> {
    private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.hot_air_amount").component();
    private static final MutableComponent TITLE = AeroLang.translate("generic.hot_air").component();
    private static final String VALUE_FORMAT = "%s m³";

    public HotAirBurnerScrollValueBehaviour(final Component label, final SmartBlockEntity be, final ValueBoxTransform slot) {
        super(label, be, slot);
        this.withFormatter(VALUE_FORMAT::formatted);
    }

    public static HotAirBurnerScrollValueBehaviour burner(final HotAirBurnerBlockEntity be) {
        return new HotAirBurnerScrollValueBehaviour(SCROLL_OPTION_TITLE, be, new HotAirBurnerValueBoxTransform());
    }

    public static HotAirBurnerScrollValueBehaviour steamVent(final SteamVentBlockEntity be) {
        return new HotAirBurnerScrollValueBehaviour(SCROLL_OPTION_TITLE, be, new SteamVentValueBoxTransform(be));
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        final int interval = this.behaviour.getInterval();
        return new ValueSettingsBoard(this.label, this.behaviour.getMaxValue() / interval, 10, ImmutableList.of(TITLE),
                new ValueSettingsFormatter(this::format));
    }

    private MutableComponent format(final ValueSettings valueSettings) {
        final int interval = this.behaviour.getInterval();
        int value = Mth.clamp(valueSettings.value() * interval, this.behaviour.getMinValue(), this.behaviour.getMaxValue());
        return AeroLang.translate("unit.meter_cubed", value).component();
    }

    /**
     * Upstream {@code HotAirBurnerBlockEntity.HotAirBurnerValueBoxTransform}
     */
    private static class HotAirBurnerValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8f, 14);
        }

        @Override
        public float getScale() {
            return 0.45f;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction.getAxis() != Direction.Axis.Y || direction.equals(Direction.DOWN);
        }

        @Override
        public Vec3 getLocalOffset(final BlockState state) {
            if (this.getSide() == Direction.DOWN) {
                return VecHelper.voxelSpace(8, 0, 8);
            }

            Vec3 location = this.getSouthLocation();

            location = location.add(VecHelper.voxelSpace(0, -3, 1.75));
            location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(this.getSide()), Direction.Axis.Y);

            return location;
        }
    }
}
