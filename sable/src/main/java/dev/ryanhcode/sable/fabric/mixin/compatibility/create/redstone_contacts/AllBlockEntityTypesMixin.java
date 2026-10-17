package dev.ryanhcode.sable.fabric.mixin.compatibility.create.redstone_contacts;

import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.Create;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.redstone_contact.RedstoneContactBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the {@code create:redstone_contact} block entity type together with Create's own block entity types.
 * Upstream added a Registrate entry to {@code AllBlockEntityTypes}; Create Fly registers its types in the class's static
 * initializer, so ours is registered at its end.
 */
@Mixin(AllBlockEntityTypes.class)
public class AllBlockEntityTypesMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void sable$registerRedstoneContact(final CallbackInfo ci) {
        RedstoneContactBlockEntity.TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Create.MOD_ID, "redstone_contact"),
                FabricBlockEntityTypeBuilder.<RedstoneContactBlockEntity>create(RedstoneContactBlockEntity::new, AllBlocks.REDSTONE_CONTACT).build()
        );
    }
}
