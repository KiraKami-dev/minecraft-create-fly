package dev.simulated_team.simulated.content.particle;

import dev.simulated_team.simulated.index.SimParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/**
 * Client-only. Registers the particle providers of Simulated's particle types
 * (upstream: {@code SimParticleTypes.registerClientParticles} from NeoForge's {@code RegisterParticleProvidersEvent}).
 */
public class SimParticleProviders {

    public static void registerClient() {
        final ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(SimParticleTypes.MAGNET_FIELD, MagnetFieldParticle.Factory::new);
        registry.register(SimParticleTypes.MAGNET_FIELD2, MagnetFieldParticle2.Factory::new);
        registry.register(SimParticleTypes.AUGER_INDICATOR, AugerIndicatorParticle.Factory::new);
    }
}
