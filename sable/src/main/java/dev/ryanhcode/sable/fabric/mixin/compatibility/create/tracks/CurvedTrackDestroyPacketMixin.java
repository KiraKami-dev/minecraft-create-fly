package dev.ryanhcode.sable.fabric.mixin.compatibility.create.tracks;

import com.zurrtum.create.AllHandle;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the player distance check done when destroying a curved track take sub-levels into account.
 * <br>
 * Create Fly handles {@code CurvedTrackDestroyPacket} in {@link AllHandle#onCurvedTrackDestroy}, whose block entity
 * callback is the lambda targeted here (upstream: {@code CurvedTrackDestroyPacket#applySettings}).
 */
@Mixin(AllHandle.class)
public class CurvedTrackDestroyPacketMixin {

    @Redirect(method = "lambda$onCurvedTrackDestroy$5", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/track/TrackBlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;", remap = true), remap = false)
    private static BlockPos sable$getWorldBlockPos(final TrackBlockEntity instance) {
        return BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(instance.getLevel(), instance.getBlockPos().getCenter()));
    }

}
