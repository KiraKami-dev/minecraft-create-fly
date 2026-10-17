package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.fabric.compatibility.create.CreateLevelWrappers;
import dev.ryanhcode.sable.platform.SablePlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class SablePlatformImpl implements SablePlatform {
    private static final boolean CREATE_LOADED = FabricLoader.getInstance().isModLoaded("create");

    @Override
    public boolean isWrappedLevel(@Nullable final Level level) {
        // CreateLevelWrappers references Create classes, so only touch it when Create is present
        return CREATE_LOADED && CreateLevelWrappers.isWrapped(level);
    }

    @Override
    public boolean isBlockstateLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
        return false; //handled already for fabric
    }
}
