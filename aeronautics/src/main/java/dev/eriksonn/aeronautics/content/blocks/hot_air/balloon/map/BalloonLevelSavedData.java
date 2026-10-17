package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BalloonLevelSavedData extends SavedData {
    public static final String ID = "aeronautics_unloaded_balloons";
    public static Codec<List<SavedBalloon>> CODEC = Codec.list(SavedBalloon.CODEC);

    /**
     * {@link net.minecraft.world.level.storage.DimensionDataStorage} caches by type (record equality), so the type is a
     * single instance; each level has its own storage. Same NBT layout as upstream's {@code SavedData.Factory}
     * ({@code {ID: [balloons]}}); null data fix type as upstream.
     */
    private static final SavedDataType<BalloonLevelSavedData> TYPE = new SavedDataType<>(
            ID,
            BalloonLevelSavedData::new,
            CompoundTag.CODEC.xmap(BalloonLevelSavedData::load, data -> data.save(new CompoundTag())),
            null
    );

    private Level level;

    /**
     * Balloons read from disk, handed to the level's {@link BalloonMap} by {@link #get(ServerLevel)} (upstream did it
     * while loading, which had the level at hand)
     */
    private @Nullable List<SavedBalloon> loadedBalloons;

    private static BalloonLevelSavedData load(final CompoundTag tag) {
        final BalloonLevelSavedData sd = new BalloonLevelSavedData();

        if (tag.contains(ID)) {
            final DataResult<Pair<List<SavedBalloon>, Tag>> result = CODEC.decode(NbtOps.INSTANCE, tag.getListOrEmpty(ID));

            result.ifSuccess(x -> sd.loadedBalloons = x.getFirst());
        }
        return sd;
    }

    public static BalloonLevelSavedData get(final ServerLevel level) {
        final BalloonLevelSavedData data = level.getChunkSource().getDataStorage().computeIfAbsent(TYPE);
        data.level = level;

        if (data.loadedBalloons != null) {
            final BalloonMap map = BalloonMap.MAP.get(level);
            map.getUnloadedBalloons().addAll(data.loadedBalloons);
            data.loadedBalloons = null;
        }

        return data;
    }

    public @NotNull CompoundTag save(final CompoundTag tag) {
        final BalloonMap map = BalloonMap.MAP.get(this.level);
        final ObjectArrayList<SavedBalloon> list = new ObjectArrayList<>(map.getUnloadedBalloons());

        for (final Balloon balloon : map.getBalloons()) {
            list.add(BalloonMap.saveBalloon((ServerBalloon) balloon));
        }

        final DataResult<Tag> result = CODEC.encodeStart(NbtOps.INSTANCE, list);
        result.ifSuccess(data -> tag.put(ID, data));

        return tag;
    }
}
