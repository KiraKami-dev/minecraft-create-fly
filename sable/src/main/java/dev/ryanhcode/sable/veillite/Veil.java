/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite;

import dev.ryanhcode.sable.veillite.platform.VeilPlatform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-rendering entry points of the veillite shim.
 */
public final class Veil {

    /**
     * Namespace used for veil resources (shader includes such as {@code veil:light}).
     */
    public static final String MODID = "veil";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sable/veillite");
    public static final boolean DEBUG = System.getProperty("veil.debug") != null;
    public static final boolean VERBOSE_SHADER_ERRORS = System.getProperty("veil.verboseShaderErrors") != null;

    public static final boolean SODIUM = platform().isModLoaded("sodium");
    public static final boolean IRIS = platform().isModLoaded("iris");
    public static final boolean IMGUIMC = platform().isModLoaded("imguimc");

    private Veil() {
    }

    public static Identifier veilPath(final String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static VeilPlatform platform() {
        return VeilPlatform.INSTANCE;
    }

    /**
     * @return Whether the current physical side is the client
     */
    public static boolean isClient() {
        return VeilPlatform.INSTANCE.isClient();
    }
}
