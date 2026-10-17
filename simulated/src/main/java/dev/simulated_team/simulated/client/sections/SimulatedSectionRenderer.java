package dev.simulated_team.simulated.client.sections;

import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.mixin.accessor.CreativeModeInventoryScreenAccessor;
import dev.simulated_team.simulated.mixin_interface.SpriteContentsExtension;
import dev.simulated_team.simulated.mixin_interface.TickerExtension;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * Draws the section banners over the padded rows of the Simulated creative tab (client side of
 * {@link SimulatedCreativeTab}). Ported from SimulatedCreativeTab#renderBanners to the 1.21.11 GUI: no depth test or
 * shader colour, a 2D pose stack, and GUI-space scissors.
 */
public class SimulatedSectionRenderer {

	public static void renderBanners(final CreativeModeInventoryScreen screen, final GuiGraphics graphics, final int mouseX, final int mouseY) {
		final int left = ((CreativeModeInventoryScreenAccessor) screen).getLeftPos() + 8;
		final int top = ((CreativeModeInventoryScreenAccessor) screen).getTopPos() + 17;

		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);

		final List<SimulatedSection> sections = SimResourceManagers.SIMULATED_SECTION.sortedEntries();
		final Font font = Minecraft.getInstance().font;

		for (final SimulatedSection section : sections) {
			final Identifier id = SimResourceManagers.SIMULATED_SECTION.getId(section);
			final int yValue = SimulatedCreativeTab.SECTION_Y_VALUES.getInt(id);
			final int sectionRow = (yValue - SimulatedCreativeTab.CURRENT_ROW);
			if (sectionRow < 0 || sectionRow > 4) continue;

			final int x = 0;
			final int y = sectionRow * 18;
			final int w = 162;
			final int h = 18;

			final Identifier bannerTexture = section.sprite();

			if (section.animateOnHover()) {
				final boolean isHovering =
						mouseX >= left + x &&
								mouseX <= left + x + w &&
								mouseY >= top + y &&
								mouseY <= top + y + h;
				setPlaying(bannerTexture, isHovering);
			}

			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bannerTexture, x, y, w, h);

			final Component text = section.title().text();
			final int textWidth = font.width(text);

			graphics.fill(x + 2, y + 2, x + textWidth + 8, y + h - 2, section.title().background());

			final int light = section.title().color();
			// Veil's Colorc#darken(0.2): mix 20% black into every channel
			final int dark = section.title().secondaryColor()
					.orElse(ARGB.srgbLerp(0.2f, light, 0xFF000000));
			drawAuraText(graphics, text, dark, light, x + 5, y + 5);
		}

		graphics.pose().popMatrix();
	}

	/**
	 * Draws the text in {@code color1} with a shadow, then redraws its upper part (scissored to the top 1/1.8 of a
	 * line) in {@code color2}.
	 */
	public static void drawAuraText(final GuiGraphics graphics, final Component text, final int color1, final int color2, final int x, final int y) {
		final Font font = Minecraft.getInstance().font;

		graphics.drawString(font, text, x, y, color1, true);

		// 1.21.11 scissors take GUI coordinates and are transformed by the current pose
		graphics.enableScissor(x, y, x + font.width(text), y + (int) (font.lineHeight / 1.8f));
		graphics.drawString(font, text, x, y, color2, false);
		graphics.disableScissor();
	}

	public static void setPlaying(final Identifier identifier, final boolean playing) {
		final TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(identifier);
		final SpriteContents.AnimationState ticker = ((SpriteContentsExtension) sprite.contents()).simulated$getTicker();
		if (ticker instanceof final TickerExtension extension) {
			extension.simulated$setPlaying(playing);
		}
	}
}
