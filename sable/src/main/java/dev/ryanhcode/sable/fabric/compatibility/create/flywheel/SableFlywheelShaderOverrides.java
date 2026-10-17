package dev.ryanhcode.sable.fabric.compatibility.create.flywheel;

import dev.ryanhcode.sable.Sable;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Sable replaces some of Flywheel's internal shaders so lighting respects sub-level lighting scenes
 * (see {@code assets/sable/flywheel_overrides/explanation.md}).
 * <p>
 * Upstream shipped the replacements at Flywheel's own paths and relied on the mod resource order. Fabric doesn't
 * guarantee which mod's copy wins, so Sable's copies live at {@code sable:flywheel_overrides/<path>} and Create Fly's
 * shader loader is pointed at them explicitly.
 */
public final class SableFlywheelShaderOverrides {
    private static final String FLYWHEEL_NAMESPACE = "flywheel";
    private static final String OVERRIDE_DIR = "flywheel_overrides/";

    private SableFlywheelShaderOverrides() {
    }

    /**
     * @param manager  the resource manager Flywheel's shader sources are loading from
     * @param location the shader location, without Flywheel's {@code flywheel/} directory prefix (e.g. {@code flywheel:internal/light_lut.glsl})
     * @param resource the resource Flywheel resolved
     * @return Sable's replacement for the shader if there is one, otherwise {@code resource}
     */
    public static Resource apply(final ResourceManager manager, final Identifier location, final Resource resource) {
        if (!FLYWHEEL_NAMESPACE.equals(location.getNamespace())) {
            return resource;
        }

        return manager.getResource(Sable.sablePath(OVERRIDE_DIR + location.getPath())).orElse(resource);
    }
}
