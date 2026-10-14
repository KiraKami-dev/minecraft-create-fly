package dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.ArrayList;

/**
 * An (always empty) shadow piece list handed to the vanilla shadow submit, carrying extra free-form shadow quads
 * for sub-levels. Vanilla's shadow feature renderer iterates it as a no-op; Sable's ShadowFeatureRenderer mixin draws the quads.
 * Vertices are stored as {@code x, y, z, u, v, alpha(0-255)}, four per quad, relative to the entity's render origin.
 */
public class SubLevelShadowList extends ArrayList<EntityRenderState.ShadowPiece> {

    public final ArrayList<float[]> vertices = new ArrayList<>();

    public void addVertex(final int alpha, final float x, final float y, final float z, final float u, final float v) {
        this.vertices.add(new float[]{x, y, z, u, v, alpha});
    }
}
