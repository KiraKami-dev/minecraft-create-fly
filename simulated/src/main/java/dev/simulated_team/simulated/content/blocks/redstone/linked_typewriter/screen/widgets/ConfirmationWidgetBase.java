package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.zurrtum.create.client.catnip.gui.element.ScreenElement;
import com.zurrtum.create.client.foundation.gui.widget.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class ConfirmationWidgetBase extends IconButton {

    public boolean confirmation;
    public MutableComponent message;

    public ConfirmationWidgetBase(final int x, final int y, final ScreenElement icon) {
        super(x, y, icon);
    }

    public <T extends ConfirmationWidgetBase> T withMessage(final MutableComponent component) {
        this.message = component;
        return (T) this;
    }

    @Override
    public void doRender(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);

        if (this.isHovered && this.visible && this.active && this.confirmation) {
            this.renderHoveredText(graphics, mouseX, mouseY);
        }
    }

    public void renderHoveredText(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(this.message.withColor(0xff0000)), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        // clicking anywhere else cancels the confirmation
        if (!this.isMouseOver(event.x(), event.y())) {
            this.confirmation = false;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.confirmation) {
            this.runCallback(event.x(), event.y());
            this.confirmation = false;
        } else {
            this.confirmation = true;
        }
    }
}
