package dev.ryanhcode.sable.fabric.mixin.compatibility.create.assembly;

import com.zurrtum.create.content.decoration.placard.PlacardBlock;
import com.zurrtum.create.content.fluids.drain.ItemDrainBlock;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlock;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmBlock;
import com.zurrtum.create.content.logistics.depot.EjectorBlock;
import com.zurrtum.create.content.logistics.tableCloth.TableClothBlock;
import com.zurrtum.create.content.logistics.tunnel.BrassTunnelBlock;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.assembly.MovedBlockEntityContents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Blocks whose block entities drop their contents when removed: once assembly has copied them into the plot, the
 * originals are emptied so removing them doesn't duplicate the items (see {@link MovedBlockEntityContents}).
 */
@Mixin({DeployerBlock.class, ItemDrainBlock.class, EjectorBlock.class, ArmBlock.class, PlacardBlock.class, BrassTunnelBlock.class, TableClothBlock.class})
public class MovedBlockEntityContentsMixin implements BlockSubLevelAssemblyListener {

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void afterMove(final ServerLevel originLevel, final ServerLevel resultingLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {
        MovedBlockEntityContents.clear(originLevel.getBlockEntity(oldPos));
    }
}
