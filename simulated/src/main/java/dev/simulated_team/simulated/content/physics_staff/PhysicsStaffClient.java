package dev.simulated_team.simulated.content.physics_staff;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModels;

/**
 * Client-side registration for the physics staff. Call {@link #registerClient()} once from the client entrypoint.
 */
public final class PhysicsStaffClient {

    private PhysicsStaffClient() {
    }

    public static void registerClient() {
        // item model type used by assets/simulated/items/creative_physics_staff.json (was a NeoForge IClientItemExtensions)
        ItemModels.ID_MAPPER.put(PhysicsStaffItemRenderer.ID, PhysicsStaffItemRenderer.Unbaked.CODEC);

        // beams, lock icons and the hover selection; upstream drew these at Veil's AFTER_TRANSLUCENT_BLOCKS stage
        WorldRenderEvents.END_MAIN.register(context ->
                PhysicsStaffRenderHandler.renderSelectionBox(context.matrices(), context.consumers(), Minecraft.getInstance().gameRenderer.getMainCamera()));
    }
}
