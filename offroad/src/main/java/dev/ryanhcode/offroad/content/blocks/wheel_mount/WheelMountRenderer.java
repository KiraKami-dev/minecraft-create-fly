package dev.ryanhcode.offroad.content.blocks.wheel_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

/**
 * Renders the wheel mount's shaft, telescope arm, suspension spring, steering diodes and the mounted tire.
 * PORT(1.21.11): split into extract (render state) and submit; every part is submitted as custom geometry at the pose
 * upstream rendered it with, and an item tire through its item render state. The wheel mount has no Flywheel visual,
 * so this renders the shaft even when Flywheel is on (upstream overrode {@code renderSafe} the same way).
 */
public class WheelMountRenderer extends KineticBlockEntityRenderer<WheelMountBlockEntity, WheelMountRenderer.WheelMountRenderState> {
    private final ItemModelResolver itemModelResolver;

    public WheelMountRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public WheelMountRenderState createRenderState() {
        return new WheelMountRenderState();
    }

    @Override
    public void extractRenderState(final WheelMountBlockEntity be, final WheelMountRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        state.support = false;
        this.updateBaseRenderState(be, state, be.getLevel(), crumblingOverlay);
        state.model = this.getRotatedModel(be, state);
        state.angle = getAngleForBe(be, state.blockPos, state.axis);

        final BlockState blockState = be.getBlockState();
        state.facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        state.diodeLeft = CachedBuffers.partial(OffroadPartialModels.DIODE_LEFT, blockState);
        state.diodeRight = CachedBuffers.partial(OffroadPartialModels.DIODE_RIGHT, blockState);
        state.teleOuter = CachedBuffers.partial(OffroadPartialModels.TELE_OUTER, blockState);
        state.teleInner = CachedBuffers.partial(OffroadPartialModels.TELE_INNER, blockState);
        state.teleMount = CachedBuffers.partial(OffroadPartialModels.TELE_MOUNT, blockState);
        state.springTop = CachedBuffers.partial(OffroadPartialModels.SPRING_UPPER, blockState);
        state.springBottom = CachedBuffers.partial(OffroadPartialModels.SPRING_LOWER, blockState);
        state.springMiddle = CachedBuffers.partial(OffroadPartialModels.SPRING_MIDDLE, blockState);

        state.extension = be.getLerpedExtension(partialTicks);
        state.yaw = be.getLerpedYaw(partialTicks);
        state.wheelAngle = be.getLerpedAngle(partialTicks);
        state.steeringLeft = be.clientSteeringSignalLeft;
        state.steeringRight = be.clientSteeringSignalRight;

        final ItemStack itemStack = be.getHeldItem();
        state.tire = itemStack.get(OffroadDataComponents.TIRE);
        state.tireModel = null;
        state.tireItem = null;
        if (state.tire != null) {
            if (state.tire.model().isPresent()) {
                state.tireModel = CachedBuffers.partial(PartialModel.of(state.tire.model().get()), state.blockState);
            } else {
                final ItemStackRenderState item = new ItemStackRenderState();
                this.itemModelResolver.updateForTopItem(item, itemStack, ItemDisplayContext.NONE, be.getLevel(), null, 0);
                state.tireItem = item;
            }
        }
    }

    @Override
    public void submit(final WheelMountRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, state.layer, state);

        final RenderType cutout = RenderTypes.cutoutMovingBlock();
        final int light = state.lightCoords;
        final Direction direction = state.facing;

        final double wheelPivotOffsetHor = 10.0 / 16.0;
        final double springWheelPivotOffsetHor = 12.0 / 16.0;
        final double springWheelPivotOffsetVer = -2.0 / 16.0;

        final double horizontalWheelPosition = 22.0 / 16.0;
        final double verticalWheelPosition = -state.extension;

        final double teleMountHor = 0.0 / 16.0;
        final double teleMountVer = -6.0 / 16.0;

        final double springMountHor = 7.0 / 16.0;
        final double springMountVer = 7.0 / 16.0;

        final double teleAngle = Math.atan2(verticalWheelPosition - teleMountVer, horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor);
        final double teleDistance = new Vector2d(verticalWheelPosition - teleMountVer, horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor).length();

        final double springAngle = Math.atan2(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer, horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor);
        final double springDistance = new Vector2d(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer, horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor).length();

