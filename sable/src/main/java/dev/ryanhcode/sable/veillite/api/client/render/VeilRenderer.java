/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.render;

import dev.ryanhcode.sable.veillite.api.client.editor.EditorManager;

/**
 * The minimal subset of Veil's renderer that Sable uses.
 */
public final class VeilRenderer {

    private final EditorManager editorManager = new EditorManager();

    VeilRenderer() {
    }

    public EditorManager getEditorManager() {
        return this.editorManager;
    }
}
