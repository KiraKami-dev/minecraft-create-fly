package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.google.common.collect.ImmutableList;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the velocity sensor's max speed value box; the server half is
 * {@link VelocitySensorBlockEntity.VelocitySensorScrollValueBehaviour}.
 */
public class VelocitySensorValueBehaviour extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {

    public VelocitySensorValueBehaviour(final SmartBlockEntity be) {
        super(SimLang.translate("velocity_sensor.description").component(), be, new VelocitySensorValueBoxTransform());
        this.withFormatter((value) -> value + " m/s");
    }

    @Override
    public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
        final ImmutableList<Component> rows = ImmutableList.of(
                SimLang.translate("velocity_sensor.selection.away").component(),
                SimLang.translate("velocity_sensor.selection.towards").component()
        );
        return new ValueSettingsBoard(this.label, this.behaviour.getMax(), 10, rows,
                new ValueSettingsFormatter(this::formatValue));
    }

    public MutableComponent formatValue(final ValueSettings settings) {
        return SimLang.number(settings.value()).component().append(" m/s");
    }

    private static class VelocitySensorValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.8);
        }

        @Override
        public float getScale() {
            return 0.35f;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return AbstractDirectionalAxisBlock.getAxis(state) == direction.getAxis();
        }
    }
}
