package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.content.contraptions.IDisplayAssemblyExceptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import com.zurrtum.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;

/**
 * Shows the last assembly exception on the goggle overlay (upstream: the block entity implemented
 * {@code IDisplayAssemblyExceptions}, which is client-only in Create Fly)
 */
public class PhysicsAssemblerTooltipBehaviour extends TooltipBehaviour<PhysicsAssemblerBlockEntity> implements IDisplayAssemblyExceptions {

    public PhysicsAssemblerTooltipBehaviour(final PhysicsAssemblerBlockEntity be) {
        super(be);
    }

    /**
     * Attaches the client behaviours to the physics assembler block entity type
     */
    public static void registerClient() {
        BlockEntityBehaviour.addClient(SimBlockEntityTypes.PHYSICS_ASSEMBLER, PhysicsAssemblerTooltipBehaviour::new);
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return this.blockEntity.getLastAssemblyException();
    }
}
