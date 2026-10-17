package dev.eriksonn.aeronautics.config.server;

import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.catnip.config.ConfigBase;

public class AeroServer extends ConfigBase {
	private Builder builder;

	@Override
	public void registerAll(final Builder builder) {
		super.registerAll(builder);
		this.builder = builder;
	}

	/**
	 * Re-reads the config file (upstream: NeoForge's config reload event; Create Fly's {@code CServer#reload}).
	 */
	public void reload() {
		if (this.builder == null) {
			return;
		}
		this.builder.read();
		this.depth = 0;
		super.registerAll(this.builder);
	}

	public final AeroPhysics physics = this.nested(0, AeroPhysics::new, Comments.physics);
	public final AeroBlockConfigs blocks = this.nested(0, AeroBlockConfigs::new, Comments.blockConfig);
	public final AeroKinetics kinetics = this.nested(0, AeroKinetics::new, Comments.kinetics);

	@Override
	public String getName() {
		return "server";
	}

	private static class Comments {
		static String kinetics = "Parameters and abilities of Aeronautics's kinetic mechanisms";
		static String physics = "Parameters related to the physics of Aeronautics blocks";
		static String blockConfig = "Parameters and abilities of Aeronautics Blocks";
	}
}
