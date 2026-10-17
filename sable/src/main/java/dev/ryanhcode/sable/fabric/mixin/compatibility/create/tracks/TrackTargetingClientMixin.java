package dev.ryanhcode.sable.fabric.mixin.compatibility.create.tracks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.trains.track.TrackBlockRenderState;
import com.zurrtum.create.client.content.trains.track.TrackBlockRenderer;
import com.zurrtum.create.client.content.trains.track.TrackTargetingClient;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import com.zurrtum.create.infrastructure.component.BezierTrackPointLocation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Renders the track targeting overlay of tracks on sub-levels in the sub-level's frame.
 * <br>
 * Upstream replaced the camera-relative {@code translate} of the overlay; Create Fly bakes that translation into the
 * render state as an offset instead, so the offset is made local and the sub-level transform is applied around
 * the render call.
 */
@Mixin(TrackTargetingClient.class)
public class TrackTargetingClientMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/content/trains/track/TrackBlockRenderer;getRenderState(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction$AxisDirection;Lcom/zurrtum/create/infrastructure/component/BezierTrackPointLocation;Lcom/zurrtum/create/content/trains/track/TrackTargetingBehaviour$RenderedTrackOverlayType;F)Lcom/zurrtum/create/client/content/trains/track/TrackBlockRenderState;"), remap = false)
    private static TrackBlockRenderState sable$useLocalOffset(final TrackBlockRenderer renderer,
                                                              final Level level,
                                                              final Vec3 offset,
                                                              final BlockState state,
                                                              final BlockPos pos,
                                                              final Direction.AxisDirection direction,
                                                              final BezierTrackPointLocation bezier,
                                                              final TrackTargetingBehaviour.RenderedTrackOverlayType type,
                                                              final float scale,
                                                              final Operation<TrackBlockRenderState> original,
                                                              @Share("subLevel") final LocalRef<ClientSubLevel> subLevelRef) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            subLevelRef.set(clientSubLevel);
            // The sub-level transform is applied to the pose stack instead
            return original.call(renderer, level, Vec3.ZERO, state, pos, direction, bezier, type, scale);
        }

        return original.call(renderer, level, offset, state, pos, direction, bezier, type, scale);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/content/trains/track/TrackBlockRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"), remap = false)
    private static void sable$manipulateMatrixStack(final TrackBlockRenderState instance,
                                                    final PoseStack ms,
                                                    final MultiBufferSource buffer,
                                                    final Operation<Void> original,
                                                    @Local(argsOnly = true) final Vec3 camera,
                                                    @Local final BlockPos pos,
                                                    @Share("subLevel") final LocalRef<ClientSubLevel> subLevelRef) {
        final ClientSubLevel clientSubLevel = subLevelRef.get();

        if (clientSubLevel == null) {
            original.call(instance, ms, buffer);
            return;
        }

        final Pose3dc renderPose = clientSubLevel.renderPose();
        final Vec3 renderPos = renderPose.transformPosition(Vec3.atLowerCornerOf(pos));
        final Quaternionf renderOrientation = new Quaternionf(renderPose.orientation());

        ms.pushPose();
        ms.translate(renderPos.x() - camera.x(), renderPos.y() - camera.y(), renderPos.z() - camera.z());
        ms.mulPose(renderOrientation);
        original.call(instance, ms, buffer);
        ms.popPose();
    }

}
