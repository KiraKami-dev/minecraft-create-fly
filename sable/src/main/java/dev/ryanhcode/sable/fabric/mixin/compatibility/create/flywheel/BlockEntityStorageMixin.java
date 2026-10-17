package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visual.BlockEntityVisual;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.BlockEntityStorage;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.Storage;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.BlockEntityStorageExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.Map;

/**
 * Redirects {@link BlockEntityVisual BlockEntityVisuals} of block entities in a sub-level's plot into a
 * {@link VisualEmbedding} that follows the sub-level's render pose.
 */
@Mixin(value = BlockEntityStorage.class, remap = false)
public abstract class BlockEntityStorageMixin extends Storage<BlockEntity> implements BlockEntityStorageExtension {

    @Unique
    private final Map<ClientSubLevel, SubLevelEmbedding> sable$subLevelEmbeddings = new Object2ObjectOpenHashMap<>();

    @Override
    public void sable$preFlywheelFrame(final ClientLevel level, final float partialTick) {
        final Iterator<Map.Entry<ClientSubLevel, SubLevelEmbedding>> iter = this.sable$subLevelEmbeddings.entrySet().iterator();

        while (iter.hasNext()) {
            final Map.Entry<ClientSubLevel, SubLevelEmbedding> entry = iter.next();
            final ClientSubLevel subLevel = entry.getKey();
            final SubLevelEmbedding subLevelEmbedding = entry.getValue();

            if (subLevel.isRemoved()) {
                final VisualizationManager manager = VisualizationManager.get(level);

                if (manager != null) {
                    for (final BlockEntity blockEntity : subLevelEmbedding.blockEntities()) {
                        manager.blockEntities().queueRemove(blockEntity);
                    }
                }

                subLevelEmbedding.embedding().delete();
                iter.remove();
                continue;
            }

            this.sable$updateEmbeddingTransforms(subLevel, subLevelEmbedding, partialTick);
        }
    }

    /**
     * Moves a {@link VisualEmbedding} to its {@link ClientSubLevel}'s render pose
     */
    @Unique
    private void sable$updateEmbeddingTransforms(final ClientSubLevel subLevel, final SubLevelEmbedding subLevelEmbedding, final float partialTick) {
        final VisualEmbedding embedding = subLevelEmbedding.embedding();
        final Pose3dc renderPose = subLevel.renderPose(partialTick);
        final Vector3dc rotationPoint = renderPose.rotationPoint();
        final Vector3dc position = renderPose.position();

        final Vec3i parentOrigin = subLevelEmbedding.parent().renderOrigin();
        final Vec3i localOrigin = embedding.renderOrigin();

        final Matrix4f transformation = new Matrix4f();
        transformation.setTranslation((float) (position.x() - parentOrigin.getX()), (float) (position.y() - parentOrigin.getY()), (float) (position.z() - parentOrigin.getZ()));
        transformation.rotate(new Quaternionf(renderPose.orientation()));
        transformation.translate((float) (localOrigin.getX() - rotationPoint.x()), (float) (localOrigin.getY() - rotationPoint.y()), (float) (localOrigin.getZ() - rotationPoint.z()));

        embedding.transforms(transformation, transformation.normal(new Matrix3f()));
    }

    @Unique
    private VisualEmbedding sable$getOrCreateSubLevelEmbedding(final VisualizationContext visualizationContext, final ClientSubLevel subLevel, final float partialTick) {
        final SubLevelEmbedding existing = this.sable$subLevelEmbeddings.get(subLevel);

        if (existing != null) {
            return existing.embedding();
        }

        final VisualEmbedding embedding = visualizationContext.createEmbedding(subLevel.getPlot().getCenterBlock());
        final SubLevelEmbedding created = new SubLevelEmbedding(embedding, visualizationContext, new ObjectArrayList<>());

        this.sable$subLevelEmbeddings.put(subLevel, created);
        this.sable$updateEmbeddingTransforms(subLevel, created, partialTick);

        return embedding;
    }

    @WrapOperation(method = "createRaw", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;"))
    private BlockEntityVisual<?> sable$createVisual(final BlockEntityVisualizer<?> instance, final VisualizationContext visualizationContext, final BlockEntity blockEntity, final float partialTick, final Operation<BlockEntityVisual<?>> original) {
        final SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);

        if (!(subLevel instanceof final ClientSubLevel clientSubLevel)) {
            return original.call(instance, visualizationContext, blockEntity, partialTick);
        }

        final VisualEmbedding embedding = this.sable$getOrCreateSubLevelEmbedding(visualizationContext, clientSubLevel, partialTick);
        final BlockEntityVisual<?> visual = original.call(instance, embedding, blockEntity, partialTick);
        this.sable$subLevelEmbeddings.get(clientSubLevel).blockEntities().add(blockEntity);

        return visual;
    }

    @Override
    public void remove(final BlockEntity blockEntity) {
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockEntity);

        if (subLevel != null && this.sable$subLevelEmbeddings.containsKey(subLevel)) {
            this.sable$subLevelEmbeddings.get(subLevel).blockEntities().remove(blockEntity);
        }

        super.remove(blockEntity);
    }

    @Override
    public void recreateAll(final VisualizationContext visualizationContext, final float partialTick) {
        this.sable$subLevelEmbeddings.clear();
        super.recreateAll(visualizationContext, partialTick);
    }
}
