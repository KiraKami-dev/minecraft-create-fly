package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.zurrtum.create.content.kinetics.base.IRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only value box slot of the steam vent (upstream {@code SteamVentBlockEntity.SteamVentValueBoxTransform}): on top
 * of the vent, the side facing the cursor is used
 */
public class SteamVentValueBoxTransform extends ValueBoxTransform.Sided {
    final BlockEntity be;

    public SteamVentValueBoxTransform(final BlockEntity be) {
        this.be = be;
    }

    @Override
    public void fromSide(final Direction direction) {
        this.direction = direction;

        Level level = this.be.getLevel();
        if (level != null && level.isClientSide() && direction == Direction.UP) {
            final Minecraft mc = Minecraft.getInstance();
            final HitResult target = mc.hitResult;
            if (target instanceof BlockHitResult) {
                final Vec3 hit = target.getLocation();
                final Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
                if (localHit.y < 0.4) {
                    this.direction = Direction.getApproximateNearest(localHit.x, 0, localHit.z);
                }
            }
        }
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8f, 12);
    }

    @Override
    public float getScale() {
        return 0.45f;
    }

    protected ValueBoxTransform getMovementModeSlot() {
        return new DirectionalExtenderScrollOptionSlot((state, d) -> {
            final Direction.Axis axis = d.getAxis();

            final Direction.Axis shaftAxis = ((IRotate) state.getBlock()).getRotationAxis(state);

            return shaftAxis != axis;
        });
    }

    @Override
    public void rotate(final BlockState state, final PoseStack ms) {
        final float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180;
        float xRot = this.getSide() == Direction.UP ? 90 : this.getSide() == Direction.DOWN ? 270 : 0;
        xRot += 22.5f;
        TransformStack.of(ms)
                .rotateYDegrees(yRot)
                .rotateXDegrees(xRot);
    }

    @Override
    protected boolean isSideActive(final BlockState state, final Direction direction) {
        Level level = this.be.getLevel();
        if (level != null && level.isClientSide() && direction == Direction.UP) {
            final Minecraft mc = Minecraft.getInstance();
            final HitResult target = mc.hitResult;
            if (target instanceof BlockHitResult) {
                final Vec3 hit = target.getLocation();
                final Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
                return localHit.y < 0.4;
            }
        }
        return true;
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
