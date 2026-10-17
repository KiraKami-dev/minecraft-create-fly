package dev.simulated_team.simulated.config.server.blocks;

import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.infrastructure.config.CStress;
import dev.simulated_team.simulated.Simulated;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class SimStress extends CStress {

    // IDs need to be used since configs load before registration
    private static final Object2DoubleMap<Identifier> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap<>();
    private static final Object2DoubleMap<Identifier> DEFAULT_CAPACITIES = new Object2DoubleOpenHashMap<>();

    @Override
    public void registerAll(final Builder builder) {
        builder.comment(".", Comments.su, Comments.impact).push("impact");
        DEFAULT_IMPACTS.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));
        builder.pop();
        builder.comment(".", Comments.su, Comments.capacity).push("capacity");
        DEFAULT_CAPACITIES.forEach((id, value) -> this.capacities.put(id, builder.define(id.getPath(), value)));
        builder.pop();
    }

    public static void setNoImpact(final Block block) {
        setImpact(block, 0.0);
    }

    /**
     * Registers the default stress impact of a Simulated block. Must be called before the server config is created.
     */
    public static void setImpact(final Block block, final double value) {
        DEFAULT_IMPACTS.put(idOf(block), value);
    }

    /**
     * Registers the default stress capacity of a Simulated block. Must be called before the server config is created.
     */
    public static void setCapacity(final Block block, final double value) {
        DEFAULT_CAPACITIES.put(idOf(block), value);
    }

    private static Identifier idOf(final Block block) {
        final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (!id.getNamespace().equals(Simulated.MOD_ID)) {
            throw new IllegalStateException("Non-Simulated blocks cannot be added to Simulated's config.");
        }
        return id;
    }

    private static class Comments {
        static String su = "[in Stress Units]";
        static String impact =
                "Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
        static String capacity = "Configure how much stress a source can accommodate for.";
    }
}