        ms.pushPose();
        TransformStack.of(ms)
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(direction))
                .rotateXDegrees(AngleHelper.verticalAngle(direction))
                .uncenter();

        // wheel & telescope
        ms.pushPose();
        ms.pushPose();
        ms.translate(0.0, -6.0 / 16.0, 0.0);
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.XP.rotation((float) teleAngle));
        ms.translate(-0.5, -0.5, -0.5);
        queue.submitCustomGeometry(ms, cutout, (pose, vb) -> state.teleOuter.light(light).renderInto(pose, vb));
        ms.translate(0.0, 0.0, -(teleDistance - 1.0));
        queue.submitCustomGeometry(ms, cutout, (pose, vb) -> state.teleInner.light(light).renderInto(pose, vb));
        ms.popPose();

        ms.pushPose();
        ms.translate(0.0, verticalWheelPosition, 26.0 / 16.0 - horizontalWheelPosition);

        ms.translate(0.5, 0.5, 0.5);
        ms.rotateAround(Axis.YP.rotation((float) state.yaw), 0.0F, 0.0F, (float) (-horizontalWheelPosition + 6.0 / 16.0));
        ms.translate(-0.5, -0.5, -0.5);

        queue.submitCustomGeometry(ms, cutout, (pose, vb) -> state.teleMount.light(light).renderInto(pose, vb));

        ms.translate(0.5, 0.5, 0.5);
        ms.translate(0.0, 0.0, -26.0 / 16.0f);

        final double signMultiplier = -state.wheelAngle
                * (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : -1.0)
                * (direction.getAxis() == Direction.Axis.X ? 1.0 : -1.0);

        ms.mulPose(Axis.ZP.rotation((float) signMultiplier));

        final TireLike tireLike = state.tire;
        if (tireLike != null) {
            final Vec3 rotation = tireLike.rotation();
            ms.mulPose(Axis.XP.rotation((float) Math.toRadians(rotation.x)));
            ms.mulPose(Axis.YP.rotation((float) Math.toRadians(rotation.y)));
            ms.mulPose(Axis.ZP.rotation((float) Math.toRadians(rotation.z)));
            ms.translate(tireLike.offset().x, tireLike.offset().y, tireLike.offset().z);

            if (state.tireModel != null) {
                final SuperByteBuffer wheel = state.tireModel;
                queue.submitCustomGeometry(ms, cutout, (pose, vb) -> wheel.light(light)
                        .translate(-0.5f, 0.0f, -0.5f)
                        .renderInto(pose, vb));
            } else if (state.tireItem != null) {
                state.tireItem.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
            }
        }

        ms.popPose();

        ms.popPose();

        // spring
        ms.pushPose();
        ms.translate(0.5, 0.5 + springMountVer, 0.5 - springMountHor);
        ms.mulPose(Axis.XP.rotation((float) springAngle + Mth.PI / 2.0f));
        ms.translate(-0.5, -0.5 - springMountVer, -0.5 + springMountHor);

        final float springExtension = (float) springDistance;
        final float springSpan = springExtension - 4.0f / 16.0f;

        queue.submitCustomGeometry(ms, cutout, (pose, vb) -> {
            state.springTop.light(light).renderInto(pose, vb);
            state.springMiddle.light(light)
                    .translate(0.0f, 13.0f / 16.0f, 0.0f)
                    .scale(1.0f, springSpan / (14.0f / 16.0f), 1.0f)
                    .translateBack(0.0f, 13.0f / 16.0f, 0.0f)
                    .renderInto(pose, vb);
            state.springBottom.light(light)
                    .translate(0.0, -(springSpan + -14.0 / 16.0), 0.0)
                    .renderInto(pose, vb);
        });
        ms.popPose();

        queue.submitCustomGeometry(ms, cutout, (pose, vb) -> {
            state.diodeLeft.light(light)
                    .color(SimColors.redstone(state.steeringLeft / 15.0f))
                    .renderInto(pose, vb);
            state.diodeRight.light(light)
                    .color(SimColors.redstone(state.steeringRight / 15.0f))
                    .renderInto(pose, vb);
        });
        ms.popPose();
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    protected RenderType getRenderType(final WheelMountBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final WheelMountBlockEntity be, final WheelMountRenderState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), be.getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
    }

    public static class WheelMountRenderState extends KineticRenderState {
        public Direction facing;
        public SuperByteBuffer diodeLeft;
        public SuperByteBuffer diodeRight;
        public SuperByteBuffer teleOuter;
        public SuperByteBuffer teleInner;
        public SuperByteBuffer teleMount;
        public SuperByteBuffer springTop;
        public SuperByteBuffer springBottom;
        public SuperByteBuffer springMiddle;
        public double extension;
        public double yaw;
        public double wheelAngle;
        public int steeringLeft;
        public int steeringRight;
        @Nullable
        public TireLike tire;
        @Nullable
        public SuperByteBuffer tireModel;
        @Nullable
        public ItemStackRenderState tireItem;
    }
}
