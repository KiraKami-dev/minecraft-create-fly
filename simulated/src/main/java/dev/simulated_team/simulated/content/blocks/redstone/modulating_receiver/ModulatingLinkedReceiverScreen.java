package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.zurrtum.create.client.catnip.gui.AbstractSimiScreen;
import com.zurrtum.create.client.catnip.gui.ScreenOpener;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.client.foundation.gui.widget.IconButton;
import com.zurrtum.create.client.foundation.gui.widget.ScrollInput;
import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import dev.simulated_team.simulated.content.blocks.redstone.GradientBlitRenderState;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.network.packets.ConfigureModulatingLinkedRecieverPacket;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

public class ModulatingLinkedReceiverScreen extends AbstractSimiScreen {
    private final ModulatingLinkedReceiverBlockEntity be;
    private final SimGUITextures background;
    private IconButton confirmButton;
    private ScrollInput minScroll;
    private ScrollInput maxScroll;
    private int lastModification;

    public ModulatingLinkedReceiverScreen(final ModulatingLinkedReceiverBlockEntity be) {
        super(SimLang.translate("gui.modulating_linked_receiver.title").component());
        this.be = be;
        this.background = SimGUITextures.MODULATINGLINK;
        this.lastModification = -1;
    }

    public static void open(final ModulatingLinkedReceiverBlockEntity be) {
        ScreenOpener.open(new ModulatingLinkedReceiverScreen(be));
    }

    /**
     * @return Whether the screen for the receiver at the given position is currently open (client only)
     */
    public static boolean isEditing(final BlockPos pos) {
        return Minecraft.getInstance().screen instanceof final ModulatingLinkedReceiverScreen screen && screen.isThisBlock(pos);
    }

    public boolean isThisBlock(final BlockPos pos) {
        return this.be.getBlockPos().equals(pos);
    }

    @Override
    protected void init() {
        this.setWindowSize(this.background.width, this.background.height);
        this.setWindowOffset(-20, 0);
        super.init();

        final int x = this.guiLeft;
        final int y = this.guiTop;

        this.confirmButton =
                new IconButton(x + this.background.width - 33, y + this.background.height - 24, AllIcons.I_CONFIRM);
        this.confirmButton.withCallback(() -> {
            this.onClose();
        });
        this.addRenderableWidget(this.confirmButton);

        this.minScroll = new ScrollInput(x + 55, y + 47, 26, 16);
        this.maxScroll = new ScrollInput(x + 132, y + 47, 26, 16);

        this.minScroll.calling(value -> {
            this.be.minRange = value;
            this.be.maxRange = Math.max(this.be.maxRange, value);
            this.maxScroll.setState(this.be.maxRange);
            this.lastModification = 0;
        });
        this.maxScroll.calling(value -> {
            this.be.maxRange = value;
            this.be.minRange = Math.min(this.be.minRange, value);
            this.minScroll.setState(this.be.minRange);
            this.lastModification = 0;
        });


        this.minScroll.withRange(1, 257)
                .titled(SimLang.translate("gui.modulating_linked_receiver.minimum_range").component())
                .withShiftStep(10)
                .setState(this.be.minRange)
                .onChanged();
        this.maxScroll.withRange(1, 257)
                .titled(SimLang.translate("gui.modulating_linked_receiver.minimum_range").component())
                .withShiftStep(10)
                .setState(this.be.maxRange)
                .onChanged();
        this.addRenderableWidgets(this.minScroll);
        this.addRenderableWidgets(this.maxScroll);

    }

    public static int distanceGuiOffset(final float value, final float maxValue, final float width, final float smoothing) {
        return Math.round(
            (width * (value - 1) * (smoothing + maxValue - 1))
            / ((maxValue - 1) * (smoothing + value - 1))
        );
    }

