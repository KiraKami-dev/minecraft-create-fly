package dev.simulated_team.simulated.content.blocks.redstone;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * A textured GUI quad with a color (and alpha) per corner. Replaces the immediate-mode
 * {@code Tesselator}/{@code BufferUploader} quads the GUIs drew on 1.21.1.
 */
public record GradientBlitRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                      float x0, float y0, float x1, float y1,
                                      float u0, float u1, float v0, float v1,
                                      int topLeft, int topRight, int bottomLeft, int bottomRight,
                                      @Nullable ScreenRectangle scissorArea,
                                      @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    /**
     * Submits a textured quad to the given graphics.
     *
     * @param texture The texture, UVs are normalized
     */
    public static void submit(final GuiGraphics graphics, final Identifier texture,
                              final float x0, final float y0, final float x1, final float y1,
                              final float u0, final float u1, final float v0, final float v1,
                              final int topLeft, final int topRight, final int bottomLeft, final int bottomRight) {
        final AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        final TextureSetup setup = TextureSetup.singleTexture(abstractTexture.getTextureView(), abstractTexture.getSampler());
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final ScreenRectangle scissor = graphics.scissorStack.peek();

        ScreenRectangle bounds = new ScreenRectangle((int) Math.floor(x0), (int) Math.floor(y0),
                (int) Math.ceil(x1 - x0), (int) Math.ceil(y1 - y0)).transformMaxBounds(pose);
        if (scissor != null) {
            bounds = scissor.intersection(bounds);
        }

        graphics.guiRenderState.submitGuiElement(new GradientBlitRenderState(RenderPipelines.GUI_TEXTURED, setup, pose,
                x0, y0, x1, y1, u0, u1, v0, v1, topLeft, topRight, bottomLeft, bottomRight, scissor, bounds));
    }

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(this.u0, this.v0).setColor(this.topLeft);
        vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(this.u0, this.v1).setColor(this.bottomLeft);
        vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(this.u1, this.v1).setColor(this.bottomRight);
        vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(this.u1, this.v0).setColor(this.topRight);
    }
}
