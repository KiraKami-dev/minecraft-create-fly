package dev.simulated_team.simulated.index.fabric;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimStats;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/**
 * Fabric registration of {@link SimStats} (upstream: {@code NeoForgeSimStats}). Like vanilla's
 * {@code Stats.makeCustomStat}, the stat is created right away so it shows in the statistics menu before it is awarded.
 */
public class FabricSimStats extends SimStats {

    public static void register() {
        new FabricSimStats().init();
    }

    @Override
    protected SimStats.Stat makeCustomStat(final String key, final StatFormatter formatter) {
        final Identifier id = Simulated.path(key);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
        Stats.CUSTOM.get(id, formatter);
        return new SimStats.Stat(() -> id, formatter);
    }
}
