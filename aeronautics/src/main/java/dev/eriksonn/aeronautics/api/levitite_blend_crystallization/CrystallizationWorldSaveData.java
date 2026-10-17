package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CrystallizationWorldSaveData extends SavedData {
	public static final String ID = "aeronautics_levitite_data";

	Level level;

	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		LevititeCrystallizerManager.saveData(list, level);
		tag.put("Levitite Manager Data", list);

		return tag;
	}

	public static CrystallizationWorldSaveData load(ServerLevel level, CompoundTag tag) {
		CrystallizationWorldSaveData data = new CrystallizationWorldSaveData();
		data.level = level;

		LevititeCrystallizerManager.loadData(tag, level);

		return data;
	}

	public static CrystallizationWorldSaveData get(ServerLevel level) {
		// 1.21.5+: saved data is (de)serialized through a codec; null DataFixTypes as upstream
		CrystallizationWorldSaveData data = level.getDataStorage().computeIfAbsent(new SavedDataType<>(
				CrystallizationWorldSaveData.ID,
				CrystallizationWorldSaveData::new,
				CompoundTag.CODEC.xmap(tag -> load(level, tag), saveData -> saveData.save(new CompoundTag())),
				null
		));

		data.level = level;
		return data;
	}
}
