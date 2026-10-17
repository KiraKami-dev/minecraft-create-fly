package dev.simulated_team.simulated.index;

import com.mojang.serialization.MapCodec;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.particle.AugerIndicatorParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData2;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Particle types. Client providers are registered in {@code content.particle.SimParticleProviders}.
 */
public class SimParticleTypes {

    public static final ParticleType<MagnetFieldParticleData> MAGNET_FIELD = register(
            "magnet_field",
            MagnetFieldParticleData.CODEC,
            MagnetFieldParticleData.STREAM_CODEC
    );
    public static final ParticleType<MagnetFieldParticleData2> MAGNET_FIELD2 = register(
            "magnet_field2",
            MagnetFieldParticleData2.CODEC,
            MagnetFieldParticleData2.STREAM_CODEC
    );
    public static final ParticleType<AugerIndicatorParticleData> AUGER_INDICATOR = register(
            "auger_indicator",
            AugerIndicatorParticleData.CODEC,
            AugerIndicatorParticleData.STREAM_CODEC
    );

    private static <T extends ParticleOptions> ParticleType<T> register(final String name, final MapCodec<T> codec,
                                                                        final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Simulated.path(name), FabricParticleTypes.complex(codec, streamCodec));
    }

    public static void register() {
    }
}
