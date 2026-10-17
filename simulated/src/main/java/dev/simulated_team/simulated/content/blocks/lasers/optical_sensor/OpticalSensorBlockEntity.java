package dev.simulated_team.simulated.content.blocks.lasers.optical_sensor;

import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.filtering.ServerFilteringBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.service.SimFluidService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3dc;

import java.awt.*;
import java.util.List;

public class OpticalSensorBlockEntity extends AbstractLaserBlockEntity implements Clearable, ClipboardCloneable {

    /**
     * Server halves of the filter and the range setting, the value boxes are
     * {@code LaserSensorFilterBehaviour#create} and {@link OpticalSensorScrollValueBehaviour} on the client
     */
    private ServerFilteringBehaviour filter;
    private ServerScrollValueBehaviour range;
    public LaserBehaviour laser;

    private Block hitBlock = Blocks.AIR;
    private float rayDistance = this.getRaycastLength();
    private float lastRayDistance = this.getRaycastLength();

    private float opacity = 1;

    public Block getHitBlock() {
        return this.hitBlock;
    }

    public float getHitBlockDistance() {
        if (this.hitBlock.defaultBlockState().isAir()) {
            return this.getRaycastLength();
        }
        final Vector3dc pos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()));
        final Vector3dc hitPos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.toJOML(this.laser.getBlockHitResult().getLocation()));
        return (float) pos.distance(hitPos);
    }

    public boolean hasHit() {
        return !this.hitBlock.defaultBlockState().isAir();
    }

    public OpticalSensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {
        behaviours.add(this.filter = new ServerFilteringBehaviour(this));
        final int maxRange = SimConfigService.INSTANCE.server().blocks.opticalSensorRange.get();
        behaviours.add(this.range = new ServerScrollValueBehaviour(this).between(1, maxRange));
        this.range.setValue(maxRange);
        behaviours.add(this.laser = new LaserBehaviour(this, this::gatherStartAndEnd, this::getRaycastLength));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isVirtual()) {
            final BlockHitResult context = this.laser.getBlockHitResult();
            if (context != null && this.hasLevel()) {
                this.rayDistance = (float) Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(this.level, this.laser.getLaserPositions().get().get(true), context.getLocation()));

                final boolean shouldPower = this.checkFilter(context);
                if (this.lastRayDistance != this.rayDistance || this.getBlockState().getValue(OpticalSensorBlock.POWERED) != shouldPower) {
                    this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(OpticalSensorBlock.POWERED, shouldPower));
                    this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
                    this.invalidateRenderBoundingBox();
                }

                this.lastRayDistance = this.rayDistance;
            }
            if(SimFluidService.INSTANCE.getFluidInItem(this.filter.getFilter()) != null) {
                this.laser.setFluidCollide(ClipContext.Fluid.ANY);
            } else {
                this.laser.setFluidCollide(ClipContext.Fluid.NONE);
            }
        }
    }

    private boolean checkFilter(final BlockHitResult context) {
        final BlockState hitBlock = this.level.getBlockState(context.getBlockPos());
        final FluidState hitFluid = this.level.getFluidState(context.getBlockPos());
        boolean passed = false;

        final ItemStack filterItem = this.filter.getFilter();
        if (context.getType() != HitResult.Type.MISS) {
            if (!filterItem.isEmpty()) {
                final Fluid fluidInItem = SimFluidService.INSTANCE.getFluidInItem(filterItem);
                if (fluidInItem != null && !hitFluid.isEmpty()) {
                    passed = fluidInItem.isSame(hitFluid.getType());
                } else {
                    passed = !hitBlock.isAir() && this.filter.test(new ItemStack(hitBlock.getBlock()));
                }
            } else {
                passed = true;
            }
        }

        this.hitBlock = passed ? hitBlock.getBlock() : Blocks.AIR;
        return passed;
    }

    @Override
    public Direction getDirection() {
        final AttachFace target = this.getBlockState().getValue(OpticalSensorBlock.TARGET);
        if (target == AttachFace.CEILING) {
            return Direction.UP;
        } else if (target == AttachFace.FLOOR) {
            return Direction.DOWN;
        } else {
            return this.getBlockState().getValue(OpticalSensorBlock.FACING);
        }
    }

    @Override
    public boolean shouldCast() {
        return true;
    }

    public float getRaycastLength() {
        return this.range.getValue() + 0.5f;
    }

    public int getRange() {
        return this.range.getValue();
    }

    public void setRange(final int blocks) {
        final int max = SimConfigService.INSTANCE.server().blocks.opticalSensorRange.get();
        this.range.setValue(Math.clamp(blocks, 1, max));
    }

    public float getRayDistance() {
       return this.rayDistance;
    }

    /**
     * @param item item to be applied
     * @return true if successful opacity change
     */
    public boolean tryApplyDye(final ItemStack item) {
        if (item.getItem() instanceof final DyeItem dyeItem) {
            final Color color = new Color(dyeItem.getDyeColor().getTextColor());
            
            // color is grayscale
            if (color.getRed() == color.getGreen() && color.getGreen() == color.getBlue()) {
                this.opacity = (float) color.getRed() / 255;
                this.opacity *= this.opacity; // square it so the falloff seems more linear

                this.setChanged();
                this.sendData();
                return true;
            }
        }
        return false;
    }

    public float getOpacity() {
        return this.opacity;
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);
        this.opacity = Math.clamp(view.getFloatOr("Opacity", 1), 0, 1);
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);
        view.putFloat("Opacity", this.opacity);
    }

    @Override
    public void clearContent() {
        this.filter.setFilter(ItemStack.EMPTY);
    }

    @Override
    public String getClipboardKey() {
        return "OpticalSensor";
    }

    @Override
    public boolean writeToClipboard(final ValueOutput view, final Direction side) {
        view.putFloat("Opacity", this.getOpacity());
        return true;
    }

    @Override
    public boolean readFromClipboard(final ValueInput view, final Player player, final Direction side, final boolean simulate) {
        if(simulate) {
            return true;
        }
        this.opacity = view.getFloatOr("Opacity", 0);
        return true;
    }
}
