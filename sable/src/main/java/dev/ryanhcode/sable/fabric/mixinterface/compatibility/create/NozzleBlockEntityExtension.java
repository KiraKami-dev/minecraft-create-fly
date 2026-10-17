package dev.ryanhcode.sable.fabric.mixinterface.compatibility.create;

import net.minecraft.core.Direction;

import java.util.EnumSet;

public interface NozzleBlockEntityExtension {
	EnumSet<Direction> sable$getValidDirections();
}
