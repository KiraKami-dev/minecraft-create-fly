package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.List;

/**
 * Draws the receiver and its two range plates in the {@link ModulatingLinkedReceiverScreen}.
 * <p>
 * On 1.21.1 the screen rendered the block and the plates straight into the GUI with depth testing. 1.21.11 GUIs are
 * 2D, so the three models are rendered together into one picture-in-picture texture (keeping their mutual occlusion).
 * Registered through {@code SimBlocksRedstoneClient#registerClient}.
 */
public class ModulatingLinkedReceiverGuiRenderer extends PictureInPictureRenderer<ModulatingLinkedReceiverGuiRenderer.State> {

    private int guiScale = 1;

    public ModulatingLinkedReceiverGuiRenderer(final MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public void prepare(final State state, final GuiRenderState guiRenderState, final int guiScale) {
        this.guiScale = guiScale;
        super.prepare(state, guiRenderState, guiScale);
    }

    @Override
    protected void renderToTexture(final State state, final PoseStack poseStack) {
        final Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        // Undo the vanilla picture-in-picture base transform (bottom centred, z flipped) so we work in GUI pixels
        // relative to the top left corner of the texture, y down and z towards the viewer, like the old GUI did.
        final float width = (state.x1() - state.x0()) * this.guiScale;
        final float height = (state.y1() - state.y0()) * this.guiScale;
        final float f = this.guiScale * state.scale();
        poseStack.scale(1 / f, 1 / f, -1 / f);
        poseStack.translate(-width / 2.0f, -height, 0);
        poseStack.scale(this.guiScale, this.guiScale, this.guiScale);

        poseStack.translate(state.originX() - state.x0(), state.originY() - state.y0(), 0);
        poseStack.scale(40, 40, 40);
        poseStack.mulPose(Axis.XP.rotationDegrees(-22));
        poseStack.mulPose(Axis.YP.rotationDegrees(63));
        // block models are y up
        poseStack.scale(1, -1, 1);

        final BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        final SinglePosVirtualBlockGetter level = SinglePosVirtualBlockGetter.createFullBright();
        level.blockState(state.blockState());

        dispatcher.renderBatched(state.blockState(), BlockPos.ZERO, level, poseStack,
                this.bufferSource.getBuffer(Sheets.cutoutBlockSheet()), false,
                dispatcher.getBlockModel(state.blockState()).collectParts(mc.level.getRandom()));

        final BlockState air = Blocks.AIR.defaultBlockState();
        final SinglePosVirtualBlockGetter emptyLevel = SinglePosVirtualBlockGetter.createFullBright();
        for (final float offset : new float[]{state.bottomOffset(), state.topOffset()}) {
            poseStack.pushPose();
            poseStack.translate(0, offset, 0);
            dispatcher.renderBatched(air, BlockPos.ZERO, emptyLevel, poseStack,
                    this.bufferSource.getBuffer(Sheets.cutoutBlockSheet()), false,
                    List.of(SimPartialModels.MODULATING_RECEIVER_PLATE.get()));
            poseStack.popPose();
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "modulating linked receiver";
    }

    /**
     * @param originX      GUI x of the block corner the model is rotated around
     * @param originY      GUI y of the block corner the model is rotated around
     * @param bottomOffset offset of the bottom plate along the receiver, in blocks
     * @param topOffset    offset of the top plate along the receiver, in blocks
     */
    public record State(BlockState blockState, float bottomOffset, float topOffset, float originX, float originY,
                        int x0, int y0, int x1, int y1, Matrix3x2f pose,
                        @Nullable ScreenRectangle scissorArea,
                        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

        public static State create(final BlockState blockState, final float bottomOffset, final float topOffset,
                                   final int originX, final int originY, final Matrix3x2f pose,
                                   @Nullable final ScreenRectangle scissorArea) {
            // the rotated block (and the plates) stay within this box around the origin corner
            final int x0 = originX - 8;
            final int y0 = originY - 58;
            final int x1 = originX + 62;
            final int y1 = originY + 14;
            ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            if (scissorArea != null) {
                bounds = scissorArea.intersection(bounds);
            }
            return new State(blockState, bottomOffset, topOffset, originX, originY, x0, y0, x1, y1, pose, scissorArea, bounds);
        }

        @Override
        public float scale() {
            return 1.0f;
        }
    }
}
