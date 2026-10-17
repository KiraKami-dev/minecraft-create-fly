package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NameplateRenderer implements BlockEntityRenderer<NameplateBlockEntity, NameplateRenderer.NameplateRenderState> {

    //taken from sign renderer
    private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);

    private final BlockEntityRendererProvider.Context context;
    public NameplateRenderer(final BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public NameplateRenderState createRenderState() {
        return new NameplateRenderState();
    }

    @Override
    public void extractRenderState(final NameplateBlockEntity be, final NameplateRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        final Font font = this.context.font();

        final BlockState blockState = state.blockState;
        state.facing = blockState.getValue(NameplateBlock.FACING);

        // can't just use be.isController() because it is never set properly on create contraptions
        final NameplateBlock.Position pos = blockState.getValue(NameplateBlock.POSITION);
        state.visible = pos == NameplateBlock.Position.LEFT || pos == NameplateBlock.Position.SINGLE;
        if (!state.visible) {
            return;
        }

        state.pixelsTall = be.glowing ? 5 : 6;
        final int pixelsLeft = 3;

        final int availableSpace = ((be.getControllerWidth()) * 16 - pixelsLeft * 2) * 7 / state.pixelsTall + 1;
        final String trimmed = font.plainSubstrByWidth(be.getName(), availableSpace);

        final int width = font.width(trimmed);

        state.centerPixels = (availableSpace - 1) / 2.0 - width / 2.0;

        final MutableComponent textComponent = SimLang.text(trimmed).component();
        state.sequences = font.split(textComponent, width);

        if (be.glowing) {
            state.textColor = be.getTextColor().getTextColor();
            state.outlineColor = isOutlineVisible(be.getBlockPos(), state.textColor) ? be.getDarkColor(be.getTextColor()) : 0;
            state.textLight = 15728880;
        } else {
            state.textColor = be.getDarkColor(be.getTextColor());
            state.outlineColor = 0;
            state.textLight = state.lightCoords;
        }
    }

    @Override
    public void submit(final NameplateRenderState state, final PoseStack ps, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        if (!state.visible) {
            return;
        }

        ps.pushPose();

        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180.0f));
        ps.translate(-0.5, -0.5, -0.5);

        ps.translate(1.0, 1.0, 1.0);

        // push 4 pixels out
        ps.translate(0.0, 0.0, -4.05 / 16.0);

        final int pixelsTall = state.pixelsTall;
        final int pixelsLeft = 3;

        ps.translate(-pixelsLeft / 16.0f, -(16.0 - pixelsTall) / 16.0 / 2.0, 0.0);
        ps.scale((float) (pixelsTall / 16.0), (float) (pixelsTall / 16.0), (float) (pixelsTall / 16.0));

        ps.scale(1 / 7f, 1 / 7f, 1 / 7f);

        ps.mulPose(Axis.ZP.rotationDegrees(180.0f));

        // translate to center
        ps.translate(state.centerPixels, 0.0, 0.0);

        for (final FormattedCharSequence sequence : state.sequences) {
            // a non-zero outline color draws the glowing 8x outline
            queue.submitText(ps, 0f /*x offset*/, 0f /*y offset*/, sequence, false, Font.DisplayMode.NORMAL, state.textLight, state.textColor, 0, state.outlineColor);
        }

        ps.popPose();
    }

    //taken from sign renderer
    private static boolean isOutlineVisible(final BlockPos blockPos, final int i) {
        if (i == DyeColor.BLACK.getTextColor()) {
            return true;
        } else {
            final Minecraft minecraft = Minecraft.getInstance();
            final LocalPlayer localPlayer = minecraft.player;
            if (localPlayer != null && minecraft.options.getCameraType().isFirstPerson() && localPlayer.isScoping()) {
                return true;
            } else {
                final Entity entity = minecraft.getCameraEntity();
                return entity != null && entity.distanceToSqr(Vec3.atCenterOf(blockPos)) < (double)OUTLINE_RENDER_DISTANCE;
            }
        }
    }

    public static class NameplateRenderState extends BlockEntityRenderState {
        public boolean visible;
        public Direction facing;
        public int pixelsTall;
        public double centerPixels;
        public List<FormattedCharSequence> sequences = List.of();
        public int textColor;
        public int outlineColor;
        public int textLight;
    }
}
