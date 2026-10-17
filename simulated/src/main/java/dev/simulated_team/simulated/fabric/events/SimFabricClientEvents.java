package dev.simulated_team.simulated.fabric.events;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItemBindHandler;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.fabric.service.FabricSimItemService;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fabric API client events feeding {@link SimulatedCommonClientEvents} (port of NeoForge's {@code SimNeoForgeClientEvents}
 * and the client half of {@code SimNeoForgeCommonEvents}). Registered from {@link SimulatedCommonClientEvents#register()}.
 * <p>
 * Events without a Fabric API equivalent are fired by the client mixins in {@code dev.simulated_team.simulated.fabric.mixin.client}:
 * key input ({@code KeyboardHandlerMixin}), the use-item key ({@code MinecraftMixin}), using an item on a block after the
 * block declined ({@code MultiPlayerGameModeMixin}) and item tooltips ({@code ItemStackMixin}).
 */
@ApiStatus.Internal
public final class SimFabricClientEvents {

    private SimFabricClientEvents() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(SimulatedCommonClientEvents::preClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(SimulatedCommonClientEvents::postClientTick);

        // RenderGuiEvent.Post: drawn after the rest of the HUD
        HudElementRegistry.addLast(Simulated.path("overlays"), (graphics, deltaTracker) ->
                SimulatedCommonClientEvents.renderOverlays(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Simulated.path("linked_typewriter_binding"), LinkedTypewriterItemBindHandler::renderOverlay);

        SimKeys.registerTo(KeyBindingHelper::registerKeyBinding);

        // PlayerInteractEvent.RightClickBlock (client listener)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player.isLocalPlayer()) {
                final InteractionResult result = SimulatedCommonClientEvents.onRightClickBlock(player, hand, hitResult.getBlockPos(), hitResult);
                if (result != null) {
                    return result;
                }
            }

            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(SimItems.HONEY_GLUE)) {
                // neither the block nor the item is used
                if (level.isClientSide()) {
                    SimClickInteractions.HONEY_GLUE_MANAGER.selectPos(hitResult.getBlockPos(), player, stack);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        // PlayerInteractEvent.RightClickItem
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.isLocalPlayer()) {
                SimulatedCommonClientEvents.useItemOnAirEvent(level, player, player.getItemInHand(hand), hand);
            }
            return InteractionResult.PASS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> FabricSimItemService.setFuelValues(handler.fuelValues()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FabricSimItemService.setFuelValues(null));
    }
}
