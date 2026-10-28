package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity types. Renderers and visuals are registered on the client in {@code OffroadBlockEntityRenders}.
 */
public class OffroadBlockEntityTypes {
    private static final SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();

    public static final BlockEntityType<WheelMountBlockEntity> WHEEL_MOUNT = REGISTRATE.blockEntity(
            "wheel_mount", WheelMountBlockEntity::new, OffroadBlocks.WHEEL_MOUNT);

    public static final BlockEntityType<BoreheadBearingBlockEntity> BOREHEAD_BEARING = REGISTRATE.blockEntity(
            "borehead_bearing", BoreheadBearingBlockEntity::new, OffroadBlocks.BOREHEAD_BEARING_BLOCK);

    public static final BlockEntityType<RockCuttingWheelBlockEntity> ROCKCUTTING_WHEEL_BLOCK_ENTITY = REGISTRATE.blockEntity(
            "rockcutting_wheel", RockCuttingWheelBlockEntity::new, OffroadBlocks.ROCK_CUTTER_BLOCK);

    public static void register() {
        SimInventoryService.INSTANCE.<WheelMountBlockEntity>registerInventory((be, dir) -> be.getInventory()).accept(WHEEL_MOUNT);
        SimInventoryService.INSTANCE.<BoreheadBearingBlockEntity>registerInventory((be, dir) -> be.getContraptionWrappedInventory()).accept(BOREHEAD_BEARING);
    }
}
