/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.editor;

import net.minecraft.network.chat.Component;

/**
 * A debug inspector window.
 */
public interface Inspector {

    /**
     * Draws the inspector. Only called while ImGui is available and the inspector is shown.
     */
    void render();

    /**
     * Called when the inspector becomes visible.
     */
    default void onShow() {
    }

    /**
     * Called when the inspector is hidden.
     */
    default void onHide() {
    }

    /**
     * @return The display name of this inspector
     */
    Component getDisplayName();
}
