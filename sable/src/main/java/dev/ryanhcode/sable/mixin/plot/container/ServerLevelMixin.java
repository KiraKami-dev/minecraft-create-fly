package dev.ryanhcode.sable.mixin.plot.container;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.platform.SablePlatform;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Adds a {@link SubLevelContainer} to the server levels
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements SubLevelContainerHolder {
    @Unique
    private SubLevelContainer sable$plotContainer;

    protected ServerLevelMixin(final WritableLevelData writableLevelData, final ResourceKey<Level> resourceKey, final RegistryAccess registryAccess, final Holder<DimensionType> holder, final boolean bl, final boolean bl2, final long l, final int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }


    /**
     * PORT(1.21.11): the container can no longer be created at the start of the constructor, as
     * {@link Level#getSeaLevel()} now needs the chunk source (and so the generator) that is assigned partway through.
     */
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;chunkSource:Lnet/minecraft/server/level/ServerChunkCache;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void sable$initPlotContainer(final CallbackInfo ci) {
        this.sable$plotContainer = this.sable$createPlotContainer();
    }

    @Unique
    private SubLevelContainer sable$createPlotContainer() {
        if (SablePlatform.INSTANCE.isWrappedLevel(this)) return null;
        final SubLevelContainer container;

        if (!this.isClientSide()) {
            container = new ServerSubLevelContainer(this, SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH, SubLevelContainer.DEFAULT_LOG_PLOT_SIZE, SubLevelContainer.DEFAULT_ORIGIN, SubLevelContainer.DEFAULT_ORIGIN);
        } else {
            container = new ClientSubLevelContainer(this, SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH, SubLevelContainer.DEFAULT_LOG_PLOT_SIZE, SubLevelContainer.DEFAULT_ORIGIN, SubLevelContainer.DEFAULT_ORIGIN);
        }

        Sable.defaultSubLevelContainerInitializer(this, container);
        SableEventPublishPlatform.INSTANCE.onSubLevelContainerReady(this, container);

        return container;
    }

    @Override
    public SubLevelContainer sable$getPlotContainer() {
        return this.sable$plotContainer;
    }
}
