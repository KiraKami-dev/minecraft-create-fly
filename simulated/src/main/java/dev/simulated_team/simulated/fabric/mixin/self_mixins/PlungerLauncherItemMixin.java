package dev.simulated_team.simulated.fabric.mixin.self_mixins;

import com.zurrtum.create.foundation.item.SwingControlItem;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * NeoForge's {@code IItemExtension#onEntitySwing}: the plunger launcher never swings. Create Fly cancels the swing for
 * {@link SwingControlItem}s in its {@code LivingEntityMixin}.
 * <p>
 * The upstream mixin also attached the custom item renderer ({@code initializeClient}); on 1.21.11 item renderers are
 * registered by whoever registers the plunger launcher's client renderers.
 */
@Mixin(PlungerLauncherItem.class)
public abstract class PlungerLauncherItemMixin implements SwingControlItem {

    @Override
    public boolean onEntitySwing(final ItemStack stack, final LivingEntity entity, final InteractionHand hand) {
        return true;
    }
}
