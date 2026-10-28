package dev.ryanhcode.offroad.index;

import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.components.TireLike;

import java.util.stream.Stream;

public class OffroadPartialModels {

	public static final PartialModel
			DIODE_LEFT = block("wheel_mount/diode_left"),
			DIODE_RIGHT = block("wheel_mount/diode_right"),
			TELE_OUTER = block("wheel_mount/tele_outer"),
			TELE_INNER = block("wheel_mount/tele_inner"),
			TELE_MOUNT = block("wheel_mount/mount"),
			SPRING_UPPER = block("wheel_mount/spring_upper"),
			SPRING_MIDDLE = block("wheel_mount/spring_middle"),
			SPRING_LOWER = block("wheel_mount/spring_lower"),
			ROCK_CUTTING_WHEEL_WHEEL = block("rockcutting_wheel/wheel");

	/**
	 * PORT(1.21.11): the models of the tire components ({@link TireLike#model()}), registered before models load:
	 * Create Fly's partial models throw if created afterwards (upstream created them while rendering). Kept here so
	 * the weakly held entries stay loaded.
	 */
	public static final PartialModel[] TIRE_MODELS = Stream.of(TireLike.ROCKCUTTING_WHEEL, TireLike.MECHANICAL_ROLLER)
			.map(tire -> PartialModel.of(tire.model().orElseThrow()))
			.toArray(PartialModel[]::new);

	private static PartialModel block(final String path) {
		return PartialModel.of(Offroad.path("block/" + path));
	}
	private static PartialModel entity(final String path) {
		return PartialModel.of(Offroad.path("entity/" + path));
	}
	private static PartialModel item(final String path) {
		return PartialModel.of(Offroad.path("item/" + path));
	}

	public static void init() {
	}
}
