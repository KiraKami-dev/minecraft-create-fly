package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.content.kinetics.base.DirectionalAxisKineticBlock;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Barrel, bellows and the loaded item are always drawn here; the shaft and cog only without Flywheel (the visual draws
 * them otherwise), so this renderer is registered alongside {@link MountedPotatoCannonVisual}.
 */
public class MountedPotatoCannonRenderer implements BlockEntityRenderer<MountedPotatoCannonBlockEntity, MountedPotatoCannonRenderer.MountedPotatoCannonRenderState> {

	private final ItemModelResolver itemModelResolver;

	public MountedPotatoCannonRenderer(final BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public MountedPotatoCannonRenderState createRenderState() {
		return new MountedPotatoCannonRenderState();
	}

	@Override
	public void extractRenderState(final MountedPotatoCannonBlockEntity be, final MountedPotatoCannonRenderState state, final float partialTicks,
								   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		this.extractComponents(be, state, partialTicks);
		this.extractItem(be, state, partialTicks);
	}

	private void extractComponents(final MountedPotatoCannonBlockEntity be, final MountedPotatoCannonRenderState state, final float partialTicks) {
		final BlockState blockState = state.blockState;

		state.drawParts = !VisualizationManager.supportsVisualization(be.getLevel());
		if (state.drawParts) {
			// upstream KineticBlockEntityRenderer.renderRotatingKineticBlock(be, shaft, ...)
			final Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(be);
			state.shaft = CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK, this.getRenderedBlockState(be));
			state.shaftAngle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
			state.shaftDirection = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
			state.shaftColor = KineticBlockEntityRenderer.getColor(be);

			state.cog = CachedBuffers.partial(AeroPartialModels.CANNON_COG, blockState);
			state.cogAngle = be.getCogwheelAngle(partialTicks);
		}

		//TODO Don't make this jank
		state.barrelOffset = !be.isBlocked() ? be.getBarrelDistance(partialTicks) : (float) -(be.getBlockedLength() / 2);
		state.bellowOffset = -be.getBellowDistance(partialTicks);

		state.barrel = CachedBuffers.partial(AeroPartialModels.CANNON_BARREL, blockState);
		state.bellow = CachedBuffers.partial(AeroPartialModels.CANNON_BELLOW, blockState);
	}

	private void extractItem(final MountedPotatoCannonBlockEntity be, final MountedPotatoCannonRenderState state, final float partialTicks) {
		state.item = null;
		if (be.getInventory().isEmpty()) {
			return;
		}

		// upstream ItemRenderer#renderStatic(stack, FIXED, light, overlay, ms, buffer, level, 0)
		final ItemStackRenderState item = new ItemStackRenderState();
		this.itemModelResolver.updateForTopItem(item, be.getInventory().slot.getStack(), ItemDisplayContext.FIXED, be.getLevel(), null, 0);
		state.item = item;

		final float normalizedTimer = be.getItemTime(partialTicks);
		float itemPosition = !be.isBlocked() ? 1 - (float) Math.exp(-0.25f * normalizedTimer) : 0;
		itemPosition *= 0.8f;
		state.itemPosition = itemPosition;

		final int itemRotationId = be.getItemRotationId();
		final Quaternionf Q = new Quaternionf((float) Math.sin(itemRotationId * 0.4f), (float) Math.cos(itemRotationId * 1.4f), (float) Math.sin(itemRotationId * 3.0f), (float) Math.cos(itemRotationId * 5.0f));
		Q.normalize();
		state.itemRotation = Q;
	}

	@Override
	public void submit(final MountedPotatoCannonRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
		queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state::renderComponents);
		this.submitItem(state, ms, queue);
	}

	private void submitItem(final MountedPotatoCannonRenderState state, final PoseStack ms, final SubmitNodeCollector queue) {
		if (state.item == null) {
			return;
		}

		final TransformStack<PoseTransformStack> msr = TransformStack.of(ms);
		ms.pushPose();
		msr.center();

		final Direction facing = state.blockState.getValue(BlockStateProperties.FACING);
		final Vec3i facingVec = facing.getUnitVec3i();

		final float itemScale = 0.35f;
		final float itemPosition = state.itemPosition;
		ms.translate(facingVec.getX() * itemPosition, facing.getStepY() * itemPosition, facingVec.getZ() * itemPosition);
		ms.scale(itemScale, itemScale, itemScale);
		msr.rotate(state.itemRotation);
		state.item.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static SuperByteBuffer transform(final SuperByteBuffer buffer, final BlockState state, final boolean axisDirectionMatters) {
		final Direction facing = state.getValue(BlockStateProperties.FACING);

		final float zRotLast = axisDirectionMatters && (state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z) ? 90 : 0;
		final float yRot = AngleHelper.horizontalAngle(facing);
		final float zRot = facing == Direction.UP ? (float) 90 : facing == Direction.DOWN ? 90 : 0;
		final float zRotSecondLast = facing == Direction.UP ? (float) 180 : 0;

		buffer.rotateCentered((float) ((zRot) / 180 * Math.PI), Direction.SOUTH);
		buffer.rotateCentered((float) ((zRot) / 180 * Math.PI), Direction.DOWN);
		buffer.rotateCentered((float) ((yRot) / 180 * Math.PI), Direction.UP);
		buffer.rotateCentered((float) ((zRotLast) / 180 * Math.PI), Direction.SOUTH);
		buffer.rotateCentered((float) ((zRotSecondLast) / 180 * Math.PI), Direction.UP);

		return buffer;
	}

	private BlockState getRenderedBlockState(final MountedPotatoCannonBlockEntity te) {
		return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(te));
	}

	public static class MountedPotatoCannonRenderState extends BlockEntityRenderState {
		public boolean drawParts;
		public SuperByteBuffer shaft;
		public float shaftAngle;
		public Direction shaftDirection;
		public Color shaftColor;
		public SuperByteBuffer cog;
		public float cogAngle;

		public SuperByteBuffer barrel;
		public SuperByteBuffer bellow;
		public float barrelOffset;
		public float bellowOffset;

		@Nullable
		public ItemStackRenderState item;
		public float itemPosition;
		public Quaternionf itemRotation;

		/**
		 * Upstream {@code renderComponents}; the cached buffers are shared, so all transforms are applied here
		 */
		public void renderComponents(final PoseStack.Pose pose, final VertexConsumer vb) {
			final BlockState blockState = this.blockState;
			final int light = this.lightCoords;

			if (this.drawParts) {
				// upstream KineticBlockEntityRenderer.kineticRotationTransform
				this.shaft.light(light);
				this.shaft.rotateCentered(this.shaftAngle, this.shaftDirection);
				this.shaft.color(this.shaftColor);
				this.shaft.renderInto(pose, vb);
			}

			transform(this.barrel, blockState, true)
					.translate(0, 0, this.barrelOffset)
					.light(light)
					.renderInto(pose, vb);

			transform(this.bellow, blockState, true)
					.translate(0, this.bellowOffset, 0)
					.light(light)
					.renderInto(pose, vb);

			transform(this.bellow, blockState, true)
					.rotateCentered((float) (Math.PI), Direction.SOUTH)
					.light(light)
					.translate(0, this.bellowOffset, 0).renderInto(pose, vb);

			if (this.drawParts) {
				transform(this.cog, blockState, true)
						.rotateCentered(Mth.DEG_TO_RAD * (this.cogAngle % 360), Direction.SOUTH)
						.light(light)
						.renderInto(pose, vb);
			}
		}
	}
}
