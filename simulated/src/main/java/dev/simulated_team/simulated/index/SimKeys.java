package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.client.AllKeys;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum SimKeys {
	ROTATE_MODE("rotate_mode", GLFW.GLFW_KEY_TAB, "Physics Staff Rotate Mode"),
	SCROLL_UP("scroll_up", InputConstants.UNKNOWN.getValue(), "Scroll Up"),
	SCROLL_DOWN("scroll_down", InputConstants.UNKNOWN.getValue(), "Scroll Down"),
	;

	/**
	 * Key binding category (1.21.9+ categories are registered ids; label key {@code key.category.simulated.simulated}).
	 * Upstream used the mod name as the category string.
	 */
	public static final String CATEGORY_NAME = "simulated";
	private static KeyMapping.Category category;

	private KeyMapping keybind;
	private final String description;
	private final String translation;
	private final int key;
	private final boolean modifiable;

	SimKeys(final int defaultKey) {
		this("", defaultKey, "");
	}

	SimKeys(final String description, final int defaultKey, final String translation) {
		this.description = Simulated.MOD_ID + ".keyinfo." + description;
		this.key = defaultKey;
		this.modifiable = !description.isEmpty();
		this.translation = translation;
	}

	public static void provideLang(final BiConsumer<String, String> consumer) {
		for (final SimKeys key : values())
			if (key.modifiable)
				consumer.accept(key.description, key.translation);
	}

	/**
	 * Creates the key mappings; pass the modifiable ones to the loader ({@code KeyBindingHelper::registerKeyBinding}).
	 */
	public static void registerTo(final Consumer<KeyMapping> consumer) {
		if (category == null) {
			category = KeyMapping.Category.register(Simulated.path(CATEGORY_NAME));
		}

		for (final SimKeys key : values()) {
			key.keybind = new KeyMapping(key.description, key.key, category);
			if (!key.modifiable)
				continue;

			consumer.accept(key.keybind);
		}
	}

	public KeyMapping getKeybind() {
		return this.keybind;
	}

	public boolean isPressed() {
		if (!this.modifiable)
			return AllKeys.isKeyDown(this.key);
		return this.keybind != null && this.keybind.isDown();
	}

	public String getBoundKey() {
		return this.keybind.getTranslatedKeyMessage()
			.getString()
			.toUpperCase();
	}
}
