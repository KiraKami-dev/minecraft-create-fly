package dev.eriksonn.aeronautics.fabric.service;

import dev.eriksonn.aeronautics.index.AeroFluids;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.service.AeroLevititeService;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * Upstream: {@code NeoForgeAeroLevititeService}.
 */
public class FabricAeroLevititeService implements AeroLevititeService {

    @Override
    public Item getBucket() {
        return AeroItems.LEVITITE_BLEND_BUCKET;
    }

    @Override
    public Fluid getFluid() {
        return AeroFluids.LEVITITE_BLEND;
    }
}
