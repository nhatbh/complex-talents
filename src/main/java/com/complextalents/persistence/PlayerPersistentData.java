package com.complextalents.persistence;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified SavedData for persisting player capability data across deaths.
 * Stores origin, skill, and passive stack data keyed by player UUID.
 */
public class PlayerPersistentData extends SavedData {

    private static final String DATA_NAME = TalentsMod.MODID + "_player_data";

    // Storage maps keyed by player UUID
    private final Map<UUID, CompoundTag> originData = new ConcurrentHashMap<>();
    private final Map<UUID, CompoundTag> skillData = new ConcurrentHashMap<>();
    private final Map<UUID, CompoundTag> passiveData = new ConcurrentHashMap<>();

    /**
     * Get or create the PlayerPersistentData for a server level.
     */
    public static PlayerPersistentData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlayerPersistentData::load,
                () -> new PlayerPersistentData(),
                DATA_NAME
        );
    }

    /**
     * Load from NBT - called by SavedData system.
     */
    public static PlayerPersistentData load(CompoundTag tag) {
        PlayerPersistentData data = new PlayerPersistentData();

        CompoundTag originTag = tag.getCompound("originData");
        for (String uuidStr : originTag.getAllKeys()) {
            data.originData.put(UUID.fromString(uuidStr), originTag.getCompound(uuidStr));
        }

        CompoundTag skillTag = tag.getCompound("skillData");
        for (String uuidStr : skillTag.getAllKeys()) {
            data.skillData.put(UUID.fromString(uuidStr), skillTag.getCompound(uuidStr));
        }

        CompoundTag passiveTag = tag.getCompound("passiveData");
        for (String uuidStr : passiveTag.getAllKeys()) {
            data.passiveData.put(UUID.fromString(uuidStr), passiveTag.getCompound(uuidStr));
        }

        return data;
    }

    /**
     * Save to NBT - called by SavedData system.
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag originTag = new CompoundTag();
        for (var entry : originData.entrySet()) {
            originTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("originData", originTag);

        CompoundTag skillTag = new CompoundTag();
        for (var entry : skillData.entrySet()) {
            skillTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("skillData", skillTag);

        CompoundTag passiveTag = new CompoundTag();
        for (var entry : passiveData.entrySet()) {
            passiveTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("passiveData", passiveTag);

        return tag;
    }

    // --- Origin Data Methods ---

    public void saveOriginData(UUID playerId, CompoundTag data) {
        originData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getOriginData(UUID playerId) {
        return originData.get(playerId);
    }

    public boolean hasOriginData(UUID playerId) {
        return originData.containsKey(playerId);
    }

    public void removeOriginData(UUID playerId) {
        originData.remove(playerId);
        setDirty();
    }

    // --- Skill Data Methods ---

    public void saveSkillData(UUID playerId, CompoundTag data) {
        skillData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getSkillData(UUID playerId) {
        return skillData.get(playerId);
    }

    public boolean hasSkillData(UUID playerId) {
        return skillData.containsKey(playerId);
    }

    public void removeSkillData(UUID playerId) {
        skillData.remove(playerId);
        setDirty();
    }

    // --- Passive Data Methods ---

    public void savePassiveData(UUID playerId, CompoundTag data) {
        passiveData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getPassiveData(UUID playerId) {
        return passiveData.get(playerId);
    }

    public boolean hasPassiveData(UUID playerId) {
        return passiveData.containsKey(playerId);
    }

    public void removePassiveData(UUID playerId) {
        passiveData.remove(playerId);
        setDirty();
    }

    // --- Cleanup Methods ---

    public void removeAllPlayerData(UUID playerId) {
        originData.remove(playerId);
        skillData.remove(playerId);
        passiveData.remove(playerId);
        setDirty();
    }
}
