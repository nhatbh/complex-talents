package com.complextalents.stats.persistence;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated SavedData for General Stats persistence.
 */
public class GeneralStatsSavedData extends SavedData {

    private static final String DATA_NAME = TalentsMod.MODID + "_general_stats";

    private final Map<UUID, CompoundTag> statsData = new ConcurrentHashMap<>();

    public static GeneralStatsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GeneralStatsSavedData::load,
                GeneralStatsSavedData::new,
                DATA_NAME
        );
    }

    public static GeneralStatsSavedData load(CompoundTag tag) {
        GeneralStatsSavedData data = new GeneralStatsSavedData();
        CompoundTag statsTag = tag.getCompound("statsData");
        for (String uuidStr : statsTag.getAllKeys()) {
            data.statsData.put(UUID.fromString(uuidStr), statsTag.getCompound(uuidStr));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag statsTag = new CompoundTag();
        for (var entry : statsData.entrySet()) {
            statsTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("statsData", statsTag);
        return tag;
    }

    public void saveStatsData(UUID playerId, CompoundTag data) {
        statsData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getStatsData(UUID playerId) {
        return statsData.get(playerId);
    }

    public boolean hasStatsData(UUID playerId) {
        return statsData.containsKey(playerId);
    }

    public void removeStatsData(UUID playerId) {
        statsData.remove(playerId);
        setDirty();
    }
}
