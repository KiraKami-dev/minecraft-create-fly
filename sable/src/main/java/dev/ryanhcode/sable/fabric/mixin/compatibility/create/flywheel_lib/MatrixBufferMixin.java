package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.backend.engine.indirect.MatrixBuffer;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.SableFlywheelMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MatrixBuffer.class)
public class MatrixBufferMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/flywheel/backend/engine/indirect/ResizableStorageArray;<init>(J)V"), index = 0, remap = false)
    private long sable$overrideMatrixSize(final long stride) {
        return SableFlywheelMatrixBuffer.INFO_SIZE_BYTES;
    }

}
