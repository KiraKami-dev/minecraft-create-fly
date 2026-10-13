package dev.ryanhcode.sable.sublevel.render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3dc;

import java.util.Objects;

/**
 * Renders a {@link dev.ryanhcode.sable.sublevel.SubLevel} that consists of a single block without compiling a section.
 * <p>
 * On 1.21.11 the block is submitted as a vanilla moving block (like a piston-moved block), using a pose that applies the
 * sub-level transform.
 */
public class VanillaSingleSubLevelRenderData implements SubLevelRenderData {

    private static final Matrix4f TRANSFORM = new Matrix4f();

    /**
     * The sub-level this renderer is for
     */
    private final ClientSubLevel subLevel;

    /**
     * The render state submitted to the feature renderer. One per sub-level, since submits are deferred.
     */
    private final SingleBlockSubLevelWrapper renderState = new SingleBlockSubLevelWrapper();

    /**
     * The cached block state for single block rendering
     */
    private BlockState singleBlockState = null;

    /**
     * The cached block position for single block rendering
     */
    private BlockPos singleBlockPos = null;

    /**
     * The block entity to extract for rendering, if it isn't rendered globally
     */
    private BlockEntity singleBlockEntity = null;

    /**
     * Creates a new renderer for the given sub-level
     *
     * @param subLevel the sub-level to render
     */
    public VanillaSingleSubLevelRenderData(final ClientSubLevel subLevel) {
        this.subLevel = subLevel;
        this.rebuild();
    }

    private <E extends BlockEntity> void handleBlockEntity(@Nullable final E blockEntity) {
        if (Objects.equals(this.singleBlockEntity, blockEntity)) {
            return;
        }

        if (blockEntity == null) {
            this.singleBlockEntity = null;
            return;
        }

        final BlockEntityRenderer<E, ?> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);

        // Block entities that render off-screen are tracked by the level itself (ClientLevel#getGloballyRenderedBlockEntities)
        this.singleBlockEntity = blockEntityRenderer == null || blockEntityRenderer.shouldRenderOffScreen() ? null : blockEntity;
    }

    /**
     * Submits the block to the feature renderer.
     */
    public void submitSingleBlock(final PoseStack poseStack, final SubmitNodeCollector collector, final double camX, final double camY, final double camZ) {
        if (this.singleBlockState.isAir()) {
            this.rebuild();
        }

        if (this.singleBlockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        final Pose3dc renderPose = this.subLevel.renderPose();
        final Vector3dc renderPos = renderPose.position();
        final Vector3dc rotationPoint = renderPose.rotationPoint();
        this.renderState.setup(this.subLevel.getLevel(), renderPos.x(), renderPos.y(), renderPos.z(), this.singleBlockPos, this.singleBlockState);

        poseStack.pushPose();
        poseStack.mulPose(this.getTransformation(camX, camY, camZ, TRANSFORM));
        poseStack.translate(this.singleBlockPos.getX() - rotationPoint.x(), this.singleBlockPos.getY() - rotationPoint.y(), this.singleBlockPos.getZ() - rotationPoint.z());
        collector.submitMovingBlock(poseStack, this.renderState);
        poseStack.popPose();
    }

    public @Nullable BlockEntity getRenderBlockEntity() {
        if (this.singleBlockState.isAir()) {
            this.rebuild();
        }
        return this.singleBlockEntity;
    }

    @Override
    public void rebuild() {
        final BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();
        final BlockPos pos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());

        final BlockState blockState = this.subLevel.getLevel().getBlockState(pos);

        this.singleBlockState = blockState;
        this.singleBlockPos = pos;

        this.handleBlockEntity(blockState.hasBlockEntity() ? this.subLevel.getLevel().getBlockEntity(pos) : null);

        if (this.singleBlockEntity != null) {
            SableSubLevelRenderPlatform.INSTANCE.tryAddFlywheelVisual(this.singleBlockEntity);
        }
    }

    @Override
    public void compileSections(final PrioritizeChunkUpdates chunkUpdates, final RenderRegionCache renderRegionCache, final Camera camera) {
    }

    @Override
    public int getVisibleSectionCount() {
        return 1;
    }

    @Override
    public ClientSubLevel getSubLevel() {
        return this.subLevel;
    }

    @Override
    public void setDirty(final int x, final int y, final int z, final boolean playerChanged) {
        this.rebuild();
    }

    @Override
    public boolean isSectionCompiled(final int x, final int y, final int z) {
        return true;
    }

    @Override
    public void close() {
        this.singleBlockEntity = null;
    }
}
