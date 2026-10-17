package dev.ryanhcode.sable.fabric.mixin.compatibility.create.behaviour_compatibility.harvester_block_entity;

import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.content.contraptions.actors.harvester.HarvesterRenderer;
import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.harvester.HarvesterLerpedSpeed;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spins harvester blades on moving sub-levels with the sub-level's speed.
 * <p>
 * Upstream wrapped {@code HarvesterRenderer#transform} in {@code renderSafe}. Create Fly's render-state renderer
 * computes the blade angle in {@code extractRenderState} and applies the same pivot/rotation in
 * {@code HarvesterRenderState#render}, so only the angle is replaced here (when the block entity has no animated speed
 * of its own).
 */
@Mixin(HarvesterRenderer.class)
public class HarvesterRendererMixin {

    @Inject(method = "extractRenderState(Lcom/zurrtum/create/content/contraptions/actors/harvester/HarvesterBlockEntity;Lcom/zurrtum/create/client/content/contraptions/actors/harvester/HarvesterRenderer$HarvesterRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
    public void sable$smoothSpeed(final HarvesterBlockEntity be, final HarvesterRenderer.HarvesterRenderState state, final float pt, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, final CallbackInfo ci) {
        if (be.getAnimatedSpeed() == 0) { // use our own transformation
            state.angle = AngleHelper.rad(-((HarvesterLerpedSpeed) be).sable$getLerpedFloat().getValue(pt));
        }
    }
}
