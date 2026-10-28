package dev.ryanhcode.offroad.config.server;

import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.catnip.config.ConfigBase;

public class OffroadServer extends ConfigBase {
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

	public final OffroadBlockConfigs blocks = this.nested(0, OffroadBlockConfigs::new, Comments.blockConfig);
	public final OffroadKinetics kinetics = this.nested(0, OffroadKinetics::new, Comments.kinetics);

	@Override
	public String getName() {
		return "server";
	}

	private static class Comments {
		static String kinetics = "Parameters and abilities of Offroad's kinetic mechanisms";
		static String blockConfig = "Parameters and abilities of Offroad Blocks";
	}
}
