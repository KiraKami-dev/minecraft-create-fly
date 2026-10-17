package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.AllHandle;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.zurrtum.create.infrastructure.packet.c2s.ChainPackageInteractionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Initialises the world position of packages hung onto a chain conveyor by a player, so that the cross-sub-level
 * frogport distance checks have a position to work with right away.
 * <br>
 * Create Fly handles {@code ChainPackageInteractionPacket} in {@link AllHandle#onChainPackageInteraction}, whose
 * block entity callback is the lambda targeted here (upstream: {@code ChainPackageInteractionPacket#applySettings}).
 */
@Mixin(AllHandle.class)
public class ChainPackageInteractionPacketMixin {

    @Inject(method = "lambda$onChainPackageInteraction$16", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;addLoopingPackage(Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorPackage;)Z"), remap = false)
    private static void sable$initialiseLoopingWorldPosition(final ChainPackageInteractionPacket packet,
                                                             final ServerPlayer player,
                                                             final BlockEntity blockEntity,
                                                             final CallbackInfoReturnable<Boolean> cir,
                                                             @Local(name = "be") final ChainConveyorBlockEntity be,
                                                             @Local(name = "chainConveyorPackage") final ChainConveyorPackage chainConveyorPackage) {
        chainConveyorPackage.worldPosition = be.getPackagePosition(packet.chainPosition(), null);
    }

    @Inject(method = "lambda$onChainPackageInteraction$16", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;addTravellingPackage(Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorPackage;Lnet/minecraft/core/BlockPos;)Z"), remap = false)
    private static void sable$initialiseTravellingWorldPosition(final ChainPackageInteractionPacket packet,
                                                                final ServerPlayer player,
                                                                final BlockEntity blockEntity,
                                                                final CallbackInfoReturnable<Boolean> cir,
                                                                @Local(name = "be") final ChainConveyorBlockEntity be,
                                                                @Local(name = "chainConveyorPackage") final ChainConveyorPackage chainConveyorPackage) {
        chainConveyorPackage.worldPosition = be.getPackagePosition(packet.chainPosition(), packet.selectedConnection());
    }
}
