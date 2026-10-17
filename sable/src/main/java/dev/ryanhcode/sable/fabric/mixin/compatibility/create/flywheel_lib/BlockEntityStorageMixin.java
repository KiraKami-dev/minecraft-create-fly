package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.flywheel.api.visual.BlockEntityVisual;
import com.zurrtum.create.client.flywheel.api.visual.LightUpdatedVisual;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.BlockEntityStorage;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.Storage;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.BlockEntityStorageExtension;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.EmbeddedEnvironmentExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;

/**
 * Adds compatibility with Flywheel by redirecting the addition of {@link BlockEntityVisual BlockEntityVisuals} to {@link VisualEmbedding VisualEmbeddings} if they reside in a data plot.
 */
@Mixin(BlockEntityStorage.class)
public abstract class BlockEntityStorageMixin extends Storage<BlockEntity> implements BlockEntityStorageExtension {

    // Storage for temporary pose values to avoid repeated allocation
    @Unique
    private final Quaternionf sable$orientationStorage = new Quaternionf();
    @Unique
    private final Vector3d sable$localOffsetStorage = new Vector3d();
    /**
     * Visual embeddings & info kept for every sub-level
     */
    @Unique
    private final Map<ClientSubLevel, SubLevelEmbedding> sable$subLevelEmbeddings = new Object2ObjectOpenHashMap<>();
    @Unique
    private final Matrix3f sable$normalMatStorage = new Matrix3f();
    @Shadow(remap = false)
    @Final
    private Long2ObjectMap<BlockEntityVisual<?>> posLookup;
    @Unique
    private VisualizationContext sable$planVisualizationContext;

    @Override
    public void sable$setPlanVisualizationContext(final VisualizationContext visualizationContext) {
        // TODO: we really shouldn't be storing visualization contexts like this
        this.sable$planVisualizationContext = visualizationContext;
    }

    @Override
    public SubLevelEmbedding sable$getEmbeddingInfo(final SubLevel subLevel) {
        if (!(subLevel instanceof final ClientSubLevel clientSubLevel)) {
            throw new IllegalArgumentException("SubLevel must be a ClientSubLevel");
        }

        return this.sable$subLevelEmbeddings.get(clientSubLevel);
    }

    @Override
    public void sable$preFlywheelFrame() {
        this.sable$updateSubLevelEmbeddingsFrame(this.sable$planVisualizationContext);
    }

    /**
     * Updates all sub-level embeddings, removing stale entries and updating transforms
     */
    @Unique
    private void sable$updateSubLevelEmbeddingsFrame(final VisualizationContext visualizationContext) {
        final Iterator<Map.Entry<ClientSubLevel, SubLevelEmbedding>> iter = this.sable$subLevelEmbeddings.entrySet().iterator();

        while (iter.hasNext()) {
            final Map.Entry<ClientSubLevel, SubLevelEmbedding> entry = iter.next();

            final SubLevelEmbedding subLevelEmbedding = entry.getValue();
            final ClientSubLevel subLevel = entry.getKey();

            if (subLevel.isRemoved()) {
                this.sable$onEmbeddingRemoved(subLevel);
                iter.remove();
                continue;
            }

            if (subLevel.getLatestSkyLightScale() != subLevelEmbedding.latestSkyLightScale()) {
                for (final BlockEntity be : subLevelEmbedding.blockEntities()) {
                    final BlockEntityVisual<?> visual = this.posLookup.get(be.getBlockPos().asLong());

                    if (visual instanceof final LightUpdatedVisual lightUpdatedVisual) {
                        lightUpdatedVisual.updateLight(0.0f);
                    }
                }

                subLevelEmbedding.setLatestSkyLightScale(subLevel.getLatestSkyLightScale());
            }

            this.sable$updateEmbeddingTransforms(visualizationContext, subLevel, subLevelEmbedding.embedding());
        }
    }

    @Unique
    private void sable$onEmbeddingRemoved(final ClientSubLevel subLevel) {
        final SubLevelEmbedding subLevelEmbedding = this.sable$subLevelEmbeddings.get(subLevel);

        final VisualizationManager manager = VisualizationManager.get(subLevel.getLevel());

        if (manager != null) {
            for (final BlockEntity blockEntity : subLevelEmbedding.blockEntities()) {
                manager.blockEntities().queueRemove(blockEntity);
            }
        }

        subLevelEmbedding.embedding().delete();
    }

