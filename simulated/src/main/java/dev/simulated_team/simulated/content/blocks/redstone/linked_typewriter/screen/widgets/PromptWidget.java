package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.client.catnip.gui.widget.AbstractSimiWidget;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.EntryModifierScreen;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class PromptWidget extends AbstractSimiWidget {

    private final EntryModifierScreen entryModifierScreen;
    protected boolean bindingActive = false;

    public PromptWidget(final EntryModifierScreen entryModifierScreen, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.entryModifierScreen = entryModifierScreen;
    }

    @Override
    protected void doRender(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.doRender(graphics, mouseX, mouseY, partialTicks);

        if (this.entryModifierScreen.modifying && this.entryModifierScreen.psuedoEntry != null) {
            Component displayName = InputConstants.getKey(new KeyEvent(this.entryModifierScreen.psuedoEntry.glfwKeyCode, -1, 0)).getDisplayName();

            if (this.bindingActive) {
                displayName = SimLang.translate("linked_typewriter.bind_screen_prompt").component();
            } else if (this.entryModifierScreen.psuedoEntry.glfwKeyCode == -1) {
                displayName = SimLang.translate("linked_typewriter.bind_new_key").component();
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(3, 4);
            graphics.drawString(Minecraft.getInstance().font, displayName, this.getX(), this.getY(), 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.bindingActive ^= true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.bindingActive && this.entryModifierScreen.psuedoEntry != null) {
            this.entryModifierScreen.psuedoEntry.keyCode(event.key());
            this.bindingActive = false;
            return true;
        }

        this.bindingActive = false;
        return super.keyPressed(event);
    }
}
