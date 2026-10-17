package dev.eriksonn.aeronautics.fabric.content.fluids.levitite;

import com.zurrtum.create.infrastructure.fluids.FlowableFluid;
import com.zurrtum.create.infrastructure.fluids.FluidEntry;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendDummyInterface;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Levitite Blend (upstream NeoForge module: {@code LevititeBlendNeoForge} as the source of a Registrate
 * {@code standardFluid}, {@code LevititeBlendFluidType} for entity movement). On Fabric it is a Create Fly
 * {@link FlowableFluid} (same flow parameters as Registrate's standard fluid: drop-off 2, tick rate 25, slope 3,
 * explosion resistance 100). The NeoForge fluid type behaviour lives in {@code fabric.mixin.levitite_blend}:
 * entities are pushed with motion scale {@link #MOTION_SCALE}, move like in lava with reduced gravity
 * ({@link #move}) and can't swim, drown or extinguish themselves in it.
 */
public final class LevititeBlendFluid {
	/**
	 * NeoForge {@code FluidType.Properties#motionScale}: the negative of lava's push.
	 */
	public static final double MOTION_SCALE = -7 / 3000f;

	/**
	 * NeoForge's default {@code FluidType.Properties#fallDistanceModifier}.
	 */
	public static final double FALL_DISTANCE_MODIFIER = 0.5;

	private LevititeBlendFluid() {
	}

	/**
	 * The source fluid: ticks the crystallization (upstream {@code LevititeBlendNeoForge}).
	 */
	public static class Source extends FlowableFluid.Still implements LevititeBlendDummyInterface {
		public Source(final FluidEntry entry) {
			super(entry);
		}

		@Override
		public void tick(final ServerLevel level, final BlockPos pos, final BlockState blockState, final FluidState state) {
			super.tick(level, pos, blockState, state);
			LevititeBlendDummyInterface.super.levititeBlendTick(level, pos, state);
		}

		@Override
		protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
			// NeoForge fluid types don't extinguish by default (Create Fly's fluids extend WaterFluid, which does)
		}

		@Override
		public Optional<SoundEvent> getPickupSound() {
			return Optional.of(AeroSoundEvents.LEVITITE_BLEND_FILL.event());
		}
	}

	public static class Flowing extends FlowableFluid.Flowing {
		public Flowing(final FluidEntry entry) {
			super(entry);
		}

		@Override
		protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
		}

		@Override
		public Optional<SoundEvent> getPickupSound() {
			return Optional.of(AeroSoundEvents.LEVITITE_BLEND_FILL.event());
		}
	}

	/**
	 * Upstream {@code LevititeBlendFluidType#move}: "effectively the lava movement code", with gravity reduced by how
	 * deep the entity is in the blend (unless crouching).
	 *
	 * @param fluidHeight the entity's immersion in the blend ({@code getFluidHeight(AeroTags.FluidTags.LEVITITE_BLEND)})
	 */
	public static void move(final LivingEntity entity, final Vec3 movementVector, double gravity, final double fluidHeight) {
		final double originalY = entity.getY();
		final boolean falling = entity.getDeltaMovement().y < 0;
		if (!entity.isCrouching()) { // if not crouching, reduce effective gravity based on immersion in fluid down to 0
			gravity = Math.clamp(gravity * (1f - fluidHeight),
					0, gravity);
		}
		entity.moveRelative(0.02F, movementVector);
		entity.move(MoverType.SELF, entity.getDeltaMovement());
		if (fluidHeight <= entity.getFluidJumpThreshold()) {
			entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.7F, 0.5));
			entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, falling, entity.getDeltaMovement()));
		} else {
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
		}

		if (gravity != 0.0) {
			entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -gravity / 4.0, 0.0));
		}

		final Vec3 interV = entity.getDeltaMovement();
		if (entity.horizontalCollision && entity.isFree(interV.x, interV.y + 0.6F - entity.getY() + originalY, interV.z)) {
			entity.setDeltaMovement(interV.x, 0.3F, interV.z);
		}
	}

	public static boolean is(final FluidState state) {
		return state.is(AeroTags.FluidTags.LEVITITE_BLEND);
	}
}
