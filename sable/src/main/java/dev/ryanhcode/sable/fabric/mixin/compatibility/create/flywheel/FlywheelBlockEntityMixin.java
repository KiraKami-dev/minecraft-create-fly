package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel;

import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.flywheel.FlywheelBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlywheelBlockEntity.class)
public abstract class FlywheelBlockEntityMixin extends KineticBlockEntity implements BlockEntitySubLevelReactionWheel {

    @Unique float sable$smoothedSpeed = 0;
    public FlywheelBlockEntityMixin(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    @Inject(method = "tick",at = @At("HEAD"), remap = false)
    public void sable$tick(CallbackInfo ci)
    {
        sable$smoothedSpeed += (speed - sable$smoothedSpeed) / 32f;
    }

    /**
     * Create Fly's FlywheelBlockEntity does not override {@code write}, so it is added here (upstream injected at its TAIL).
     */
    @Override
    protected void write(final ValueOutput view, final boolean clientPacket)
    {
        super.write(view, clientPacket);
        view.putFloat("SmoothedSpeed",sable$smoothedSpeed);
    }

    @Inject(method = "read",at = @At("TAIL"), remap = false)
    public void sable$read(ValueInput view, boolean clientPacket, CallbackInfo ci)
    {
        sable$smoothedSpeed = view.getFloatOr("SmoothedSpeed", 0);
    }

    @Override
    public void sable$getAngularVelocity(Vector3d v) {
        Direction.Axis axis = ((IRotate) getBlockState()
                .getBlock()).getRotationAxis(getBlockState());
        Direction dir = Direction.get(Direction.AxisDirection.NEGATIVE,axis);
        float angularSpeed = sable$smoothedSpeed * (float)Math.TAU / 60f;
        v.set(dir.getStepX(),dir.getStepY(),dir.getStepZ()).mul(angularSpeed);
    }
}
