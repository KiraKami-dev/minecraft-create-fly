package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorInteractorBehaviour;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimLevelUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LaserPointerBlockEntity extends AbstractLaserBlockEntity implements ClipboardCloneable {
    /**
     * Server half of the max length setting, the value box is {@link LaserPointerScrollValueBehaviour} on the client
     */
    private ServerScrollValueBehaviour range;
    public LaserSensorInteractorBehaviour sensorInteraction;
    private boolean rainbow;
    public int laserColor;
    protected int bestPower;
    public Vec3 currentHitPos;

    public LaserPointerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.laserColor = SimColors.MEDIA_OURPLE;
        this.currentHitPos = Vec3.ZERO;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        final int rangeMax = SimConfigService.INSTANCE.server().blocks.laserPointerRange.get();
        this.range = new ServerScrollValueBehaviour(this).between(1, rangeMax);
        this.range.setValue(rangeMax);

        this.sensorInteraction = new LaserSensorInteractorBehaviour(this, this::gatherStartAndEnd, this::getRaycastLength, this::getPower, this::matchesSensor);
        this.sensorInteraction.setShouldCast(this::shouldCast);

        behaviours.add(this.sensorInteraction);
        behaviours.add(this.range);
    }

    @Override
    public void tick() {
        if (this.level == null || !SimLevelUtil.isAreaActuallyLoaded(this.level, this.worldPosition, 2)) {
            return;
        }

        if (!this.level.isClientSide() || this.isVirtual()) {
            final int currentPower = this.level.getBestNeighborSignal(this.worldPosition);
            if (currentPower != this.bestPower) {
                this.bestPower = currentPower;
                this.sendData();
            }
        }

        super.tick();
        if (!this.shouldCast()) {
            this.currentHitPos = Vec3.ZERO;
            return;
        }

        if (!this.isVirtual()) {
            final BlockHitResult context = this.sensorInteraction.getBlockHitResult();
            if (context.getType() != HitResult.Type.MISS) {
                this.currentHitPos = context.getLocation();
            } else {
                this.currentHitPos = Vec3.ZERO;
            }
        }
    }

    public boolean isAmethyst() {
        return this.laserColor == SimColors.MEDIA_OURPLE && !this.isRainbow();
    }

    public int getPower() {
        return this.getBlockState().getValue(LaserPointerBlock.INVERTED) ? 15 - this.bestPower : this.bestPower;
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        this.laserColor = view.getIntOr("LaserColor", SimColors.MEDIA_OURPLE);
        this.bestPower = view.getIntOr("BestPower", 0);
        this.rainbow = view.getBooleanOr("Rainbow", false);

        this.currentHitPos = view.read("HitPos", Vec3.CODEC).orElse(Vec3.ZERO);
        super.read(view, clientPacket);
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        view.putInt("LaserColor", this.laserColor);
        view.putInt("BestPower", this.bestPower);
        view.putBoolean("Rainbow", this.isRainbow());

        // a list of three doubles, as VecHelper#writeNBT wrote it
        view.store("HitPos", Vec3.CODEC, this.currentHitPos);
        super.write(view, clientPacket);
    }

    @Override
    public Direction getDirection() {
        return this.getBlockState().getValue(LaserPointerBlock.FACING);
    }

    @Override
    public float getRaycastLength() {
        return this.range.getValue();
    }

    @Override
    public boolean shouldCast() {
        return this.getPower() != 0;
    }

    public void setLaserColor(final int color) {
        this.laserColor = color;
        this.setChanged();
        this.sendData();
    }

    public int getLaserColor() {
        return this.laserColor;
    }

    public boolean matchesSensor(final LaserSensorBlockEntity sensor) {
        return sensor.filterColor(this.laserColor, this.rainbow);
    }

    @Override
    public String getClipboardKey() {
        return "LaserPointer";
    }

    @Override
    public boolean writeToClipboard(final ValueOutput view, final Direction side) {
        view.putInt("Color", this.laserColor);
        view.putBoolean("Rainbow", this.isRainbow());
        return true;
    }

    @Override
    public boolean readFromClipboard(final ValueInput view, final Player player, final Direction side, final boolean simulate) {
        if(simulate) {
            return true;
        }
        this.setLaserColor(view.getIntOr("Color", 0));
        this.setRainbow(view.getBooleanOr("Rainbow", false));
        return true;
    }

    public boolean isRainbow() {
        return this.rainbow;
    }

    public void setRainbow(final boolean rainbow) {
        this.rainbow = rainbow;

        if (!this.getLevel().isClientSide()) {
            this.notifyUpdate();
        }
    }
}
