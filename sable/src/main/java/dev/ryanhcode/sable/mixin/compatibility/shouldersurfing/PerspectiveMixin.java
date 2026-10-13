package dev.ryanhcode.sable.mixin.compatibility.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.CrosshairVisibility;
import com.github.exopandora.shouldersurfing.api.client.Perspective;
import com.github.exopandora.shouldersurfing.api.config.IPerspectiveConfig;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.mixinhelpers.compatibility.shouldersurfing.SablePerspectives;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Perspective.class)
public class PerspectiveMixin {

    @Shadow
    @Final
    @Mutable
    private static Perspective[] $VALUES;

    static {
        final var subLevelView = create("SUB_LEVEL_VIEW", $VALUES.length, SableCameraTypes.SUB_LEVEL_VIEW, CrosshairVisibility.NEVER);

        $VALUES = ArrayUtils.add($VALUES, subLevelView);

        final var subLevelViewUnlocked = create("SUB_LEVEL_VIEW_UNLOCKED", $VALUES.length, SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED, CrosshairVisibility.NEVER);

        $VALUES = ArrayUtils.add($VALUES, subLevelViewUnlocked);
    }

    @SuppressWarnings("SameParameterValue")
    @Invoker(value = "<init>")
    private static Perspective create(final String name, final int ordinal, final CameraType cameraType, final CrosshairVisibility defaultCrosshairVisibility) {
        throw new AssertionError("Unreachable");
    }

    /**
     * Inserts the sub-level views into the perspective cycle:
     * first person, back, sub-level view, sub-level view (unlocked), front, shoulder surfing.
     */
    @SuppressWarnings("ConstantValue")
    @Inject(method = "next", at = @At("HEAD"), cancellable = true)
    public void sable$next(final IPerspectiveConfig config, final CallbackInfoReturnable<Perspective> cir) {
        final Object self = this;
        Perspective result = null;

        if (self == SablePerspectives.SUB_LEVEL_VIEW) {
            result = SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED;
        } else if (self == SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED) {
            result = Perspective.THIRD_PERSON_FRONT;
        } else if (config.isThirdPersonReplaced()) {
            if (self == Perspective.SHOULDER_SURFING) {
                result = SablePerspectives.SUB_LEVEL_VIEW;
            }
        } else {
            // The normal logic would wrap around to our new values, but the next one should be first person
            if (self == Perspective.SHOULDER_SURFING) {
                result = Perspective.FIRST_PERSON;
            } else if (self == Perspective.THIRD_PERSON_BACK) {
                result = SablePerspectives.SUB_LEVEL_VIEW;
            }
        }

        if (result != null) {
            cir.setReturnValue(result.isEnabled(config) ? result : result.next(config));
        }
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    public void isEnabled(final IPerspectiveConfig config, final CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == SablePerspectives.SUB_LEVEL_VIEW || (Object) this == SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void of(final CameraType cameraType, final boolean shoulderSurfing, final CallbackInfoReturnable<Perspective> cir) {
        if (cameraType == SableCameraTypes.SUB_LEVEL_VIEW) {
            cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW);
        }
        if (cameraType == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED);
        }
    }
}
