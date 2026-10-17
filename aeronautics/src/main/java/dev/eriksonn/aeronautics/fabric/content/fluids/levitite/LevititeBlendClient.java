package dev.eriksonn.aeronautics.fabric.content.fluids.levitite;

import com.zurrtum.create.client.AllFluidConfigs;
import com.zurrtum.create.client.infrastructure.config.AllConfigs;
import com.zurrtum.create.client.infrastructure.fluid.FluidConfig;
import dev.eriksonn.aeronautics.index.AeroFluids;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.util.AeroColors;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Client half of Levitite Blend (upstream: {@code AeroFluidType}'s {@code IClientFluidTypeExtensions}, registered by
 * the NeoForge module's client events).
 * <ul>
 *     <li>world rendering: Fabric's fluid render handler (still/flow textures, no tint);</li>
 *     <li>Create Fly's {@link AllFluidConfigs} entry, which Create uses for fluids in tanks, pipes, spouts, items;</li>
 *     <li>fog while the camera is inside the blend: {@link Fog}.</li>
 * </ul>
 */
public final class LevititeBlendClient {
	private LevititeBlendClient() {
	}

	/**
	 * Upstream: {@code 1/32 * chocolateTransparencyMultiplier} of the 96 block water fog, like Create's chocolate.
	 */
	private static float fogEnd() {
		return 96.0f * (1f / 32f * AllConfigs.client().chocolateTransparencyMultiplier.getF());
	}

	public static void register() {
		final Identifier id = BuiltInRegistries.FLUID.getKey(AeroFluids.LEVITITE_BLEND).withPrefix("fluid/");
		final Identifier still = id.withSuffix("_still");
		final Identifier flow = id.withSuffix("_flow");

		FluidRenderHandlerRegistry.INSTANCE.register(AeroFluids.LEVITITE_BLEND, AeroFluids.getFlowing(AeroFluids.LEVITITE_BLEND),
				new SimpleFluidRenderHandler(still, flow));

		final FluidConfig config = new FluidConfig(
				sprite(still),
				sprite(flow),
				component -> -1,
				LevititeBlendClient::fogEnd,
				AeroColors.LEVIBLEND_THE_FOG_IS_COMING
		);
		AllFluidConfigs.ALL.put(AeroFluids.LEVITITE_BLEND, config);
		AllFluidConfigs.ALL.put(AeroFluids.getFlowing(AeroFluids.LEVITITE_BLEND), config);

		// before vanilla's (and Create Fly's) environments; it only applies inside the blend
		FogRenderer.FOG_ENVIRONMENTS.addFirst(new Fog());
	}

	@SuppressWarnings("deprecation")
	private static Supplier<TextureAtlasSprite> sprite(final Identifier texture) {
		return () -> Minecraft.getInstance().getAtlasManager().get(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
	}

	/**
	 * Upstream {@code AeroFluidType#modifyFogColor/modifyFogRender}: Levitite Blend's fog color, fog from -8 blocks to
	 * {@link #fogEnd()} while the camera is below the blend's surface (NeoForge's fluid fog check).
	 */
	public static class Fog extends FogEnvironment {
		@Override
		public void setupFog(final FogData fogData, final Camera camera, final ClientLevel level, final float viewDistance, final DeltaTracker deltaTracker) {
			fogData.environmentalStart = -8;
			fogData.environmentalEnd = fogEnd();
			fogData.skyEnd = fogData.environmentalEnd;
			fogData.cloudEnd = fogData.environmentalEnd;
		}

		@Override
		public int getBaseColor(final ClientLevel level, final Camera camera, final int renderDistance, final float partialTick) {
			return AeroColors.LEVIBLEND_THE_FOG_IS_COMING;
		}

		@Override
		public boolean isApplicable(@Nullable final FogType fogType, final Entity entity) {
			final Minecraft minecraft = Minecraft.getInstance();
			final ClientLevel level = minecraft.level;
			final Camera camera = minecraft.gameRenderer.getMainCamera();
			if (level == null || !camera.isInitialized()) {
				return false;
			}

			final BlockPos pos = camera.blockPosition();
			final FluidState state = level.getFluidState(pos);
			return state.is(AeroTags.FluidTags.LEVITITE_BLEND) && camera.position().y < pos.getY() + state.getHeight(level, pos);
		}
	}
}
