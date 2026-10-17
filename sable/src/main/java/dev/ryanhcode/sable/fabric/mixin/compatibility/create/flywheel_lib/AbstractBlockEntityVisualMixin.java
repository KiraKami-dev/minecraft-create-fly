package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractBlockEntityVisual.class)
public class AbstractBlockEntityVisualMixin {

    /**
     * Scales the (CPU-side, {@link com.zurrtum.create.client.flywheel.lib.instance.FlatLit}) light of block-entity visuals in
     * sub-levels by the sub-level's sky-light scale. Upstream only patched {@code relight(BlockPos, FlatLit...)}; the
     * {@code Iterator}/{@code Iterable} overloads and {@code computePackedLight} read the light the same way and are
     * covered too (e.g. belts relight through the {@code Iterable} overload).
     */
    @Redirect(method = {
            "relight(Lnet/minecraft/core/BlockPos;[Lcom/zurrtum/create/client/flywheel/lib/instance/FlatLit;)V",
            "relight(Lnet/minecraft/core/BlockPos;Ljava/util/Iterator;)V",
            "relight(Lnet/minecraft/core/BlockPos;Ljava/lang/Iterable;)V",
            "computePackedLight()I"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int sable$getLightColor(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
        assert container != null;
        final SubLevel subLevel = Sable.HELPER.getContainingClient(blockPos);

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            final int color = LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
            return clientSubLevel.scaleLightColor(color);
        }

        return LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
    }
}
