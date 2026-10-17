package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SableSubLevelRenderPlatformImpl implements SableSubLevelRenderPlatform {
    // Flywheel is bundled in Create Fly
    private static final boolean FLYWHEEL_LOADED = FabricLoader.getInstance().isModLoaded("create");

    @Override
    public void tryAddFlywheelVisual(final BlockEntity blockEntity) {
        // FlywheelCompat references Flywheel classes, so only touch it when Create is present
        if (FLYWHEEL_LOADED) {
            FlywheelCompat.tryAddVisual(blockEntity);
        }
    }
}
