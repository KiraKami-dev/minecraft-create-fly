package dev.ryanhcode.sable.fabric.mixin.compatibility.create.stock_ticker;

import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.content.equipment.hats.HatFeatureRenderer;
import com.zurrtum.create.client.content.equipment.hats.HatState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.contraptions.actors.seat.SeatEntity;
import com.zurrtum.create.content.logistics.stockTicker.StockTickerBlock;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes the logistics hat of entities seated next to a stock ticker on a sub-level appear.
 * <p>
 * Upstream redirected {@code EntityHats#getLogisticsHatFor} to search around the (sub-level) seat instead of the
 * passenger. Create Fly inlines that search into its own {@code LivingEntityRendererMixin#addHat}, which runs at the
 * tail of {@link LivingEntityRenderer#extractRenderState}, so this re-does the logistics hat decision right after it
 * (higher priority = applied later) using the root vehicle's position when that vehicle is in a sub-level.
 */
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class EntityHatsMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Shadow
    @Final
    protected List<RenderLayer<S, M>> layers;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void sable$getStockTickerPosition(final T entity, final S renderState, final float partialTick, final CallbackInfo ci) {
        if (!entity.isPassenger() || !(entity.getVehicle() instanceof SeatEntity)) {
            return;
        }

        if (!(renderState instanceof final HatState state) || this.layers.stream().noneMatch(feature -> feature instanceof HatFeatureRenderer)) {
            return;
        }

        final PartialModel currentHat = state.create$getHat();
        if (currentHat != null && currentHat != AllPartialModels.LOGISTICS_HAT) {
            return;
        }

        final Entity vehicle = entity.getRootVehicle();
        if (Sable.HELPER.getContaining(vehicle) == null) {
            return;
        }

        if (sable$hasSingleStockTicker(entity.level(), vehicle.blockPosition())) {
            if (currentHat == null) {
                state.create$setHat(AllPartialModels.LOGISTICS_HAT);
                state.create$updateHatInfo(entity);
            }
        } else if (currentHat != null) {
            state.create$setHat(null);
        }
    }

    /**
     * The logistics hat check of Create's {@code EntityHats#getLogisticsHatFor}: exactly one stock ticker next to the
     * seat (horizontally, at its height or one above).
     */
    @Unique
    private static boolean sable$hasSingleStockTicker(final Level level, final BlockPos pos) {
        int stations = 0;
        for (final Direction d : Iterate.horizontalDirections) {
            for (final int y : Iterate.zeroAndOne) {
                if (level.getBlockState(pos.relative(d).above(y)).getBlock() instanceof StockTickerBlock) {
                    stations++;
                }
            }
        }
        return stations == 1;
    }
}
