package dev.ryanhcode.sable.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ResourceReloadDelegate implements IdentifiableResourceReloadListener {
    private final Identifier id;
    private final SimpleJsonResourceReloadListener delegate;

    public ResourceReloadDelegate(final Identifier id, final SimpleJsonResourceReloadListener delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    @Override
    public Identifier getFabricId() {
        return this.id;
    }

    @Override
    public CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
        return this.delegate.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }
}
