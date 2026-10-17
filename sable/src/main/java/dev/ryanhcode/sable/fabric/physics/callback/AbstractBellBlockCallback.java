package dev.ryanhcode.sable.fabric.physics.callback;

import com.zurrtum.create.content.equipment.bell.AbstractBellBlock;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.fabric.mixin.compatibility.create.impact.AbstractBellBlockAccessor;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * Rings Create bells when a sub-level hits them. Only reachable through the Create-gated {@code AbstractBellBlockMixin}.
 */
public class AbstractBellBlockCallback extends FragileBlockCallback {
    public static final AbstractBellBlockCallback INSTANCE = new AbstractBellBlockCallback();

    public AbstractBellBlockCallback() {}

    @Override
    public boolean shouldTriggerFor(final BlockState state) {
        return state.getBlock() instanceof AbstractBellBlock<?>;
    }

    @Override
    public CollisionResult onHit(final ServerLevel level, final BlockPos pos, final BlockState state, final Vector3d hitPos) {
        final Vec3 hitDir = pos.getCenter().subtract(hitPos.x, hitPos.y, hitPos.z);
        final Direction facing = state.getValue(AbstractBellBlock.FACING);
        final BellAttachType attachment = state.getValue(AbstractBellBlock.ATTACHMENT);

        int xMul = Math.abs(facing.getStepX());
        int zMul = Math.abs(facing.getStepZ());

        if (attachment == BellAttachType.CEILING) {
            xMul = 1;
            zMul = 1;
        }

        final Direction ringDir = Direction.getApproximateNearest(hitDir.x * xMul, 0.0, hitDir.z * zMul)
                .getOpposite();
        final AbstractBellBlock<?> block = (AbstractBellBlock<?>) state.getBlock();

        if (block.canRingFrom(state, ringDir, 0.0)) {
            ((AbstractBellBlockAccessor) block).invokeRing(level, pos, ringDir, null);
        }

        return new CollisionResult(JOMLConversion.ZERO, false);
    }
}
