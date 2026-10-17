package dev.simulated_team.simulated.content.physics_staff;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.flywheel.lib.util.ShadersModHelper;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.lang.Math;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Renders the physics staff.
 * <p>
 * Upstream this was a Create {@code CustomRenderedItemModelRenderer} hooked in through NeoForge's
 * {@code IClientItemExtensions}. On 1.21.11 it is an item model type ({@value #ID_STRING}, referenced from
 * {@code assets/simulated/items/creative_physics_staff.json}) that bakes the staff parts itself, like Create Fly's
 * {@code SymmetryWandModel}. Register it with {@link PhysicsStaffClient#registerClient()}.
 */
public class PhysicsStaffItemRenderer implements ItemModel, SpecialModelRenderer<PhysicsStaffItemRenderer.StaffRenderData> {
    private static final String ID_STRING = "simulated:model/physics_staff";
    public static final Identifier ID = Simulated.path("model/physics_staff");

    private static final Identifier ITEM_ID = Simulated.path("item/creative_physics_staff/item");
    private static final Identifier CORE_ID = Simulated.path("item/creative_physics_staff/core");
    private static final Identifier CORE_GLOW_ID = Simulated.path("item/creative_physics_staff/core_glow");
    private static final Identifier RING_ID = Simulated.path("item/creative_physics_staff/ring");
    private static final Identifier SIGMA_ID = Simulated.path("item/creative_physics_staff/sigma");
    private static final Identifier INNER_CUBE_ID = Simulated.path("item/creative_physics_staff/inner_cube");
    private static final Identifier OUTER_CUBE_ID = Simulated.path("item/creative_physics_staff/outer_cube");

    private static final int[] NO_TINTS = new int[0];

    private static final Vector3d focusPos = new Vector3d();
    private static final Matrix4f itemProjMat = new Matrix4f();

    private final ModelRenderProperties settings;
    private final List<BakedQuad> item;
    private final List<BakedQuad> core;
    private final List<BakedQuad> coreGlow;
    private final List<BakedQuad> ring;
    private final List<BakedQuad> sigma;
    private final List<BakedQuad> innerCube;
    private final List<BakedQuad> outerCube;
    private final Supplier<Vector3fc[]> extents;
    private final Identifier atlas;
    private @Nullable RenderType cutout;

    public PhysicsStaffItemRenderer(final ModelRenderProperties settings,
                                    final List<BakedQuad> item,
                                    final List<BakedQuad> core,
                                    final List<BakedQuad> coreGlow,
                                    final List<BakedQuad> ring,
                                    final List<BakedQuad> sigma,
                                    final List<BakedQuad> innerCube,
                                    final List<BakedQuad> outerCube) {
        this.settings = settings;
        this.item = item;
        this.core = core;
        this.coreGlow = coreGlow;
        this.ring = ring;
        this.sigma = sigma;
        this.innerCube = innerCube;
        this.outerCube = outerCube;
        this.atlas = item.isEmpty() ? TextureAtlas.LOCATION_BLOCKS : item.getFirst().sprite().atlasLocation();
        this.extents = Suppliers.memoize(() -> {
            final Set<Vector3fc> set = new HashSet<>();
            for (final List<BakedQuad> quads : List.of(item, core, coreGlow, ring, sigma, innerCube, outerCube)) {
                for (final BakedQuad quad : quads) {
                    set.add(quad.position0());
                    set.add(quad.position1());
                    set.add(quad.position2());
                    set.add(quad.position3());
                }
            }
            return set.toArray(Vector3fc[]::new);
        });
    }

    public static Vec3 getFirstPersonFocusPos(final float pt) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Vector3d focusPoint = new Vector3d(PhysicsStaffItemRenderer.focusPos);
        final Quaternionf orientation = camera.rotation();
        orientation.transformInverse(focusPoint);
        final Vector4f v4 = new Vector4f((float) focusPoint.x, (float) focusPoint.y, (float) focusPoint.z, 1.0f);


        final Matrix4f actualProjMat = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, AnimationTickHolder.getPartialTicks(), true));
        actualProjMat.invert(new Matrix4f()).transform(v4);
        itemProjMat.transform(v4);
        focusPoint.set(v4.x, v4.y, v4.z);
        orientation.transform(focusPoint);

        final double fov = gameRenderer.getFov(camera, pt, true);
        focusPoint.mul(100 / fov);

        return JOMLConversion.toMojang(focusPoint);
    }

    @Override
    public void update(final ItemStackRenderState state,
                       final ItemStack stack,
                       final ItemModelResolver resolver,
                       final ItemDisplayContext displayContext,
                       @Nullable final ClientLevel level,
                       @Nullable final ItemOwner owner,
                       final int seed) {
        state.appendModelIdentityElement(this);
        state.setAnimated();

        final ItemStackRenderState.LayerRenderState layer = state.newLayer();
        layer.setExtents(this.extents);
        layer.setupSpecialModel(this, extractOpenState(stack, displayContext));
        this.settings.applyToLayer(layer, displayContext);
    }

    /**
     * Works out how far the staff in this stack is opened. The stack has to be compared by identity, so this runs while
     * the item render state is extracted.
     */
    private static StaffRenderData extractOpenState(final ItemStack stack, final ItemDisplayContext context) {
        float openAmount = 0;
        float cubeScale = 0;
        final PhysicsStaffClientHandler clientHandler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        final Minecraft minecraft = Minecraft.getInstance();

        final float partialTicks = AnimationTickHolder.getPartialTicks();

        // only staffs held in a players hand should attempt to render opened
        final Player player = SimDistUtil.getClientPlayer();

        if (player != null && (context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
                // smooth lerping for your own staff you're holding
                openAmount = Mth.lerp(partialTicks, clientHandler.previousExtension, clientHandler.extension);
                cubeScale = Mth.lerp(partialTicks, clientHandler.previousCubeScale, clientHandler.cubeScale);
            } else if (minecraft.level != null) {
                // look up existing beams for their associated player
                for (final UUID playerUUID : clientHandler.beams.keySet()) {
                    final Player otherPlayer = minecraft.level.getPlayerByUUID(playerUUID);
                    if (otherPlayer != null && (otherPlayer.getMainHandItem() == stack || otherPlayer.getOffhandItem() == stack)) {
                        openAmount = Mth.lerp(partialTicks, clientHandler.beams.get(playerUUID).previousExtension, clientHandler.beams.get(playerUUID).extension);
                        cubeScale = Mth.lerp(partialTicks, clientHandler.beams.get(playerUUID).previousCubeScale, clientHandler.beams.get(playerUUID).cubeScale);
                        break;
                    }
                }
            }
        }

        return new StaffRenderData(openAmount, cubeScale);
    }

    @Override
    public void submit(@Nullable final StaffRenderData data,
                       final ItemDisplayContext context,
                       final PoseStack ms,
                       final SubmitNodeCollector queue,
                       final int light,
                       final int overlay,
                       final boolean glint,
                       final int outlineColor) {
        final float openAmount = data != null ? data.openAmount() : 0;
        float cubeScale = data != null ? data.cubeScale() : 0;

        final PhysicsStaffClientHandler clientHandler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
        final Minecraft minecraft = Minecraft.getInstance();
        final Camera camera = minecraft.gameRenderer.getMainCamera();
        final float partialTicks = AnimationTickHolder.getPartialTicks();
        final Player player = SimDistUtil.getClientPlayer();

        final float tiltAmount = Mth.lerp(partialTicks, clientHandler.previousTilt, clientHandler.tilt);
        final Quaternionf utilQuat = new Quaternionf();

        final boolean shadersActive = ShadersModHelper.isShaderPackInUse();
        final RenderType cutout = this.getCutoutRenderType();
        final RenderType glowingSolid = SimRenderTypes.itemGlowingSolid(shadersActive);
        final RenderType glowingTranslucent = SimRenderTypes.itemGlowingTranslucent(shadersActive);

        ms.pushPose();
        // the upstream renderer worked from the item center, 1.21.11 hands special renderers the model corner
        ms.translate(0.5F, 0.5F, 0.5F);

        if (context.firstPerson()) {
            if (clientHandler.getDragSession() != null && player != null) {
                final PhysicsStaffClientHandler.ClientDragSession dragSession = clientHandler.getDragSession();

                final Quaternionf rotation = camera.rotation();
                final Vector3d globalAnchor = ((ClientSubLevel) dragSession.dragSubLevel()).renderPose().transformPosition(new Vector3d(dragSession.dragLocalAnchor()));
                final Vector3d dirToAnchor = globalAnchor.sub(JOMLConversion.toJOML(player.getEyePosition(partialTicks))).normalize();
                rotation.transformInverse(dirToAnchor);

                final Quaternionf quat = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 0.0, -1.0), dirToAnchor);
                ms.mulPose(utilQuat.identity().rotateY(-Mth.HALF_PI));
                ms.mulPose(quat.slerp(utilQuat.identity(), 0.6f));
                ms.mulPose(utilQuat.identity().rotateY(Mth.HALF_PI));
            }
            final float tiltMultiplier = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0f : 1.0f;

            ms.mulPose(utilQuat.identity().rotateZ((float) Math.toRadians((tiltAmount * 0.5 + 0.5) * -61.0f) * tiltMultiplier));
        }

        renderPart(ms, queue, context, light, overlay, outlineColor, this.item, cutout);

        renderPart(ms, queue, context, LightTexture.FULL_BRIGHT, overlay, outlineColor, this.core, glowingSolid);
        renderPart(ms, queue, context, LightTexture.FULL_BRIGHT, overlay, outlineColor, this.coreGlow, glowingTranslucent);

        ms.pushPose();
        ms.translate(0, 6.5 / 16.0, 0);
        renderPart(ms, queue, context, light, overlay, outlineColor, this.ring, cutout);
        ms.popPose();

        ms.translate(0, 9 / 16.0, 0);
        for (int i = 0; i < 2; i++) {
            ms.pushPose();
            ms.mulPose(Axis.YP.rotationDegrees(i * 180));
            ms.translate(-3 / 16.0, 0, 0);
            ms.mulPose(Axis.ZP.rotationDegrees(openAmount * 20));
            renderPart(ms, queue, context, light, overlay, outlineColor, this.sigma, cutout);
            ms.popPose();
        }
        ms.translate(0, 6 / 16.0, 0);

        if (context.firstPerson()) {
            if (clientHandler.getDragSession() != null) {
                clientHandler.lastCubeOrientation.set(clientHandler.getDragSession().dragOrientation());
            }

            final Matrix4f m = new Matrix4f(ms.last().pose());
            m.m30(0).m31(0).m32(0);
            m.invert();
            m.rotate(clientHandler.lastCubeOrientation);
            ms.mulPose(m);
        }

        cubeScale = Mth.lerp(cubeScale, -0.05f, 1f);
        cubeScale = Mth.clamp(cubeScale, 0, 1);
        cubeScale *= 0.8f;
        ms.scale(cubeScale, cubeScale, cubeScale);
        renderPart(ms, queue, context, LightTexture.FULL_BRIGHT, overlay, outlineColor, this.innerCube, glowingSolid);

        if (context.firstPerson()) {
            final Vector3f focusPoint = new Vector3f();
            ms.last().pose().transformPosition(focusPoint);

            itemProjMat.set(getHandProjectionMatrix(minecraft, camera, partialTicks));
            focusPos.set(focusPoint.x, focusPoint.y, focusPoint.z);
        }

        ms.scale(1.2f, 1.2f, 1.2f);
        renderPart(ms, queue, context, LightTexture.FULL_BRIGHT, overlay, outlineColor, this.outerCube, glowingTranslucent);

        // (upstream ended all batches here for Iris; submitted geometry is batched by the feature renderer on 1.21.11)
        ms.popPose();
    }

    /**
     * @return the projection matrix used to draw the first person hand ({@code GameRenderer#renderLevel}; upstream read
     * {@code RenderSystem.getProjectionMatrix()} while the hand rendered, 1.21.11 only keeps it in a GPU buffer)
     */
    private static Matrix4f getHandProjectionMatrix(final Minecraft minecraft, final Camera camera, final float partialTicks) {
        final float fov = minecraft.gameRenderer.getFov(camera, partialTicks, false);
        return new Matrix4f().perspective(fov * Mth.DEG_TO_RAD, (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight(), 0.05F, 100.0F);
    }

    private static void renderPart(final PoseStack ms,
                                   final SubmitNodeCollector queue,
                                   final ItemDisplayContext context,
                                   final int light,
                                   final int overlay,
                                   final int outlineColor,
                                   final List<BakedQuad> quads,
                                   final RenderType renderType) {
        ms.pushPose();
        ms.translate(-0.5F, -0.5F, -0.5F);
        queue.submitItem(ms, context, light, overlay, outlineColor, NO_TINTS, quads, renderType, ItemStackRenderState.FoilType.NONE);
        ms.popPose();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        for (final Vector3fc extent : this.extents.get()) {
            output.accept(extent);
        }
    }

    @Override
    public @Nullable StaffRenderData extractArgument(final ItemStack stack) {
        return new StaffRenderData(0, 0);
    }

    public record StaffRenderData(float openAmount, float cubeScale) {
    }

    /**
     * The cutout render type for the staff parts. 1.21.1 had a single block/item atlas; on 1.21.11 the staff textures
     * live on the item atlas, so {@code Sheets.cutoutBlockSheet()} is only used if the quads are on the block atlas.
     */
    private RenderType getCutoutRenderType() {
        if (this.cutout == null) {
            this.cutout = this.atlas.equals(TextureAtlas.LOCATION_BLOCKS) ? Sheets.cutoutBlockSheet() : RenderTypes.entityCutout(this.atlas);
        }
        return this.cutout;
    }

    public static class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(final Resolver resolver) {
            resolver.markDependency(ITEM_ID);
            resolver.markDependency(CORE_ID);
            resolver.markDependency(CORE_GLOW_ID);
            resolver.markDependency(RING_ID);
            resolver.markDependency(SIGMA_ID);
            resolver.markDependency(INNER_CUBE_ID);
            resolver.markDependency(OUTER_CUBE_ID);
        }

        @Override
        public ItemModel bake(final ItemModel.BakingContext context) {
            final ModelBaker baker = context.blockModelBaker();
            final ResolvedModel model = baker.getModel(ITEM_ID);
            final ModelRenderProperties settings = ModelRenderProperties.fromResolvedModel(baker, model, model.getTopTextureSlots());
            return new PhysicsStaffItemRenderer(
                    settings,
                    bake(baker, ITEM_ID),
                    bake(baker, CORE_ID),
                    bake(baker, CORE_GLOW_ID),
                    bake(baker, RING_ID),
                    bake(baker, SIGMA_ID),
                    bake(baker, INNER_CUBE_ID),
                    bake(baker, OUTER_CUBE_ID)
            );
        }

        private static List<BakedQuad> bake(final ModelBaker baker, final Identifier id) {
            final ResolvedModel model = baker.getModel(id);
            return model.bakeTopGeometry(model.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY).getAll();
        }
    }
}
