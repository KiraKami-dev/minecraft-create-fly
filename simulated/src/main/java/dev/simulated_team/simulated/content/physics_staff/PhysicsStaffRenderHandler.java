package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllSpecialTextures;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.ryanhcode.sable.veillite.api.client.render.MatrixStack;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

import java.util.List;
import java.util.UUID;

public class PhysicsStaffRenderHandler {

    @Nullable
    private static BlockPos hoverBlockPos = null;

    /**
     * Upstream entry point (Veil's AFTER_TRANSLUCENT_BLOCKS stage). The veillite shim never fires render stages, so the
     * staff is rendered from {@link #renderSelectionBox(PoseStack, MultiBufferSource, Camera)} instead,
     * which {@link PhysicsStaffClient#registerClient()} hooks into the end of the main level pass.
     */
    @Deprecated
    public static void renderSelectionBox(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer renderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack ps, final Matrix4fc frustrumMat, final Matrix4fc projectionMat, final int renderTick, final DeltaTracker tracker, final Camera camera, final Frustum frustrum) {
    }

    /**
     * Renders the selection / hovering box for the staff
     *
     * @param ps           an empty pose stack (camera relative)
     * @param bufferSource the buffer source of the level renderer
     * @param camera       the camera the level is rendered from
     */
    public static void renderSelectionBox(final PoseStack ps, final MultiBufferSource bufferSource, final Camera camera) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }

        ps.pushPose();
        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.onRender(ps);
        ps.popPose();

        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;

        if (player == null) {
            return;
        }

        if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(SimItems.PHYSICS_STAFF) &&
                !player.getItemInHand(InteractionHand.OFF_HAND).is(SimItems.PHYSICS_STAFF)) {
            return;
        }

        final Vec3 cameraPos = camera.position();

        final Level level = player.level();
        renderAllLocks(bufferSource, ps, level, cameraPos, camera);

        updateHoverPos(minecraft, player);

        if (hoverBlockPos != null) {
            // (191, 191, 191)
            final int color = 0xBFBFBF;

            Outliner.getInstance().showCluster("physicsStaffSelection", List.of(hoverBlockPos))
                    .colored(color)
                    .disableLineNormals()
                    .lineWidth(1 / 32f)
                    .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
    }

    /**
     * Updates the hovered block position
     */
    private static void updateHoverPos(final Minecraft minecraft, final LocalPlayer player) {
        final ClientLevel level = minecraft.level;
        final float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        hoverBlockPos = null;

        final PhysicsStaffClientHandler.ClientDragSession dragSession = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getDragSession();

        if (dragSession != null) {
            final Vector3dc localAnchor = dragSession.dragLocalAnchor();
            hoverBlockPos = BlockPos.containing(localAnchor.x(), localAnchor.y(), localAnchor.z());
            return;
        }

        final LevelPoseProviderExtension extension = (LevelPoseProviderExtension) level;
        extension.sable$pushPoseSupplier(x -> ((ClientSubLevel) x).renderPose());
        final HitResult hit = player.pick(PhysicsStaffItem.RANGE, partialTicks, false);
        extension.sable$popPoseSupplier();

        if (!(hit instanceof final BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        final Vec3 hitLocation = hit.getLocation();

        final SubLevel subLevel = Sable.HELPER.getContaining(level, hitLocation);
        if (subLevel == null) {
            return;
        }

        hoverBlockPos = blockHitResult.getBlockPos();
    }

    /**
     * Renders all the locks our client is aware about
     */
    private static void renderAllLocks(final MultiBufferSource bufferSource, final PoseStack ps, final Level level, final Vec3 cameraPos, final Camera camera) {
        final List<UUID> locks = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getLocks(level);
        final SubLevelContainer container = SubLevelContainer.getContainer(level);

        if (locks == null || container == null) {
            return;
        }

        for (final UUID lock : locks) {
            final SubLevel subLevel = container.getSubLevel(lock);

            if (!(subLevel instanceof final ClientSubLevel clientSubLevel)) continue;

            ps.pushPose();
            final Vector3dc renderPos = clientSubLevel.renderPose().position();
            ps.translate(renderPos.x() - cameraPos.x(), renderPos.y() - cameraPos.y(), renderPos.z() - cameraPos.z());
            // the entity render dispatcher's camera orientation
            ps.mulPose(camera.rotation());

            final VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.lock());

            final PoseStack.Pose pose = ps.last();
            final int color = 0xffffffff;
            buffer.addVertex(pose, 0.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 1.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 0.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 0.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 0.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 1.0f).setLight(LightTexture.FULL_BRIGHT);

            ps.popPose();
        }
    }

}
