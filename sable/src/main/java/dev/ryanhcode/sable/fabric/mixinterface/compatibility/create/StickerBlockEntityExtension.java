package dev.ryanhcode.sable.fabric.mixinterface.compatibility.create;


import net.minecraft.core.HolderLookup;

public interface StickerBlockEntityExtension {

    void sable$removeConstraint();

    void sable$tickConstraint();

    void sable$saveToContraption(HolderLookup.Provider registries);
}
