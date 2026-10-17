package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperBufferFactory;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferCache;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.content.kinetics.waterwheel.WaterWheelRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import dev.simulated_team.simulated.index.SimPartialModels;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

public class SteeringWheelRenderer extends KineticBlockEntityRenderer<SteeringWheelBlockEntity, SteeringWheelRenderer.SteeringWheelRenderState> {
    public static final SuperByteBufferCache.Compartment<ModelKey> STEERING_WHEEL = new SuperByteBufferCache.Compartment<>();

    public SteeringWheelRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SteeringWheelRenderState createRenderState() {
        return new SteeringWheelRenderState();
    }

    @Override
    public void extractRenderState(final SteeringWheelBlockEntity be, final SteeringWheelRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        final Level level = be.getLevel();
        state.support = VisualizationManager.supportsVisualization(level);
        if (state.support) {
            return;
        }
        this.updateBaseRenderState(be, state, level, crumblingOverlay);

        final boolean floor = state.blockState.getValue(SteeringWheelBlock.ON_FLOOR);
        final Direction facing = state.blockState.getValue(SteeringWheelBlock.FACING);

        if (be.shouldRenderShaft()) {
            state.model = CachedBuffers.partialFacing(
                    AllPartialModels.SHAFT_HALF,
                    state.blockState,
                    floor ? Direction.DOWN : Direction.UP
            );
            state.angle = getAngleForBe(be, state.blockPos, state.axis);
        } else {
            state.model = null;
        }

        state.wheel = getWheelModel(be.material);
        state.facingRotation = facing.getRotation();
        state.floor = floor;
        state.wheelAngle = be.getRenderAngle(partialTicks);
    }

    @Override
    protected RenderType getRenderType(final SteeringWheelBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    public static SuperByteBuffer getWheelModel(final BlockState material) {
        final ModelKey key = new ModelKey(material);
        return SuperByteBufferCache.getInstance().get(STEERING_WHEEL, key, () -> {
            final SimpleModelWrapper model = generateModel(SimPartialModels.STEERING_WHEEL.get(), material);
            return SuperBufferFactory.getInstance().createForBlock(model, Blocks.AIR.defaultBlockState(), new PoseStack());
        });
    }

    /**
     * Swaps the oak planks of the (OBJ) wheel model for the planks of the given material, like Create's water wheels
     */
    public static SimpleModelWrapper generateModel(final SimpleModelWrapper template, final BlockState planksBlockState) {
        final String wood = plankStateToWoodName(planksBlockState);

        if (wood == null)
            return BakedModelHelper.generateModel(template, sprite -> null);

        final Map<TextureAtlasSprite, TextureAtlasSprite> map = new Reference2ReferenceOpenHashMap<>();
        map.put(WaterWheelRenderer.OAK_PLANKS_TEMPLATE.get(), getSpriteOnSide(planksBlockState, Direction.UP));

        return BakedModelHelper.generateModel(template, map::get);
    }

    public record ModelKey(BlockState material) {
    }

    // todo tell create to make these public :p
    @Nullable
    private static String plankStateToWoodName(final BlockState planksBlockState) {
        final Block planksBlock = planksBlockState.getBlock();
        final Identifier id = RegisteredObjectsHelper.getKeyOrThrow(planksBlock);
        final String path = id.getPath();

        if (path.endsWith("_planks")) // Covers most wood types
            return (path.startsWith("archwood") ? "blue_" : "") + path.substring(0, path.length() - 7);

        if (path.contains("wood/planks/")) // TerraFirmaCraft
            return path.substring(12);

        return null;
    }

    private static TextureAtlasSprite getSpriteOnSide(final BlockState state, final Direction side) {
        final BlockStateModel model = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(state);
        if (model == null)
            return null;
        final RandomSource random = RandomSource.create();
        random.setSeed(42L);
        final List<BlockModelPart> parts = model.collectParts(random);
        for (final BlockModelPart part : parts) {
            final List<BakedQuad> quads = part.getQuads(side);
            if (!quads.isEmpty()) {
                return quads.getFirst().sprite();
            }
        }
        for (final BlockModelPart part : parts) {
            for (final BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == side) {
                    return quad.sprite();
                }
            }
        }
        return model.particleIcon();
    }

    public static class SteeringWheelRenderState extends KineticRenderState {
        public SuperByteBuffer wheel;
        public Quaternionf facingRotation;
        public boolean floor;
        public float wheelAngle;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            if (this.model != null) {
                super.render(matricesEntry, vertexConsumer);
            }

            this.wheel.rotateCentered(this.facingRotation);
            if (this.floor) {
                this.wheel.translate(0, 6.5 / 16f, -5 / 16f);
            } else {
                this.wheel.translate(0, 6.5 / 16f, 5 / 16f);
            }
            this.wheel.rotateCentered(this.wheelAngle, Direction.UP);

            this.wheel.light(this.lightCoords);
            this.wheel.color(Color.WHITE);
            this.wheel.renderInto(matricesEntry, vertexConsumer);
        }
    }
}
