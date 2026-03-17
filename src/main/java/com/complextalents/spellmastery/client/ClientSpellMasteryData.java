package com.complextalents.spellmastery.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-side storage for player's spell mastery data.
 * Used for UI and local logic verification.
 */
public class ClientSpellMasteryData {
    private static final Map<ResourceLocation, Integer> masteryLevels = new HashMap<>();
    private static final Map<ResourceLocation, Integer> learnedSpells = new HashMap<>(); // SpellID -> Max Level Learned

    public static void updateData(CompoundTag nbt) {
        masteryLevels.clear();
        CompoundTag masteryNbt = nbt.getCompound("MasteryLevels");
        for (String key : masteryNbt.getAllKeys()) {
            masteryLevels.put(ResourceLocation.parse(key), masteryNbt.getInt(key));
        }

        learnedSpells.clear();
        if (nbt.contains("LearnedSpellsMap")) {
            CompoundTag spellsNbt = nbt.getCompound("LearnedSpellsMap");
            for (String key : spellsNbt.getAllKeys()) {
                learnedSpells.put(ResourceLocation.parse(key), spellsNbt.getInt(key));
            }
        } else if (nbt.contains("LearnedSpells")) {
            // Migration
            ListTag legacySpells = nbt.getList("LearnedSpells", Tag.TAG_STRING);
            for (int i = 0; i < legacySpells.size(); i++) {
                learnedSpells.put(ResourceLocation.parse(legacySpells.getString(i)), 1);
            }
        }
    }

    public static int getMasteryLevel(ResourceLocation schoolId) {
        return masteryLevels.getOrDefault(schoolId, 0);
    }

    public static boolean isSpellLearned(ResourceLocation spellId, int level) {
        return learnedSpells.getOrDefault(spellId, 0) >= level;
    }

    public static Set<ResourceLocation> getLearnedSpells() {
        return new HashSet<>(learnedSpells.keySet());
    }

    public static Map<ResourceLocation, Integer> getAllMasteryLevels() {
        return new HashMap<>(masteryLevels);
    }
}
