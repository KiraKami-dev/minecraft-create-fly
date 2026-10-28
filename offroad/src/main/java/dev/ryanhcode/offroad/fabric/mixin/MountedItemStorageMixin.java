package dev.ryanhcode.offroad.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorage;
import com.zurrtum.create.content.contraptions.Contraption;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadAttachedStorage;
import dev.ryanhcode.offroad.content.contraptions.borehead_contraption.BoreheadBearingContraption;
import dev.ryanhcode.offroad.fabric.transfer.BoreheadMenuContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Upstream: NeoForge's {@code MountedItemStorageMixin}. Storage menus opened on a borehead contraption unstall it when
 * items are taken out.
 */
@Mixin(MountedItemStorage.class)
public class MountedItemStorageMixin {

    @WrapMethod(method = "getHandlerForMenu", remap = false)
    private Container offroad$wrapHandler(final StructureTemplate.StructureBlockInfo info, final Contraption contraption, final Operation<Container> original) {
        final Container originalCall = original.call(info, contraption);
        if (contraption instanceof BoreheadBearingContraption && originalCall != null) {
            return new BoreheadMenuContainer((BoreheadAttachedStorage) contraption.getStorage(), originalCall);
        }

        return originalCall;
    }
}
