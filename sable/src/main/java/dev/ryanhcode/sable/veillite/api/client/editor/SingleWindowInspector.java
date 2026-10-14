/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.editor;

import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem;
import net.minecraft.network.chat.Component;

/**
 * Displays a single window as the editor. Automatically handles the close widget in the corner.
 */
public abstract class SingleWindowInspector implements Inspector {

    protected boolean open;

    @Override
    public void render() {
        // There is no ImGui on 1.21.11 (see VeilRenderSystem#hasImGui), so this is only reached by a future editor backend
        if (this.open) {
            this.renderComponents();
        } else {
            VeilRenderSystem.renderer().getEditorManager().hide(this);
        }
    }

    @Override
    public void onShow() {
        this.open = true;
    }

    /**
     * Adds all components inside the window.
     */
    protected abstract void renderComponents();

    /**
     * @return The title of the window
     */
    protected Component getWindowTitle() {
        return this.getDisplayName();
    }
}
