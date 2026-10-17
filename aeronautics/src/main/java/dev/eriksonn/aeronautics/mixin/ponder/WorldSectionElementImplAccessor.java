package dev.eriksonn.aeronautics.mixin.ponder;

import com.zurrtum.create.client.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSectionElementImpl.class)
public interface WorldSectionElementImplAccessor {
    @Accessor(remap = false)
    Vec3 getCenterOfRotation();
}
