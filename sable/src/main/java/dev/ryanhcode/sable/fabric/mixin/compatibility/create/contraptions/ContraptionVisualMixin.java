package dev.ryanhcode.sable.fabric.mixin.compatibility.create.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractEntityVisual;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.EmbeddedEnvironmentExtension;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the rendering of contraption visuals on sub-levels
 */
@Mixin(ContraptionVisual.class)
public abstract class ContraptionVisualMixin extends AbstractEntityVisual<AbstractContraptionEntity> {

    @Shadow(remap = false)
    @Final
    protected VisualEmbedding embedding;
    @Shadow(remap = false)
    @Final
    private PoseStack contraptionMatrix;

    public ContraptionVisualMixin(final VisualizationContext ctx, final AbstractContraptionEntity entity, final float partialTick) {
        super(ctx, entity, partialTick);
    }

    /**
     * Create Fly moved {@code AbstractContraptionEntity#applyLocalTransforms} into the (client-side) visual.
     */
    @Shadow(remap = false)
    public abstract void transform(PoseStack contraptionMatrix, float partialTick);

    @Inject(method = "setEmbeddingMatrices", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void sable$setEmbeddingMatrices(final float partialTick, final CallbackInfo ci) {
        final SubLevelContainer container = SubLevelContainer.getContainer(this.entity.level());

        if (container == null)
            return;

        final ChunkPos chunkPos = this.entity.chunkPosition();
        final boolean inBounds = container.inBounds(chunkPos);

        if (!inBounds)
            return;

        final int plotX = (chunkPos.x >> container.getLogPlotSize()) - container.getOrigin().x;
        final int plotZ = (chunkPos.z >> container.getLogPlotSize()) - container.getOrigin().y;

        final FlywheelCompat.SubLevelFlwRenderState state = FlywheelCompat.getInfo(ChunkPos.asLong(plotX, plotZ));

        if (state == null) return;

        final Vec3i origin = this.renderOrigin();

        final Vector3d pos = new Vector3d();
        if (this.entity.isPrevPosInvalid()) {
            pos.x = this.entity.getX();
            pos.y = this.entity.getY();
            pos.z = this.entity.getZ();
        } else {
            pos.x = Mth.lerp(partialTick, this.entity.xo, this.entity.getX());
            pos.y = Mth.lerp(partialTick, this.entity.yo, this.entity.getY());
            pos.z = Mth.lerp(partialTick, this.entity.zo, this.entity.getZ());
        }

        final ChunkPos centerChunk = state.centerChunk;
        final PoseStack sceneMatrix = new PoseStack();
        sceneMatrix.translate(
                (float) (pos.x - centerChunk.getMinBlockX()),
                (float) pos.y,
                (float) (pos.z - centerChunk.getMinBlockZ())
        );
        this.transform(sceneMatrix, partialTick);

        final Pose3dc renderPose = state.renderPose;
        renderPose.transformPosition(pos).sub(origin.getX(), origin.getY(), origin.getZ());

        this.contraptionMatrix.setIdentity();
        this.contraptionMatrix.translate(pos.x, pos.y, pos.z);
        this.contraptionMatrix.mulPose(new Quaternionf(renderPose.orientation()));
        this.transform(this.contraptionMatrix, partialTick);
        this.embedding.transforms(this.contraptionMatrix.last().pose(), this.contraptionMatrix.last().normal());

        if (this.embedding instanceof final EmbeddedEnvironmentExtension extension) {
            extension.sable$setLightingInfo(sceneMatrix.last().pose(), state.sceneID, state.latestSkyLightScale / 15.0f);
        }
        ci.cancel();
    }
}
