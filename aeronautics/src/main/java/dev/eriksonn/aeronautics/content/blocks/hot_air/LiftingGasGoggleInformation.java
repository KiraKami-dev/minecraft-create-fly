package dev.eriksonn.aeronautics.content.blocks.hot_air;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider.ClientBalloonInfo;
import dev.eriksonn.aeronautics.data.AeroLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Client-only goggle information of lifting gas providers (upstream: {@code BlockEntityLiftingGasProvider#addBalloonGoggleInformation}
 * and the burner / steam vent {@code addToGoggleTooltip}). Create Fly reads goggle information from tooltip behaviours.
 */
public final class LiftingGasGoggleInformation {
    private LiftingGasGoggleInformation() {
    }

    /**
     * Upstream {@code HotAirBurnerBlockEntity#addToGoggleTooltip} / {@code SteamVentBlockEntity#addToGoggleTooltip}
     */
    public static class LiftingGasTooltipBehaviour<T extends SmartBlockEntity & BlockEntityLiftingGasProvider> extends TooltipBehaviour<T> implements IHaveGoggleInformation {
        public LiftingGasTooltipBehaviour(final T be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
            if (!this.blockEntity.canOutputGas()) return false;

            AeroLang.blockName(this.blockEntity.getBlockState()).text(":").forGoggles(tooltip, 1);
            final ClientBalloonInfo info = this.blockEntity.getClientBalloonInfo();
            if (info != null) {
                addBalloonGoggleInformation(this.blockEntity, tooltip, info, this.blockEntity.getTicksSinceSync(),
                        this.blockEntity.getAirPressure(info, this.blockEntity.getLevel()));
            }
            return true;
        }
    }

    /**
     * Upstream {@code BlockEntityLiftingGasProvider#addBalloonGoggleInformation}
     */
    public static void addBalloonGoggleInformation(final BlockEntityLiftingGasProvider provider, final List<Component> tooltip,
                                                   final ClientBalloonInfo info, final int ticksSinceSync, final double airPressure) {
        if (info != null) {
            final int totalVolume = info.clientBalloonVolume();
            if (totalVolume == 0) {
                AeroLang.translate("lifting_gas.no_suitable_balloon").style(ChatFormatting.RED)
                        .forGoggles(tooltip, 2);
                return;
            }

            final MutableComponent gasOutputComponent = AeroLang.translate("unit.meter_cubed", String.format("%.2f", provider.getGasOutput())).style(ChatFormatting.AQUA).component();
            AeroLang.translate("lifting_gas.gas_output", provider.getLiftingGasType().getName(), gasOutputComponent).style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            AeroLang.emptyLine(tooltip);
            AeroLang.translate("lifting_gas.balloon").forGoggles(tooltip, 1);

            final int totalBar = 30;
            final int targetBar = (int) Math.ceil(totalBar * info.clientBalloonTarget() / totalVolume);
            final double volumeInterp = BlockEntityLiftingGasProvider.getPredictedVolume(info, ticksSinceSync);
            final int volumeBar = Mth.clamp((int) Math.ceil(totalBar * volumeInterp / totalVolume), 0, totalBar);

            final MutableComponent base = BlockEntityLiftingGasProvider.barComponent(volumeBar, targetBar, totalBar);
            AeroLang.translate("lifting_gas.fill", base).style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            double lift = info.clientBalloonLift() * airPressure;
            if (info.clientBalloonFilled() > 0.01) {
                // lift is linear with volume, so we can use the fractional change in volume to also dynamically update lift
                lift *= volumeInterp / info.clientBalloonFilled();
            }

            final MutableComponent liftComponent = AeroLang.kilopixelGram(lift)
                    .style(ChatFormatting.AQUA).component();

            AeroLang.translate("lifting_gas.total_lift", liftComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);

            final MutableComponent balloonVolumeComponent = AeroLang.translate("unit.meter_cubed", totalVolume)
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("lifting_gas.balloon_volume", balloonVolumeComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);
        }
    }
}
