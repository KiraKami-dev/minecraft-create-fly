package dev.ryanhcode.sable.mixin.sublevel_render.block_entity_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

/**
 * Extracts and submits the block entities of sub-levels with the sub-level transform.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    /**
     * Globally rendered block entities of sub-levels are extracted by the sub-level renderer instead
     */
    @WrapOperation(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;", ordinal = 1))
    private BlockEntityRenderState sable$skipSubLevelGlobalBlockEntities(final BlockEntityRenderDispatcher instance, final BlockEntity blockEntity, final float partialTick, final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, final Operation<BlockEntityRenderState> original) {
        if (Sable.HELPER.getContainingClient(blockEntity) != null) {
            return null;
        }

        return original.call(instance, blockEntity, partialTick, crumblingOverlay);
    }

    @Inject(method = "extractVisibleBlockEntities", at = @At("TAIL"))
    private void sable$extractSubLevelBlockEntities(final Camera camera, final float partialTick, final LevelRenderState levelRenderState, final CallbackInfo ci) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container != null) {
            SubLevelRenderDispatcher.get().extractBlockEntities(container.getAllSubLevels(), camera, partialTick, this.destructionProgress);
        }
    }

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void sable$submitSubLevelBlockEntities(final PoseStack poseStack, final LevelRenderState levelRenderState, final SubmitNodeStorage submitNodeStorage, final CallbackInfo ci) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container != null) {
            SubLevelRenderDispatcher.get().submit(container.getAllSubLevels(), poseStack, submitNodeStorage, levelRenderState.cameraRenderState);
        }
    }
}
