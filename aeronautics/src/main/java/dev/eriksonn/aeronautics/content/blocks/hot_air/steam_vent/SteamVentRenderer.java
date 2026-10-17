package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SteamVentRenderer extends SmartBlockEntityRenderer<SteamVentBlockEntity, SteamVentRenderer.SteamVentRenderState> {

    public SteamVentRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SteamVentRenderState createRenderState() {
        return new SteamVentRenderState();
    }

    @Override
    public void extractRenderState(final SteamVentBlockEntity blockEntity, final SteamVentRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPos, crumblingOverlay);

        final float signalStrength = Math.max(0, blockEntity.signalStrength / 15F);
        final BlockState blockState = blockEntity.getBlockState();
        state.redstone = CachedBuffers.partial(AeroPartialModels.STEAM_VENT_REDSTONE, blockState);
        state.redstoneColor = SimColors.redstone(signalStrength);

        final GasEmitterRenderHandler renderHandler = blockEntity.getRenderHandler();
        state.alpha = renderHandler.getAlpha(partialTicks);

        if (state.alpha > 2) {
            state.position = renderHandler.getPosition(partialTicks);
            state.base = CachedBuffers.partial(AeroPartialModels.STEAM_VENT_BASE, blockState);
            state.jet = CachedBuffers.partial(AeroPartialModels.STEAM_VENT_JET, blockState);
        }
    }

    @Override
    public void submit(final SteamVentRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);

        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state::renderRedstone);

        if (state.alpha > 2) {
            ms.pushPose();

            queue.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), state::renderBase);

            ms.translate(0.0f, (state.position - 1) / 3.0f, 0.0f);
            queue.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), state::renderJet);

            ms.popPose();
        }
    }

    public static class SteamVentRenderState extends SmartRenderState {
        public SuperByteBuffer redstone;
        public int redstoneColor;
        public int alpha;
        public float position;
        public SuperByteBuffer base;
        public SuperByteBuffer jet;

        public void renderRedstone(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.redstone
                    .light(this.lightCoords)
                    .color(this.redstoneColor)
                    .renderInto(pose, vb);
        }

        public void renderBase(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.base.disableDiffuse()
                    .light(LightTexture.FULL_BRIGHT)
                    .color(255, 255, 255, this.alpha)
                    .renderInto(pose, vb);
        }

        public void renderJet(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.jet.disableDiffuse()
                    .light(LightTexture.FULL_BRIGHT)
                    .color(255, 255, 255, this.alpha)
                    .renderInto(pose, vb);
        }
    }
}
