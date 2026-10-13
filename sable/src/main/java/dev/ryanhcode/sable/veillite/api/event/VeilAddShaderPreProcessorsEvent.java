/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.event;

import dev.ryanhcode.sable.veillite.api.client.render.shader.processor.ShaderPreProcessor;
import net.minecraft.server.packs.resources.ResourceProvider;

/**
 * Fired when shaders are about to be compiled, to collect shader pre-processors.
 * <p>
 * Fired by {@link dev.ryanhcode.sable.veillite.impl.event.VeilLiteEvents#fireAddShaderPreProcessors}.
 */
@FunctionalInterface
public interface VeilAddShaderPreProcessorsEvent {

    void onRegisterShaderPreProcessors(ResourceProvider resourceProvider, Registry registry);

    interface Registry {

        void addPreprocessorFirst(ShaderPreProcessor processor, boolean modifyImports);

        default void addPreprocessorFirst(final ShaderPreProcessor processor) {
            this.addPreprocessorFirst(processor, true);
        }

        void addPreprocessor(ShaderPreProcessor processor, boolean modifyImports);

        default void addPreprocessor(final ShaderPreProcessor processor) {
            this.addPreprocessor(processor, true);
        }
    }
}
