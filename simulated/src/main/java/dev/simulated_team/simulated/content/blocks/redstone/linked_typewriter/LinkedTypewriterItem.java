package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.zurrtum.create.content.redstone.link.ServerLinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class LinkedTypewriterItem extends BlockItem {
    public LinkedTypewriterItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    /**
     * @return The frequency of the redstone link or linked receiver at the given position, or null
     */
    public static Couple<RedstoneLinkNetworkHandler.Frequency> getLinkFrequency(final Level level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof final AbstractLinkedReceiverBlockEntity abe) {
            return abe.getFrequency();
        } else if (be instanceof final RedstoneLinkBlockEntity lbe) {
            final ServerLinkBehaviour link = lbe.getBehaviour(ServerLinkBehaviour.TYPE);
            return link != null ? link.getNetworkKey() : null;
        }

        return null;
    }

    //rclick redstone link -> prompt bind
    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();

        final BlockPos clickedPos = context.getClickedPos();

        final Couple<RedstoneLinkNetworkHandler.Frequency> frequency = getLinkFrequency(level, clickedPos);

        //should never be null here, but whatever
        if (frequency != null) {
            if (!level.isClientSide()) {
                return InteractionResult.CONSUME;
            }

            //the rest of this needs to be handled on the client...
            return LinkedTypewriterItemBindHandler.onLinkClicked(clickedPos);
        } else if (level.isClientSide()) {
            LinkedTypewriterItemBindHandler.reset();
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand usedHand) {
        final BlockHitResult blockHitResult = rayTraceRange(level, player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
        if (blockHitResult.getType() == HitResult.Type.MISS && level.isClientSide()) {
            LinkedTypewriterItemBindHandler.reset();
        }

        return super.use(level, player, usedHand);
    }

    /**
     * Same as Create's {@code RaycastHelper#rayTraceRange} (a client class in Create Fly)
     */
    private static BlockHitResult rayTraceRange(final Level level, final Player player, final double range) {
        final Vec3 origin = player.getEyePosition();
        final float yRot = -player.getYRot() * Mth.DEG_TO_RAD - (float) Math.PI;
        final float xRot = -player.getXRot() * Mth.DEG_TO_RAD;
        final float horizontal = -Mth.cos(xRot);
        final Vec3 target = origin.add(Mth.sin(yRot) * horizontal * range, Mth.sin(xRot) * range, Mth.cos(yRot) * horizontal * range);
        return level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay tooltipDisplay, final Consumer<Component> tooltipComponents, final TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        final TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null && data.contains("Keys")) {
            final int keyCount = data.getUnsafe().getListOrEmpty("Keys").size();
            tooltipComponents.accept(Component.translatable("simulated.linked_typewriter.key_count", keyCount).withStyle(ChatFormatting.GOLD));
        }
    }
}
