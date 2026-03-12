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
    private final Map<UUID, CompoundTag> darkMageData = new ConcurrentHashMap<>();
    private final Map<UUID, CompoundTag> elementalMageData = new ConcurrentHashMap<>();
    private final Map<UUID, CompoundTag> faithData = new ConcurrentHashMap<>();

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

        CompoundTag darkMageTag = tag.getCompound("darkMageData");
        for (String uuidStr : darkMageTag.getAllKeys()) {
            data.darkMageData.put(UUID.fromString(uuidStr), darkMageTag.getCompound(uuidStr));
        }

        CompoundTag elementalMageTag = tag.getCompound("elementalMageData");
        for (String uuidStr : elementalMageTag.getAllKeys()) {
            data.elementalMageData.put(UUID.fromString(uuidStr), elementalMageTag.getCompound(uuidStr));
        }

        CompoundTag faithTag = tag.getCompound("faithData");
        for (String uuidStr : faithTag.getAllKeys()) {
            data.faithData.put(UUID.fromString(uuidStr), faithTag.getCompound(uuidStr));
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

        CompoundTag darkMageTag = new CompoundTag();
        for (var entry : darkMageData.entrySet()) {
            darkMageTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("darkMageData", darkMageTag);

        CompoundTag elementalMageTag = new CompoundTag();
        for (var entry : elementalMageData.entrySet()) {
            elementalMageTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("elementalMageData", elementalMageTag);

        CompoundTag faithTag = new CompoundTag();
        for (var entry : faithData.entrySet()) {
            faithTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("faithData", faithTag);

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

    // --- Dark Mage Data Methods ---

    public void saveDarkMageData(UUID playerId, CompoundTag data) {
        darkMageData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getDarkMageData(UUID playerId) {
        return darkMageData.get(playerId);
    }

    public boolean hasDarkMageData(UUID playerId) {
        return darkMageData.containsKey(playerId);
    }

    public void removeDarkMageData(UUID playerId) {
        darkMageData.remove(playerId);
        setDirty();
    }

    // --- Elemental Mage Data Methods ---

    public void saveElementalMageData(UUID playerId, CompoundTag data) {
        elementalMageData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getElementalMageData(UUID playerId) {
        return elementalMageData.get(playerId);
    }

    public boolean hasElementalMageData(UUID playerId) {
        return elementalMageData.containsKey(playerId);
    }

    public void removeElementalMageData(UUID playerId) {
        elementalMageData.remove(playerId);
        setDirty();
    }

    // --- High Priest Faith Data Methods ---

    public void saveFaithData(UUID playerId, CompoundTag data) {
        faithData.put(playerId, data.copy());
        setDirty();
    }

    public CompoundTag getFaithData(UUID playerId) {
        return faithData.get(playerId);
    }

    public boolean hasFaithData(UUID playerId) {
        return faithData.containsKey(playerId);
    }

    public void removeFaithData(UUID playerId) {
        faithData.remove(playerId);
        setDirty();
    }

    // --- Cleanup Methods ---

    public void removeAllPlayerData(UUID playerId) {
        originData.remove(playerId);
        skillData.remove(playerId);
        passiveData.remove(playerId);
        darkMageData.remove(playerId);
        elementalMageData.remove(playerId);
        faithData.remove(playerId);
        setDirty();
    }
}
