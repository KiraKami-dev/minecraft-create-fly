package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.util.Observable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class AltitudeSensorBlockEntity extends SmartBlockEntity implements Observable, ClipboardCloneable {
	public float highSignal = 1.0f;
	public float lowSignal = 0.0f;

	public int signal = 0;
	public int tickCount = 0;

	public float visualHeight = 0.0f;
	public float previousVisualHeight = 0.0f;
	public boolean updateVisualHeight = true;

	public AltitudeSensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public int getSignal() {
		return Math.round(this.getValue() * 15.0f);
	}

	public float getValue() {
		final float y = this.getNormalHeight();
		final float value = (y - this.lowSignal) / (this.highSignal - this.lowSignal);
		return Mth.clamp(value, 0.0f, 1.0f);
	}

	public float getWorldHeight() {
		final Vector3d pos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()));
		return (float) pos.y;
	}

	public float getNormalHeight() {
		return this.toNormalHeight(this.getWorldHeight());
	}

	public float toWorldHeight(final float normalHeight) {
		return Mth.map(normalHeight, 0.0f, 1.0f, this.getLevel().getMinY(), this.getLevel().getMaxY() + 1);
	}

	public float toNormalHeight(final float worldHeight) {
		return Mth.map(worldHeight, this.getLevel().getMinY(), this.getLevel().getMaxY() + 1, 0.0f, 1.0f);
	}

	public double getAirPressure() {
		return DimensionPhysicsData.getAirPressure(this.getLevel(), Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos())));
	}

	public float getVisualHeight(final float partialTick) {
		return this.previousVisualHeight * (1 - partialTick) + this.visualHeight * partialTick;
	}

	public float getValue(final float partialTick) {
		final float y = this.getVisualHeight(partialTick);
		final float value = (y - this.lowSignal) / (this.highSignal - this.lowSignal);
		return Mth.clamp(value, 0.0f, 1.0f);
	}

	public void updateSignal() {
		this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState());
		this.getLevel().updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
		this.getLevel().updateNeighborsAt(this.getBlockPos().below(), this.getBlockState().getBlock());
	}

	@Override
	public void tick() {
		super.tick();

		this.tickCount++;

		final int lastSignal = this.signal;
		this.signal = this.getSignal();

		if(this.signal != lastSignal) {
            this.updateSignal();
		}

		if(this.getLevel().isClientSide()) {
			final float worldHeight = this.getWorldHeight();
			if(this.visualHeight == 0.0f) {
				this.visualHeight = worldHeight;
			}
			final float step = 0.15f;
			this.previousVisualHeight = this.visualHeight;
			if(this.updateVisualHeight) {
				this.visualHeight = this.visualHeight * (1.0f - step) + worldHeight * step;
			}
		}
	}

	@Override
	public void notifyUpdate() {
		super.notifyUpdate();
        this.updateSignal();
	}

	@Override
	protected void read(final ValueInput view, final boolean clientPacket) {
		super.read(view, clientPacket);
		this.highSignal = view.getFloatOr("high_signal", 0);
		this.lowSignal = view.getFloatOr("low_signal", 0);
	}

	@Override
	protected void write(final ValueOutput view, final boolean clientPacket) {
		super.write(view, clientPacket);
		view.putFloat("high_signal", this.highSignal);
		view.putFloat("low_signal", this.lowSignal);
	}

	@Override
	public void onObserved(final Player player) {
		if (this.getAirPressure() <= 0.0) {
			SimAdvancements.CAN_WE_GET_MUCH_HIGHER.awardTo(player);
		}
	}

	@Override
	public String getClipboardKey() {
		return "Altitude";
	}

	@Override
	public boolean writeToClipboard(final ValueOutput view, final Direction side) {
		view.putFloat("high_signal", this.highSignal);
		view.putFloat("low_signal", this.lowSignal);
		return true;
	}

	@Override
	public boolean readFromClipboard(final ValueInput view, final Player player, final Direction side, final boolean simulate) {
		if(simulate) {
			return true;
		}
		this.highSignal = view.getFloatOr("high_signal", 0);
		this.lowSignal = view.getFloatOr("low_signal", 0);
		this.setChanged();
		this.sendData();
		return true;
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
	}
}
