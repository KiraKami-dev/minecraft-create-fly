package dev.eriksonn.aeronautics.content.particle;

import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/**
 * Client-only. Registers the particle providers of Aeronautics' particle types
 * (upstream: {@code AeroParticleTypes.registerClientParticles} from NeoForge's {@code RegisterParticleProvidersEvent}).
 */
public class AeroParticleProviders {

    public static void registerClient() {
        final ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(AeroParticleTypes.PROPELLER_AIR_FLOW, PropellerAirParticle.Factory::new);
        registry.register(AeroParticleTypes.HOT_AIR_EMBER, HotAirEmberParticle.Factory::new);
        registry.register(AeroParticleTypes.LEVITITE_SPARKLE, LevititeSparkleParticle.Factory::new);
        registry.register(AeroParticleTypes.GUST, GustParticle.Factory::new);
        registry.register(AeroParticleTypes.AIR_POOF, AirPoofParticle.Factory::new);
    }
}
