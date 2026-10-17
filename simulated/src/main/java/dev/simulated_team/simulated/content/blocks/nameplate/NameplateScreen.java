package dev.simulated_team.simulated.content.blocks.nameplate;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;

public class NameplateScreen extends Screen {
   public static final int MAX_WIDTH = 8 * 16 - 6;
   private final NameplateBlockEntity be;
   private String message;
   private int frame;
   @Nullable
   private TextFieldHelper nameField;

   private Button button;

   public NameplateScreen(final NameplateBlockEntity pSign) {
      this(pSign, SimLang.translate("nameplate.edit").component());
   }

   public NameplateScreen(final NameplateBlockEntity pSign, final Component pTitle) {
      super(pTitle);
      this.be = pSign;
      this.message = pSign.getName();
   }

   @Override
   protected void init() {
       this.button = this.addWidget(Button.builder(CommonComponents.GUI_DONE,
              (button) -> this.onDone()).bounds(this.width / 2 - 100,
              this.height / 4 + 144,
              200,
              20).build());

      this.nameField = new TextFieldHelper(() -> this.message,
              this::setMessage,
              TextFieldHelper.createClipboardGetter(this.minecraft),
              TextFieldHelper.createClipboardSetter(this.minecraft),
              (string) -> this.minecraft.font.width(string) <= MAX_WIDTH);
   }

   private void setMessage(final String s) {
      this.message = s;
   }

   @Override
   public void tick() {
      ++this.frame;
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft != null &&
              this.minecraft.player != null &&
              !this.be.isRemoved() &&
              NameplateBlockEntity.canPlayerReach(this.be, this.minecraft.player);
   }

   @Override
   public boolean keyPressed(final KeyEvent keyEvent) {
      final int pKeyCode = keyEvent.key();
      if (pKeyCode != 264 && pKeyCode != 257 && pKeyCode != 335) {
         return this.nameField.keyPressed(keyEvent) || super.keyPressed(keyEvent);
      }
      return false;
   }

   @Override
   public boolean charTyped(final CharacterEvent characterEvent) {
      this.nameField.charTyped(characterEvent);
      return true;
   }

   @Override
   public void render(final GuiGraphics gui, final int pMouseX, final int pMouseY, final float pPartialTick) {
      // the background is drawn by Screen#renderWithTooltipAndSubtitles on 1.21.11
      gui.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFFFF);

      this.renderSign(gui);

      // the button is not a renderable widget, it is drawn here
      this.button.render(gui, pMouseX, pMouseY, pPartialTick);
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public void removed() {
      VeilPacketManager.server().sendPacket(new NameplateChangeNamePacket(this.be.findController().getBlockPos(), this.message));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   protected void renderSignBackground(final GuiGraphics gui, final BlockState pState) {
      final String color = ((NameplateBlock) pState.getBlock()).getColor().getSerializedName();

      final Matrix3x2fStack ps = gui.pose();

      ps.pushMatrix();
      final float sy = 15.0f / 12.0f;
      ps.scale(sy, sy);
      ps.translate(8.0f - 16.0f * 4, 5.7f);

      gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 0.0F, 12, 16, 10, 32, 32);

      for (int i = 0; i < 6; i++) {
         ps.translate(16.0f, 0.0f);
         gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8,  8, 12, 16, 10, 32, 32);
      }
      ps.translate(16.0f, 0.0f);
      gui.blit(RenderPipelines.GUI_TEXTURED, Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 16, 12, 16, 10, 32, 32);

      ps.popMatrix();
   }

   protected void offsetSign(final GuiGraphics pGuiGraphics, final BlockState pState) {
      pGuiGraphics.pose().translate((float)this.width / 2.0F, this.height / 2f - 26);
   }

   private void renderSign(final GuiGraphics pGuiGraphics) {
      final Matrix3x2fStack ps = pGuiGraphics.pose();

      ps.pushMatrix();

      final BlockState blockstate = this.be.getBlockState();
      pGuiGraphics.pose().pushMatrix();
      this.offsetSign(pGuiGraphics, blockstate);
      final float scale = 2.0f;
      ps.scale(scale, scale);
      pGuiGraphics.pose().pushMatrix();
      this.renderSignBackground(pGuiGraphics, blockstate);
      pGuiGraphics.pose().popMatrix();
      this.renderSignText(pGuiGraphics);
      pGuiGraphics.pose().popMatrix();

      ps.popMatrix();
   }

   private void renderSignText(final GuiGraphics pGuiGraphics) {
      final int lineHeight = 8;

      final int color = this.be.getDarkColor(this.be.getTextColor());

      final boolean cursorFlash = this.frame / 6 % 2 == 0;
      final int cursorPos = this.nameField.getCursorPos();
      final int selectionPos = this.nameField.getSelectionPos();

      if (this.message != null) {
         if (this.font.isBidirectional()) {
             this.message = this.font.bidirectionalShaping(this.message);
         }

         final int w = -this.font.width(this.message) / 2;
         pGuiGraphics.drawString(this.font, this.message, w, 0, color, false);
         if (cursorPos >= 0 && cursorFlash) {
            final int l1 = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
            final int i2 = l1 - this.font.width(this.message) / 2;
            if (cursorPos >= this.message.length()) {
               pGuiGraphics.drawString(this.font, "_", i2, 0, color, false);
            }
         }
      }

      if (this.message != null && cursorPos >= 0) {
         final int width = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
         final int cen = width - this.font.width(this.message) / 2;
         if (cursorFlash && cursorPos < this.message.length()) {
            pGuiGraphics.fill(cen, -1, cen + 1, lineHeight, -16777216 | color);
         }

         if (selectionPos != cursorPos) {
            final int min = Math.min(cursorPos, selectionPos);
            final int max = Math.max(cursorPos, selectionPos);
            final int minWidth = this.font.width(this.message.substring(0, min)) - this.font.width(this.message) / 2;
            final int maxWith = this.font.width(this.message.substring(0, max)) - this.font.width(this.message) / 2;
            final int selMin = Math.min(minWidth, maxWith);
            final int selMax = Math.max(minWidth, maxWith);
            pGuiGraphics.textHighlight(selMin, -1, selMax, lineHeight, true);
         }
      }
   }

   private void onDone() {
      this.minecraft.setScreen(null);
   }

   public static void setScreen(final NameplateBlockEntity be) {
      if (be != null && NameplateBlockEntity.canPlayerReach(be, Minecraft.getInstance().player)) {
         final NameplateScreen screen = new NameplateScreen(be.findController());
         Minecraft.getInstance().setScreen(screen);
      }
   }
}