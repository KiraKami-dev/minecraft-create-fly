package dev.ryanhcode.sable.sublevel.render.vanilla;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moving-block render state for single-block sub-levels. Block tints come from the plot, while lighting is sampled at
 * the sub-level's position in the world.
 */
public final class SingleBlockSubLevelWrapper extends MovingBlockRenderState {

    private ClientLevel clientLevel;
    private final BlockPos.MutableBlockPos globalPos = new BlockPos.MutableBlockPos();

    public void setup(final ClientLevel level, final double x, final double y, final double z, final BlockPos localPos, final BlockState state) {
        this.clientLevel = level;
        this.level = level;
        this.globalPos.set(x, y, z);
        this.blockPos = localPos.immutable();
        this.randomSeedPos = this.blockPos;
        this.blockState = state;
        this.biome = null;
    }

    @Override
    public int getBrightness(final LightLayer lightLayer, final BlockPos pos) {
        return this.getLightEngine().getLayerListener(lightLayer).getLightValue(this.globalPos);
    }

    @Override
    public int getRawBrightness(final BlockPos pos, final int i) {
        return this.getLightEngine().getRawBrightness(this.globalPos, i);
    }

    @Override
    public boolean canSeeSky(final BlockPos pos) {
        return this.getBrightness(LightLayer.SKY, this.globalPos) >= 15;
    }

    @Override
    public int getBlockTint(final BlockPos pos, final ColorResolver colorResolver) {
        return this.clientLevel != null ? this.clientLevel.getBlockTint(pos, colorResolver) : super.getBlockTint(pos, colorResolver);
    }

    public ClientLevel getLevel() {
        return this.clientLevel;
    }
}
