package dev.ryanhcode.sable.fabric.mixin.compatibility.create.redstone_contacts;

import com.zurrtum.create.content.redstone.contact.RedstoneContactBlock;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.block.WrenchableDirectionalBlock;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.redstone_contact.RedstoneContactBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RedstoneContactBlock.class)
public class RedstoneContactBlockMixin extends WrenchableDirectionalBlock implements IBE<RedstoneContactBlockEntity> {

    public RedstoneContactBlockMixin(final Properties properties) {
        super(properties);
    }

    @Override
    public Class<RedstoneContactBlockEntity> getBlockEntityClass() {
        return RedstoneContactBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneContactBlockEntity> getBlockEntityType() {
        return RedstoneContactBlockEntity.TYPE;
    }

    @Override
    public <S extends BlockEntity> BlockEntityTicker<S> getTicker(final Level level, final BlockState p_153213_, final BlockEntityType<S> p_153214_) {
        if (!level.isClientSide()) {
            return IBE.super.getTicker(level, p_153213_, p_153214_);
        } else {
            return null;
        }
    }
}
