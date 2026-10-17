package dev.ryanhcode.sable.fabric.mixin.compatibility.create.behaviour_compatibility.harvester_block_entity;

import com.zurrtum.create.content.contraptions.actors.AttachedActorBlock;
import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.zurrtum.create.foundation.block.IBE;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.harvester.HarvesterTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HarvesterBlock.class)
public abstract class HarvesterBlockMixin extends AttachedActorBlock implements IBE<HarvesterBlockEntity> {
    protected HarvesterBlockMixin(final Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <S extends BlockEntity> BlockEntityTicker<S> getTicker(final Level level, final BlockState state, final BlockEntityType<S> blockEntityType) {
        if (level.isClientSide()) {
            return new HarvesterTicker();
        } else {
            return null;
        }
    }
}
