package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;

/**
 * Client-only. Flywheel visual of the propeller bearing contraption (upstream: Create's {@code ContraptionVisual}, which
 * called the entity's {@code applyLocalTransforms}).
 */
public class PropellerBearingContraptionVisual extends ContraptionVisual<PropellerBearingContraptionEntity> {
    public PropellerBearingContraptionVisual(final VisualizationContext ctx, final PropellerBearingContraptionEntity entity, final float partialTick) {
        super(ctx, entity, partialTick);
    }

    @Override
    public void transform(final PoseStack contraptionMatrix, final float partialTicks) {
        PropellerBearingContraptionEntityRenderer.applyLocalTransforms(this.entity, contraptionMatrix, partialTicks);
    }
}
