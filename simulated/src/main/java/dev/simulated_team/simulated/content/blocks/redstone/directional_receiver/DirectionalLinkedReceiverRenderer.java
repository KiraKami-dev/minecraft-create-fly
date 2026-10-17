package dev.simulated_team.simulated.content.blocks.redstone.directional_receiver;

import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Only renders the frequency items (from {@link dev.simulated_team.simulated.content.blocks.redstone.LinkedReceiverLinkBehaviour}).
 */
public class DirectionalLinkedReceiverRenderer extends SmartBlockEntityRenderer<DirectionalLinkedReceiverBlockEntity, SmartBlockEntityRenderer.SmartRenderState> {
    public DirectionalLinkedReceiverRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
