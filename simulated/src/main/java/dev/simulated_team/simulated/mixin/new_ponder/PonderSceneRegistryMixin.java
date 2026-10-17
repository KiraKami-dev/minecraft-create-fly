package dev.simulated_team.simulated.mixin.new_ponder;

import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import com.zurrtum.create.client.ponder.foundation.PonderScene;
import com.zurrtum.create.client.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PonderSceneRegistry.class)
public class PonderSceneRegistryMixin {
	// compile(Collection) is an overload, hence the descriptor; it holds Identifier, so remapping stays on
	@Inject(method = "compile(Lnet/minecraft/resources/Identifier;)Ljava/util/List;", at = @At("RETURN"))
	private void simulated$compile(final Identifier id, final CallbackInfoReturnable<List<PonderScene>> cir) {
		final List<PonderScene> scenes = cir.getReturnValue();
		if (!scenes.isEmpty()) {
			NewPonderTooltipManager.setSceneWatched(scenes.getFirst().getId());
		}
	}
}
