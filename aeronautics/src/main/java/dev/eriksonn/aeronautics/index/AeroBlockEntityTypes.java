package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerBlockEntity;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity types. Renderers and visuals are registered on the client in {@code AeroBlockEntityRenders}.
 */
public class AeroBlockEntityTypes {
    private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();

    public static final BlockEntityType<SteamVentBlockEntity> STEAM_VENT = REGISTRATE.blockEntity(
            "steam_vent", SteamVentBlockEntity::new, AeroBlocks.STEAM_VENT);

    public static final BlockEntityType<HotAirBurnerBlockEntity> HOT_AIR_BURNER = REGISTRATE.blockEntity(
            "adjustable_burner", HotAirBurnerBlockEntity::new, AeroBlocks.HOT_AIR_BURNER);

    public static final BlockEntityType<PropellerBearingBlockEntity> PROPELLER_BEARING = REGISTRATE.blockEntity(
            "propeller_bearing", PropellerBearingBlockEntity::new, AeroBlocks.PROPELLER_BEARING);

    public static final BlockEntityType<GyroscopicPropellerBearingBlockEntity> GYROSCOPIC_PROPELLER_BEARING = REGISTRATE.blockEntity(
            "gyroscopic_propeller_bearing", GyroscopicPropellerBearingBlockEntity::new, AeroBlocks.GYROSCOPIC_PROPELLER_BEARING);

    public static final BlockEntityType<KineticBlockEntity> ENVELOPE_ENCASED_SHAFT = REGISTRATE.blockEntity(
            "envelope_encased_shaft", KineticBlockEntity::new, AeroBlocks.ENVELOPE_ENCASED_SHAFTS.toArray());

    public static final BlockEntityType<AndesitePropellerBlockEntity> ANDESITE_PROPELLER = REGISTRATE.blockEntity(
            "andesite_propeller", AndesitePropellerBlockEntity::new, AeroBlocks.ANDESITE_PROPELLER);

    public static final BlockEntityType<WoodenPropellerBlockEntity> WOODEN_PROPELLER = REGISTRATE.blockEntity(
            "wooden_propeller", WoodenPropellerBlockEntity::new, AeroBlocks.WOODEN_PROPELLER);

    public static final BlockEntityType<SmartPropellerBlockEntity> SMART_PROPELLER = REGISTRATE.blockEntity(
            "smart_propeller", SmartPropellerBlockEntity::new, AeroBlocks.SMART_PROPELLER);

    public static final BlockEntityType<MountedPotatoCannonBlockEntity> MOUNTED_POTATO_CANNON = REGISTRATE.blockEntity(
            "mounted_potato_cannon", MountedPotatoCannonBlockEntity::new, AeroBlocks.MOUNTED_POTATO_CANNON);

    public static void register() {
        // block entity storages (upstream: Registrate onRegister callbacks)
        SimInventoryService.INSTANCE.registerInventory(MOUNTED_POTATO_CANNON, (be, dir) -> be.getInventory());
    }
}
