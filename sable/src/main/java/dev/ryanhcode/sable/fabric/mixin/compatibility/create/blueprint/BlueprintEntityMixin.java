package dev.ryanhcode.sable.fabric.mixin.compatibility.create.blueprint;

import com.zurrtum.create.content.equipment.blueprint.BlueprintEntity;
import dev.ryanhcode.sable.annotation.MixinModVersionConstraint;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@MixinModVersionConstraint("(,6.0.11)")
@Mixin(BlueprintEntity.class)
public abstract class BlueprintEntityMixin extends Entity {
	public BlueprintEntityMixin(final EntityType<?> entityType, final Level level) {
		super(entityType, level);
	}

	/**
	 * @author IThundxr
	 * @reason Switch to Player#canInteractWithEntity (1.21.11: Player#isWithinEntityInteractionRange), which is patched by sable.
	 */
	@Overwrite(remap = false)
	public boolean canPlayerUse(final Player player) {
		return player.isWithinEntityInteractionRange(this, 8);
	}
}
