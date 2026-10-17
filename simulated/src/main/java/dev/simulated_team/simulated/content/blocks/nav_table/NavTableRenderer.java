package dev.simulated_team.simulated.content.blocks.nav_table;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.RenderableNavigationTarget;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class NavTableRenderer extends SmartBlockEntityRenderer<NavTableBlockEntity, NavTableRenderer.NavTableRenderState> {
    public NavTableRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public NavTableRenderState createRenderState() {
        return new NavTableRenderState();
    }

    @Override
    public void extractRenderState(final NavTableBlockEntity navBE, final NavTableRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(navBE, state, partialTicks, cameraPos, crumblingOverlay);

        final BlockState navState = state.blockState;
        if (navBE.isRemoved() || !(navState.getBlock() instanceof NavTableBlock)) {
            return;
        }

        final Direction facing = navState.getValue(NavTableBlock.FACING);
        state.facing = facing;
        state.arrowAngle = (float) (navBE.getClientTargetAngle(partialTicks) - Math.PI / 2);

        // Redstone Indicators and Pointer (drawn by NavTableVisual when Flywheel is on)
        if (!VisualizationManager.supportsVisualization(navBE.getLevel())) {
            state.indicator = CachedBuffers.partial(SimPartialModels.NAV_TABLE_INDICATOR, navState);
            state.pointer = CachedBuffers.partial(SimPartialModels.NAV_TABLE_POINTER, navState);

            final Vector3f logicalDirectionF = new Vector3f();
            for (int i = 0; i < SimDirectionUtil.Y_AXIS_PLANE.length; i++) {
                final Direction direction = SimDirectionUtil.Y_AXIS_PLANE[i];
                facing.getRotation().transform(direction.getStepX(), direction.getStepY(), direction.getStepZ(), logicalDirectionF);
                final Direction logicalDirection = Direction.getApproximateNearest(logicalDirectionF.x, logicalDirectionF.y, logicalDirectionF.z);

                final float signalStrength = navBE.isPowering ? Math.max(navBE.getRedstoneStrength(logicalDirection), 0) / 15.0F : 0;
                state.indicatorColors[i] = SimColors.redstone(signalStrength); // Analog indicators (mixes between colors smoothly)
            }
        }

        //keep item rendering outside of visual instances to allow for more flexability of custom renderers
        final ItemStack heldItem = navBE.getHeldItem();
        if (heldItem.isEmpty()) {
            return;
        }

        final ItemStackRenderState itemState = new ItemStackRenderState();
        this.itemModelManager.updateForTopItem(itemState, heldItem, ItemDisplayContext.FIXED, navBE.getLevel(), null, 0);
        // BakedModel#isGui3d is gone: block models are the ones lit like blocks
        state.blockItem = itemState.usesBlockLight();

        if (heldItem.getItem() instanceof final RenderableNavigationTarget rnti) {
            state.item = rnti.extractInNavTable(heldItem, navBE, navState, partialTicks, itemState);
        } else {
            state.rotateWithArrow = heldItem.is(SimTags.Items.ROTATE_WITH_NAV_ARROW);
            state.item = (ms, queue, light, overlay) -> itemState.submit(ms, queue, light, overlay, 0);
        }
    }

    @Override
    public void submit(final NavTableRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        if (state.facing == null) {
            return;
        }

        // Begin pose stack manipulation
        TransformStack.of(ms)
                .pushPose().center()
                .rotate(state.facing.getRotation());

        // Render Redstone Indicators
        if (state.indicator != null) {
            ms.pushPose();
            ms.translate(0, -0.5, 0);
            queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state::renderIndicators);
            ms.popPose();

            // Render Pointer
            ms.pushPose();
            ms.translate(0, 0.3, 0);
            queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state::renderPointer);
            ms.popPose();
        }

        // Render Nav table's item
        if (state.item != null) {
            ms.pushPose();
            ms.translate(0, 0.3, 0);
            TransformStack.of(ms)
                    .translate(0, state.blockItem ? 0.25f : 0.15f, 0)
                    .rotate((float) Math.toRadians(90f), Direction.WEST)
                    .scale(state.blockItem ? 0.5f : 0.375f);

            if (state.rotateWithArrow)
                ms.mulPose(Axis.ZP.rotation(state.arrowAngle));
            state.item.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY);
            ms.popPose();
        }

        ms.popPose();
    }

    public static class NavTableRenderState extends SmartBlockEntityRenderer.SmartRenderState {
        public Direction facing;
        public float arrowAngle;
        @Nullable
        public SuperByteBuffer indicator;
        @Nullable
        public SuperByteBuffer pointer;
        public final int[] indicatorColors = new int[SimDirectionUtil.Y_AXIS_PLANE.length];
        @Nullable
        public RenderableNavigationTarget.NavTableItemRender item;
        public boolean blockItem;
        public boolean rotateWithArrow;

        private void renderIndicators(final PoseStack.Pose pose, final VertexConsumer vb) {
            for (int i = 0; i < SimDirectionUtil.Y_AXIS_PLANE.length; i++) {
                this.indicator.rotateToFace(SimDirectionUtil.Y_AXIS_PLANE[i]);
                this.indicator.translate(0, 0, 0.5);
                this.indicator.light(this.lightCoords)
                        .color(this.indicatorColors[i])
                        .renderInto(pose, vb);
            }
        }

        private void renderPointer(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.pointer.rotateY(this.arrowAngle);
            this.pointer.light(this.lightCoords).renderInto(pose, vb);
        }
    }
}
