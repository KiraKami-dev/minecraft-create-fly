package dev.ryanhcode.offroad.data;

import dev.ryanhcode.offroad.Offroad;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Offroad's tags. Their contents are shipped as JSON (upstream generated them: {@code borehead_effective} = the common
 * {@code c:ores}, {@code borehead_super_effective} = ancient debris).
 */
public class OffroadTags {

    public static class Blocks {

        public static final TagKey<Block> BOREHEAD_EFFECTIVE = create("borehead_effective");
        public static final TagKey<Block> BOREHEAD_SUPER_EFFECTIVE = create("borehead_super_effective");

        private static TagKey<Block> create(final String path) {
            return TagKey.create(Registries.BLOCK, Offroad.path(path));
        }

        private static TagKey<Block> create(final String namespace, final String path) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
        }
    }
}
