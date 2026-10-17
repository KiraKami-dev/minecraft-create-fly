package dev.eriksonn.aeronautics.mixinterface;

/**
 * Exposes the steam vents counted by {@code BoilerDataMixin} to the (client) boiler goggle tooltip.
 */
public interface BoilerDataExtension {

	int aeronautics$getAttachedVents();

	float aeronautics$getPassiveEngineEfficiency();
}
