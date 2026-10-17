package dev.simulated_team.simulated.content.items.plunger_launcher;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.AllExtensions;
import com.zurrtum.create.client.content.equipment.zapper.ShootableGadgetRenderHandler;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.infrastructure.particle.AirParticleData;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Client-only. Upstream this was a Create {@code CustomRenderedItemModelRenderer} (NeoForge
 * {@code IClientItemExtensions}); on 1.21.11 it is an item model type ({@value #TYPE_NAME}, see
 * {@code assets/simulated/items/plunger_launcher.json}) that renders the original model plus the loaded plungers.
 */
public class PlungerLauncherItemRenderer implements ItemModel, SpecialModelRenderer<PlungerLauncherItemRenderer.RenderData> {
    public static final String TYPE_NAME = "model/plunger_launcher";
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Simulated.MOD_ID, TYPE_NAME);
    /**
     * The original model of the custom rendered item
     */
    public static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath(Simulated.MOD_ID, "item/plunger_launcher");
    public static final Identifier BODY_ID = Identifier.fromNamespaceAndPath(Simulated.MOD_ID, "item/plunger_launcher/plunger_tether");
    public static final Identifier SPOOL_ID = Identifier.fromNamespaceAndPath(Simulated.MOD_ID, "item/plunger_launcher/tether_spool");
    public static final Identifier JOINT_ID = Identifier.fromNamespaceAndPath(Simulated.MOD_ID, "item/plunger_launcher/spool_joint");

    private static final int[] NO_TINTS = new int[0];

    public static final Vector3d focusPos = new Vector3d();
    public static final Matrix4f itemProjMat = new Matrix4f();

    private final List<BakedQuad> itemQuads;
    private final RenderType itemLayer;
    private final ModelRenderProperties itemSettings;
    private final Supplier<Vector3fc[]> itemExtents;
    private final Part body;
    private final Part spool;
    private final Part joint;

    public PlungerLauncherItemRenderer(final List<BakedQuad> itemQuads, final ModelRenderProperties itemSettings,
                                       final Part body, final Part spool, final Part joint) {
        this.itemQuads = itemQuads;
        this.itemLayer = detectRenderType(itemQuads);
        this.itemSettings = itemSettings;
        this.itemExtents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(itemQuads));
        this.body = body;
        this.spool = spool;
        this.joint = joint;
    }

    public static void registerClient() {
        ItemModels.ID_MAPPER.put(ID, Unbaked.CODEC);
        // upstream: CustomArmPoseItem#getArmPose
        AllExtensions.ARM_POSE.put(SimItems.PLUNGER_LAUNCHER, HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }

    @Override
    public void update(final ItemStackRenderState state, final ItemStack stack, final ItemModelResolver resolver, final ItemDisplayContext displayContext,
                       @Nullable final ClientLevel level, @Nullable final ItemOwner owner, final int seed) {
        state.appendModelIdentityElement(this);
        state.setAnimated();

        final RenderData data = new RenderData();
        if (stack.hasFoil()) {
            state.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
            data.foilType = ItemStackRenderState.FoilType.STANDARD;
        }

        final ItemStackRenderState.LayerRenderState layer = state.newLayer();
        layer.setRenderType(this.itemLayer);
        layer.setExtents(this.itemExtents);
        this.itemSettings.applyToLayer(layer, displayContext);
        layer.setFoilType(data.foilType);

        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            final float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            final float cooldown = player.getCooldowns().getCooldownPercent(stack, partialTicks);

            final PlayerLaunchedPlungerExtension duck = (PlayerLaunchedPlungerExtension) player;
            final LaunchedPlungerEntity plunger = duck.simulated$getLaunchedPlunger();

            data.renderPlungers = cooldown <= 0.6f || (plunger != null && plunger.getOther() == null);
            data.renderFirst = (plunger == null || plunger.isRemoved() || plunger.getOther() != null) && cooldown <= 0.4f;

            if (cooldown > 0 && PlungerLauncherItem.reloadCooldown) {
                data.slideIn = (float) Math.pow(Mth.clamp(Mth.map(cooldown, 0.3f, 0.6f, 0, 1), 0, 1), 3);
                data.slideInFirst = (float) Math.pow(Mth.clamp(Mth.map(cooldown, 0.1f, 0.4f, 0, 1), 0, 1), 3);
            }
        }

        layer.setupSpecialModel(this, data);
    }

    @Override
    public void submit(@Nullable final RenderData data, final ItemDisplayContext transformType, final PoseStack ms, final SubmitNodeCollector queue,
                       final int light, final int overlay, final boolean glint, final int outlineColor) {
        if (data == null) {
            return;
        }

        ms.pushPose();
        // ItemTransform#apply leaves the pose at the model corner, the upstream renderer worked from the item center
        ms.translate(0.5f, 0.5f, 0.5f);

        ms.scale(0.8f, 0.8f, 0.8f);
        ms.translate(0, 0, 0.15f);

        // the original model
        ms.pushPose();
        ms.translate(-0.5f, -0.5f, -0.5f);
        queue.submitItem(ms, transformType, light, overlay, outlineColor, NO_TINTS, this.itemQuads, this.itemLayer, data.foilType);
        ms.popPose();

        if (data.renderPlungers) {
            if (data.renderFirst) {
                this.renderPlunger(ms, queue, transformType, light, overlay, outlineColor, true, data.slideInFirst);
            }

            this.renderPlunger(ms, queue, transformType, light, overlay, outlineColor, false, data.slideIn);
        }

        ms.translate(2 / 16f, -1 / 16f, -5 / 16f);
        ms.translate(0, 0, 1 / 16f);

        if (transformType.firstPerson()) {
            final Vector3f focusPoint = new Vector3f();
            ms.last().pose().transformPosition(focusPoint);

            setHandProjection(itemProjMat);
            focusPos.set(focusPoint.x, focusPoint.y, focusPoint.z);
        }

        ms.popPose();
    }

    private void renderPlunger(final PoseStack ms, final SubmitNodeCollector queue, final ItemDisplayContext transformType,
                               final int light, final int overlay, final int outlineColor, final boolean first, final float slideIn) {
        ms.pushPose();
        ms.translate(2 / 16f * (first ? -1 : 1), -1 / 16f, -5 / 16f);
        ms.translate(0, 0, -slideIn / 12f);

        this.body.submit(ms, queue, transformType, light, overlay, outlineColor);
        this.joint.submit(ms, queue, transformType, light, overlay, outlineColor);

        ms.translate(0, 0, 3 / 16f);
        this.spool.submit(ms, queue, transformType, light, overlay, outlineColor);
        ms.popPose();
    }

    /**
     * The projection the first person hand is rendered with (upstream read {@code RenderSystem.getProjectionMatrix()}
     * while the item was rendering; 1.21.11 only keeps it in a GPU buffer, see {@code GameRenderer#renderLevel})
     */
    private static void setHandProjection(final Matrix4f dest) {
        final Minecraft minecraft = Minecraft.getInstance();
        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        final float fov = gameRenderer.getFov(gameRenderer.getMainCamera(), partialTick, false);
        dest.setPerspective(fov * Mth.DEG_TO_RAD, (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight(), 0.05F, 100.0F);
    }

    private static RenderType detectRenderType(final List<BakedQuad> quads) {
        if (!quads.isEmpty() && quads.getFirst().sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            return Sheets.translucentBlockItemSheet();
        }
        return Sheets.translucentItemSheet();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RenderData extractArgument(final ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    public static class RenderData {
        ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
        boolean renderPlungers = true;
        boolean renderFirst = true;
        float slideIn;
        float slideInFirst;
    }

    /**
     * A plunger partial (upstream {@code SimPartialModels.LAUNCHED_PLUNGER_*}, rendered via {@code CachedBuffers})
     */
    public record Part(List<BakedQuad> quads, RenderType renderType) {
        private static Part bake(final ModelBaker baker, final Identifier id) {
            final ResolvedModel model = baker.getModel(id);
            final List<BakedQuad> quads = model.bakeTopGeometry(model.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY).getAll();
            return new Part(quads, detectRenderType(quads));
        }

        private void submit(final PoseStack ms, final SubmitNodeCollector queue, final ItemDisplayContext transformType,
                            final int light, final int overlay, final int outlineColor) {
            queue.submitItem(ms, transformType, light, overlay, outlineColor, NO_TINTS, this.quads, this.renderType, ItemStackRenderState.FoilType.NONE);
        }
    }

    public static class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(final Resolver resolver) {
            resolver.markDependency(ITEM_ID);
            resolver.markDependency(BODY_ID);
            resolver.markDependency(SPOOL_ID);
            resolver.markDependency(JOINT_ID);
        }

        @Override
        public ItemModel bake(final ItemModel.BakingContext context) {
            final ModelBaker baker = context.blockModelBaker();
            final ResolvedModel model = baker.getModel(ITEM_ID);
            final TextureSlots textures = model.getTopTextureSlots();
            final List<BakedQuad> quads = model.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY).getAll();
            final ModelRenderProperties settings = ModelRenderProperties.fromResolvedModel(baker, model, textures);
            return new PlungerLauncherItemRenderer(quads, settings, Part.bake(baker, BODY_ID), Part.bake(baker, SPOOL_ID), Part.bake(baker, JOINT_ID));
        }
    }

    public static class RenderHandler extends ShootableGadgetRenderHandler {

        public void basicShoot(final InteractionHand hand) {
            final LocalPlayer player = Minecraft.getInstance().player;

            if (player != null) {
                final boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
                if (rightHand) {
                    this.rightHandAnimation = .2f;
                    this.dontReequipRight = false;
                } else {
                    this.leftHandAnimation = .2f;
                    this.dontReequipLeft = false;
                }

                final Vec3 focusPos1 = LaunchedPlungerEntityRenderer.getFirstPersonFocusPos(0f);
                for (int i = 0; i < Math.random() * 4; i++) {
                    final Vec3 m2 = VecHelper.offsetRandomly(player.getViewVector(0), player.level().random, 0.5f);
                    player.level().addParticle(new AirParticleData(1, 1 / 4f), focusPos1.x, focusPos1.y, focusPos1.z, m2.x, m2.y, m2.z);
                }

                this.playSound(hand, player.position());
            }
        }

        @Override
        public void playSound(final InteractionHand hand, final Vec3 position) {

        }

        @Override
        protected boolean appliesTo(final ItemStack stack) {
            return stack.is(SimItems.PLUNGER_LAUNCHER);
        }

        @Override
        protected void transformTool(final PoseStack ms, final float flip, final float equipProgress, final float recoil, final float pt) {
            ms.translate(flip * -.1f, 0.05f, .14f);
            TransformStack.of(ms).rotateXDegrees(recoil * 80);
        }

        @Override
        protected void transformHand(final PoseStack ms, final float flip, final float equipProgress, final float recoil, final float pt) {
            ms.scale(0, 0, 0);
        }
    }
}
