package dev.simulated_team.simulated.ponder.instructions;

import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.zurrtum.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.zurrtum.create.client.ponder.api.level.PonderLevel;
import com.zurrtum.create.client.ponder.api.scene.Selection;
import com.zurrtum.create.client.ponder.foundation.PonderScene;
import com.zurrtum.create.client.ponder.foundation.instruction.WorldModifyInstruction;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public class RedstoneSignalInstruction extends WorldModifyInstruction {

    protected final int signal;

    public RedstoneSignalInstruction(final Selection selection, final int signal) {
        super(selection);
        this.signal = signal;
    }

    @Override
    protected void runModification(final Selection selection, final PonderScene scene) {
        final PonderLevel level = scene.getLevel();
        selection.forEach(pos -> {
            if (!level.getBounds().isInside(pos)) {
                return;
            }
            final BlockEntity BE = level.getBlockEntity(pos);
            if (BE instanceof final NixieTubeBlockEntity nixie) {
                nixie.updateRedstoneStrength(this.signal);
                nixie.updateDisplayedStrings();
            }
            if (BE instanceof final AnalogLeverBlockEntity lever) {
                final RegistryAccess registryAccess = level.registryAccess();
                try (final ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(lever.problemPath(), Simulated.LOGGER)) {
                    final TagValueOutput output = TagValueOutput.createWithContext(logging, registryAccess);
                    lever.write(output, false);
                    final CompoundTag tag = output.buildResult();
                    tag.putInt("State", this.signal);
                    lever.readClient(TagValueInput.create(logging, registryAccess, tag));
                }
            }
            final BlockState state = level.getBlockState(pos);
            BlockState newState = null;
            if (state == Blocks.AIR.defaultBlockState()) {
                return;
            }
            if (state.hasProperty(BlockStateProperties.POWER)) {
                newState = state.setValue(BlockStateProperties.POWER, this.signal);
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                newState = state.setValue(BlockStateProperties.POWERED, this.signal > 0);
            }
            if (state.hasProperty(RedstoneTorchBlock.LIT)) {
                newState = state.setValue(RedstoneTorchBlock.LIT, this.signal > 0);
            }

            if (newState == null) {
                return;
            }
            level.setBlockAndUpdate(pos, newState);
        });
    }

    @Override
    protected boolean needsRedraw() {
        return true;
    }
}
