package dev.ryanhcode.sable.veillite.impl.client;

import dev.ryanhcode.sable.veillite.api.client.editor.EditorManager;
import dev.ryanhcode.sable.veillite.api.client.editor.Inspector;
import foundry.imgui.fabric.api.event.RenderImGuiEventsFabric;

/**
 * Draws the shown inspectors through imguimc's ImGui frame. Only loaded when imguimc is present.
 */
public final class ImGuiEditorBackend {

    private ImGuiEditorBackend() {
    }

    public static void install(final EditorManager editorManager) {
        RenderImGuiEventsFabric.POST.register(() -> {
            for (final Inspector inspector : editorManager.getVisible()) {
                inspector.render();
            }
        });
    }
}
