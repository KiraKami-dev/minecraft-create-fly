package dev.simulated_team.simulated.content.entities.diagram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.index.SimPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.Blocks;

public class DiagramEntityRenderer extends EntityRenderer<DiagramEntity, DiagramEntityRenderer.DiagramRenderState> {

	public DiagramEntityRenderer(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public DiagramRenderState createRenderState() {
		return new DiagramRenderState();
	}

	@Override
	public void extractRenderState(final DiagramEntity entity, final DiagramRenderState state, final float pt) {
		super.extractRenderState(entity, state, pt);

		final PartialModel partialModel = entity.size == 3 ? SimPartialModels.CONTRAPTION_DIAGRAM_3x3
			: entity.size == 2 ? SimPartialModels.CONTRAPTION_DIAGRAM_2x2 : SimPartialModels.CONTRAPTION_DIAGRAM_1x1;
		state.model = CachedBuffers.partial(partialModel, Blocks.AIR.defaultBlockState());
		state.yaw = entity.getYRot(pt);
		state.pitch = entity.getXRot();
		state.size = entity.size;
	}

	@Override
	public void submit(final DiagramRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
		queue.submitCustomGeometry(ms, Sheets.solidBlockSheet(), state);
		super.submit(state, ms, queue, cameraState);
	}

	public static class DiagramRenderState extends EntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
		private SuperByteBuffer model;
		private float yaw;
		private float pitch;
		private int size;

		@Override
		public void render(final PoseStack.Pose pose, final VertexConsumer consumer) {
			final SuperByteBuffer sbb = this.model;
			sbb.rotateYDegrees(-this.yaw)
				.rotateXDegrees(90.0F + this.pitch)
				.translate(-.5, -1 / 32f, -.5);
			if (this.size == 2) {
				sbb.translate(.5, 0, -.5);
			}

			sbb.disableDiffuse()
				.light(this.lightCoords)
				.renderInto(pose, consumer);
		}
	}

}
