package dev.ryanhcode.sable.platform;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SableSubLevelRenderPlatform {
    SableSubLevelRenderPlatform INSTANCE = SablePlatformUtil.load(SableSubLevelRenderPlatform.class);

    // 1.21.11: tesselateBlock/getRenderLayers were removed; single-block sub-levels are rendered via
    // vanilla MovingBlockRenderState by the sub-level renderer.

    void tryAddFlywheelVisual(final BlockEntity blockEntity);
}