    /**
     * Updates the transform a {@link VisualEmbedding} to its {@link ClientSubLevel}'s render pose
     */
    @Unique
    private void sable$updateEmbeddingTransforms(final VisualizationContext visualizationContext, final ClientSubLevel subLevel, final VisualEmbedding embedding) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
        assert container != null;

        final Pose3dc renderPose = subLevel.renderPose();
        final Vector3dc rotationPoint = renderPose.rotationPoint();
        final Vector3dc position = renderPose.position();

        final Matrix4f transformation = new Matrix4f();
        final Vec3i parentOrigin = visualizationContext.renderOrigin();

        transformation.setTranslation((float) (position.x() - parentOrigin.getX()), (float) (position.y() - parentOrigin.getY()), (float) (position.z() - parentOrigin.getZ()));
        transformation.rotate(this.sable$orientationStorage.set(renderPose.orientation()));

        final Vec3i localOrigin = embedding.renderOrigin();
        final Vector3d localOffset = rotationPoint.sub(localOrigin.getX(), localOrigin.getY(), localOrigin.getZ(), this.sable$localOffsetStorage);

        transformation.translate((float) -localOffset.x, (float) -localOffset.y, (float) -localOffset.z);

        final Matrix3f normal = transformation.normal(this.sable$normalMatStorage);
        embedding.transforms(transformation, normal);

        final PoseStack sceneMatrix = new PoseStack();
        final ChunkPos centerChunk = subLevel.getPlot().getCenterChunk();
        sceneMatrix.translate(
                (float) (localOrigin.getX() - centerChunk.getMinBlockX()),
                (float) localOrigin.getY(),
                (float) (localOrigin.getZ() - centerChunk.getMinBlockZ())
        );

        if (embedding instanceof final EmbeddedEnvironmentExtension embeddedEnvironment) {
            embeddedEnvironment.sable$setLightingInfo(sceneMatrix.last().pose(), container.getLightingSceneId(subLevel), subLevel.getLatestSkyLightScale() / 15.0f);
        }
    }

    @Unique
    private VisualEmbedding sable$getOrCreateSubLevelEmbedding(final VisualizationContext visualizationContext, final ClientSubLevel subLevel) {
        final SubLevelEmbedding existingSubLevelEmbedding = this.sable$subLevelEmbeddings.get(subLevel);

        if (existingSubLevelEmbedding != null) return existingSubLevelEmbedding.embedding();

        // Otherwise, we'll create a new embedding

        final VisualEmbedding newEmbedding = visualizationContext.createEmbedding(subLevel.getPlot().getCenterBlock());

        this.sable$subLevelEmbeddings.put(subLevel, new SubLevelEmbedding(newEmbedding, new ObjectArrayList<>(), subLevel.getLatestSkyLightScale()));
        this.sable$updateEmbeddingTransforms(visualizationContext, subLevel, newEmbedding);

        return newEmbedding;
    }

    @WrapOperation(method = "createRaw(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;",
            at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;"))
    public BlockEntityVisual<?> sable$createVisual(final BlockEntityVisualizer<BlockEntity> instance, final VisualizationContext visualizationContext, final BlockEntity blockEntity, final float partialTick, final Operation<BlockEntityVisual<?>> original) {
        final SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);

        if (subLevel == null) {
            return original.call(instance, visualizationContext, blockEntity, partialTick);
        }

        assert subLevel instanceof ClientSubLevel;
        final VisualEmbedding embedding = this.sable$getOrCreateSubLevelEmbedding(visualizationContext, ((ClientSubLevel) subLevel));

        final BlockEntityVisual<?> newVisual = original.call(instance, embedding, blockEntity, partialTick);
        this.sable$subLevelEmbeddings.get(subLevel).blockEntities().add(blockEntity);

        return newVisual;
    }

    /**
     * When a block entity is removed, we also need to remove it from the list that we're tracking for every sub-level.
     * (Upstream overrode {@code remove}; injecting keeps Flywheel's own {@code posLookup} bookkeeping intact.)
     */
    @Inject(method = "remove(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"))
    private void sable$remove(final BlockEntity blockEntity, final CallbackInfo ci) {
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockEntity);

        if (subLevel != null && this.sable$subLevelEmbeddings.containsKey(subLevel)) {
            this.sable$subLevelEmbeddings.get(subLevel).blockEntities().remove(blockEntity);
        }
    }

    @Inject(method = "recreateAll", at = @At("HEAD"), remap = false)
    private void sable$recreateAll(final VisualizationContext visualizationContext, final float partialTick, final CallbackInfo ci) {
        this.sable$subLevelEmbeddings.clear();
    }
}
