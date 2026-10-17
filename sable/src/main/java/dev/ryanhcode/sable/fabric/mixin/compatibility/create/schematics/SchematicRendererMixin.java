package dev.ryanhcode.sable.fabric.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.levelWrappers.SchematicLevel;
import com.zurrtum.create.client.catnip.render.ShadedBlockSbbBuilder;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.schematics.client.SchematicRenderer;
import com.zurrtum.create.client.infrastructure.model.WrapperBlockStateModel;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.schematics.SchematicLevelExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Renders the sub-levels stored in a schematic, in each layer of the schematic preview.
 * Mirrors Create Fly's {@code SchematicRenderer#drawLayer} block loop (1.21.11 block model parts instead of baked models).
 */
@Mixin(SchematicRenderer.class)
public class SchematicRendererMixin {

    @Final
    @Shadow(remap = false)
    private BlockPos anchor;

    @SuppressWarnings("removal")
    @Inject(method = "drawLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;clearCache()V", shift = At.Shift.BEFORE, remap = true), remap = false)
    private void sable$drawLayer(final Minecraft mc,
                                 final ChunkSectionLayer layer,
                                 final CallbackInfoReturnable<SuperByteBuffer> cir,
                                 @Local final BlockRenderDispatcher dispatcher,
                                 @Local final ModelBlockRenderer renderer,
                                 @Local final RandomSource random,
                                 @Local final SchematicLevel mainRenderWorld,
                                 @Local final PoseStack poseStack,
                                 @Local final BlockPos.MutableBlockPos mutableBlockPos,
                                 @Local final ShadedBlockSbbBuilder sbbBuilder) {
        for (final SchematicLevelExtension.SchematicSubLevel subLevel : ((SchematicLevelExtension) mainRenderWorld).sable$getSubLevels()) {
            final SchematicLevel renderWorld = subLevel.level();
            final BoundingBox bounds = renderWorld.getBounds();
            renderWorld.renderMode = true;

            poseStack.pushPose();
            poseStack.translate(subLevel.position().x, subLevel.position().y, subLevel.position().z);
            poseStack.mulPose(new Quaternionf(subLevel.orientation()));

            for (final BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                final BlockPos pos = mutableBlockPos.setWithOffset(localPos, this.anchor);
                final BlockState state = renderWorld.getBlockState(pos);

                if (state.getRenderShape() == RenderShape.MODEL && ItemBlockRenderTypes.getChunkRenderType(state) == layer) {
                    final long seed = state.getSeed(pos);
                    final BlockStateModel model = dispatcher.getBlockModel(state);
                    random.setSeed(seed);

                    poseStack.pushPose();
                    poseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());

                    final List<BlockModelPart> parts = new ObjectArrayList<>();
                    if (WrapperBlockStateModel.unwrapCompat(model) instanceof final WrapperBlockStateModel wrapper) {
                        wrapper.addPartsWithInfo(renderWorld, pos, state, random, parts);
                    } else {
                        model.collectParts(random, parts);
                    }

                    renderer.tesselateBlock(renderWorld, parts, state, pos, poseStack, sbbBuilder, true, OverlayTexture.NO_OVERLAY);

                    poseStack.popPose();
                }
            }
            poseStack.popPose();
            renderWorld.renderMode = false;
        }
    }
}
