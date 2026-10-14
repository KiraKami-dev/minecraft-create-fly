/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps track of registered debug inspectors.
 * <p>
 * PORT-TODO(1.21.11): Veil's ImGui editor overlay does not exist on 1.21.11. Inspectors are tracked here, but nothing
 * draws them yet ({@link dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem#hasImGui()} is always false).
 */
public class EditorManager {

    private final List<Inspector> inspectors = new ArrayList<>();
    private final List<Inspector> visible = new ArrayList<>();

    public void add(final Inspector inspector) {
        this.inspectors.add(inspector);
    }

    public void remove(final Inspector inspector) {
        this.hide(inspector);
        this.inspectors.remove(inspector);
    }

    public void show(final Inspector inspector) {
        if (!this.visible.contains(inspector)) {
            this.visible.add(inspector);
            inspector.onShow();
        }
    }

    public void hide(final Inspector inspector) {
        if (this.visible.remove(inspector)) {
            inspector.onHide();
        }
    }

    public boolean isVisible(final Inspector inspector) {
        return this.visible.contains(inspector);
    }

    public List<Inspector> getInspectors() {
        return Collections.unmodifiableList(this.inspectors);
    }
}