    @Override
    protected void renderWindow(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final int x = this.guiLeft;
        final int y = this.guiTop;

        this.background.render(graphics, x, y);

        graphics.drawString(this.font, this.title, x + (this.background.width - 8) / 2 - this.font.width(this.title) / 2, y + 4, SimColors.TITLE_DARK_RED, false);

        int currentX = 22;

        this.label(graphics, currentX, 26 - 1, SimLang.translate("gui.modulating_linked_receiver.min").component());
        String text = Integer.toString(this.be.minRange);
        int stringWidth = this.font.width(text);
        this.label(graphics, currentX + 34 + (12 - stringWidth / 2), 26 - 1, Component.literal(text));

        currentX += 77;

        this.label(graphics, currentX, 26 - 1, SimLang.translate("gui.modulating_linked_receiver.max").component());
        text = Integer.toString(this.be.maxRange);
        stringWidth = this.font.width(text);
        this.label(graphics, currentX + 34 + (12 - stringWidth / 2), 26 - 1, Component.literal(text));

        final int bandStart = 37;
        final int bandEnd = 156;
        final int bandWidth = bandEnd - bandStart;
        final float smoothing = 20f;
        final float maxDistance = 256;

        final int minPos = bandStart + distanceGuiOffset(this.be.minRange, maxDistance, bandWidth, smoothing);
        final int maxPos = bandStart + distanceGuiOffset(this.be.maxRange, maxDistance, bandWidth, smoothing);

        final SimGUITextures sprite = SimGUITextures.MODULATINGLINK_POWERED_LANE;

        // fully powered up to the minimum range
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.location, x + bandStart + 1, y + 25, sprite.startX, sprite.startY,
                minPos - bandStart, sprite.height, 256, 256);

        // then fading out towards the maximum range
        final float imageSize = 256f;
        final float uvx1 = (sprite.startX + minPos - bandStart) / imageSize;
        final float uvx2 = (sprite.startX + maxPos - bandStart) / imageSize;
        final float uvy1 = sprite.startY / imageSize;
        final float uvy2 = (sprite.startY + sprite.height) / imageSize;

        final float px1 = (float) (x + minPos);
        final float px2 = (float) (x + maxPos);
        final float py1 = (float) (y + 25);
        final float py2 = (y + 25 + sprite.height);

        GradientBlitRenderState.submit(graphics, sprite.location, px1, py1, px2, py2, uvx1, uvx2, uvy1, uvy2,
                0xFFFFFFFF, 0x00FFFFFF, 0xFFFFFFFF, 0x00FFFFFF);

        SimGUITextures.MODULATINGLINK_MARKER.render(graphics, x + minPos, y + 23);
        SimGUITextures.MODULATINGLINK_MARKER.render(graphics, x + maxPos, y + 23);

        if (this.be.getClientDistance(partialTicks) < ModulatingLinkedReceiverBlockEntity.RANGE_LIMIT) {
            final int sourcePos = bandStart + distanceGuiOffset((float) this.be.getClientDistance(partialTicks), maxDistance, bandWidth, smoothing);
            SimGUITextures.MODULATINGLINK_TARGET.render(graphics, x + sourcePos, y + 16);
        }

        final float minPos2 = 5.5f * ((this.be.minRange - 1) * (smoothing + maxDistance - 1)) / ((maxDistance - 1) * (smoothing + this.be.minRange - 1));
        final float maxPos2 = 5.5f * ((this.be.maxRange - 1) * (smoothing + maxDistance - 1)) / ((maxDistance - 1) * (smoothing + this.be.maxRange - 1));

        // receiver and its plates, the plates move along the receiver like in the world
        graphics.guiRenderState.submitPicturesInPictureState(ModulatingLinkedReceiverGuiRenderer.State.create(
                this.be.getBlockState().setValue(ModulatingLinkedReceiverBlock.FACING, Direction.UP),
                minPos2 / 16.0f, (0.5f + maxPos2) / 16.0f,
                x + this.background.width + 4, y + this.background.height + 4,
                new Matrix3x2f(graphics.pose()), graphics.scissorStack.peek()));
    }

    private void label(final GuiGraphics graphics, final int x, final int y, final Component text) {
        graphics.drawString(this.font, text, this.guiLeft + x, this.guiTop + 26 + y, 0xFFFFFFEE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.lastModification >= 0)
            this.lastModification++;

        if (this.lastModification >= 20) {
            this.lastModification = -1;
            this.send();
        }
    }

    @Override
    public void removed() {
        this.send();
    }

    protected void send() {
        VeilPacketManager.server().sendPacket(new ConfigureModulatingLinkedRecieverPacket(this.be.getBlockPos(), this.minScroll.getState(), this.maxScroll.getState()));
    }

}
