package dev.eriksonn.aeronautics.index;

import com.mojang.serialization.MapCodec;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.particle.AirPoofParticleData;
import dev.eriksonn.aeronautics.content.particle.GustParticleData;
import dev.eriksonn.aeronautics.content.particle.HotAirEmberParticleData;
import dev.eriksonn.aeronautics.content.particle.LevititeSparkleParticleData;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Particle types (upstream: an enum of Create {@code ICustomParticleData} factories, registered by the NeoForge
 * module's {@code AeroParticleTypesNeoForge}; Create Fly has no {@code ICustomParticleData}). Client providers are
 * registered by the particle content's client hook.
 */
public class AeroParticleTypes {
    public static final ParticleType<PropellerAirParticleData> PROPELLER_AIR_FLOW = register(
            "propeller_air_flow", PropellerAirParticleData.CODEC, PropellerAirParticleData.STREAM_CODEC);
    public static final ParticleType<HotAirEmberParticleData> HOT_AIR_EMBER = register(
            "hot_air_ember", HotAirEmberParticleData.CODEC, HotAirEmberParticleData.STREAM_CODEC);
    public static final ParticleType<LevititeSparkleParticleData> LEVITITE_SPARKLE = register(
            "levitite_sparkle", LevititeSparkleParticleData.CODEC, LevititeSparkleParticleData.STREAM_CODEC);
    public static final ParticleType<GustParticleData> GUST = register(
            "gust", GustParticleData.CODEC, GustParticleData.STREAM_CODEC);
    public static final ParticleType<AirPoofParticleData> AIR_POOF = register(
            "air_poof", AirPoofParticleData.CODEC, AirPoofParticleData.STREAM_CODEC);

    private static <T extends ParticleOptions> ParticleType<T> register(final String name, final MapCodec<T> codec,
                                                                        final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        // alwaysSpawn false, like Create's ICustomParticleData#createType
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Aeronautics.path(name), FabricParticleTypes.complex(codec, streamCodec));
    }

    public static void register() {
    }
}
