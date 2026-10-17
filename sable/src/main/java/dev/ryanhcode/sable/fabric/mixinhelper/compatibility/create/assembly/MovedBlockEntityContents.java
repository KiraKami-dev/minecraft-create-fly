package dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.assembly;

import com.zurrtum.create.content.decoration.placard.PlacardBlockEntity;
import com.zurrtum.create.content.fluids.drain.ItemDrainBlockEntity;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.kinetics.deployer.DeployerPlayer;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.zurrtum.create.content.logistics.depot.EjectorBlockEntity;
import com.zurrtum.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.zurrtum.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-level assembly copies a block entity's data into the plot and then removes the original block, emptying it
 * first only if it is {@link net.minecraft.world.Clearable}. Create Fly drops these block entities' contents from
 * {@code preRemoveSideEffects}/{@code destroy()} (1.21.11 removal hooks without the old {@code onRemove} "is moving"
 * flag) and they are not (fully) {@code Clearable}, so every moved item would also be dropped in the world.
 * Empties the original once its data has been copied.
 */
public final class MovedBlockEntityContents {

    private MovedBlockEntityContents() {
    }

    public static void clear(@Nullable final BlockEntity blockEntity) {
        if (blockEntity instanceof final DeployerBlockEntity deployer) {
            // DeployerBlockEntity#clearContent only clears the filter; discardPlayer drops the fake player's items
            final DeployerPlayer player = deployer.getPlayer();
            if (player != null) {
                player.cast().getInventory().clearContent();
            }
            deployer.overflowItems.clear();
        } else if (blockEntity instanceof final ItemDrainBlockEntity drain) {
            drain.heldItem = null;
        } else if (blockEntity instanceof final EjectorBlockEntity ejector) {
            ejector.depotBehaviour.clearContent();
        } else if (blockEntity instanceof final ArmBlockEntity arm) {
            arm.heldItem = ItemStack.EMPTY;
        } else if (blockEntity instanceof final PlacardBlockEntity placard) {
            placard.setHeldItem(ItemStack.EMPTY);
        } else if (blockEntity instanceof final BrassTunnelBlockEntity tunnel) {
            tunnel.setStackToDistribute(ItemStack.EMPTY, null);
        } else if (blockEntity instanceof final TableClothBlockEntity tableCloth) {
            tableCloth.manuallyAddedItems.clear();
        }
    }
}
