package dev.ryanhcode.sable.fabric.mixin.compatibility.create.blaze_burner;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the blaze burner head look at the player when the burner is on a sub-level.
 * Create Fly moved the head animation from {@code BlazeBurnerBlockEntity#tickAnimation} to the static client-side
 * {@link BlazeBurnerRenderer#tickAnimation(BlazeBurnerBlockEntity)}.
 */
@Mixin(BlazeBurnerRenderer.class)
public abstract class BlazeBurnerRendererMixin {

    @Unique
    private static final Vector3d sable$playerPos = new Vector3d();

    @Inject(method = "tickAnimation", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D", remap = true), remap = false)
    private static void sable$projectPlayerPosition(final BlazeBurnerBlockEntity be, final CallbackInfo ci, @Local(name = "x") final LocalDoubleRef x, @Local(name = "z") final LocalDoubleRef z, @Local(name = "player") final LocalPlayer player) {
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        if (subLevel != null) {
            sable$playerPos.set(x.get(), player.getEyeY(), z.get());
            subLevel.logicalPose().transformPositionInverse(sable$playerPos);
            x.set(sable$playerPos.x);
            z.set(sable$playerPos.z);
        }
    }
}
