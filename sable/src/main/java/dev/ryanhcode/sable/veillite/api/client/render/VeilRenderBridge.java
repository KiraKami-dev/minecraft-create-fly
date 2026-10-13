/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.veillite.impl.client.render.PoseStackMatrixStack;

/**
 * Converts vanilla render objects to their veillite counterparts.
 */
public final class VeilRenderBridge {

    private VeilRenderBridge() {
    }

    /**
     * Wraps a vanilla pose stack. All operations are applied to the wrapped stack.
     *
     * @param poseStack The stack to wrap
     * @return A matrix stack view of the pose stack
     */
    public static MatrixStack create(final PoseStack poseStack) {
        return new PoseStackMatrixStack(poseStack);
    }
}
