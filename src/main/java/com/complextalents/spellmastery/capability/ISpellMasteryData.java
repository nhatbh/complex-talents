package com.complextalents.spellmastery.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;
import java.util.Set;

/**
 * Interface for Spell Mastery data capability.
 * Tracks mastery levels for magic schools and learned spells.
 */
public interface ISpellMasteryData extends INBTSerializable<CompoundTag> {
    
    /**
     * Gets the mastery level for a specific magic school.
     * @param schoolId The ResourceLocation of the magic school.
     * @return The mastery level (0-5 mapping to Common-Legendary).
     */
    int getMasteryLevel(ResourceLocation schoolId);
    
    /**
     * Sets the mastery level for a specific magic school.
     * @param schoolId The ResourceLocation of the magic school.
     * @param level The mastery level.
     */
    void setMasteryLevel(ResourceLocation schoolId, int level);
    
    /**
     * Checks if a spell has been learned at a specific level (or higher).
     * @param spellId The ResourceLocation of the spell.
     * @param level The level of the spell.
     * @return True if learned, false otherwise.
     */
    boolean isSpellLearned(ResourceLocation spellId, int level);
    
    /**
     * Learns a specific spell tier.
     * @param spellId The ResourceLocation of the spell.
     * @param level The level of the spell tier.
     */
    void learnSpell(ResourceLocation spellId, int level);
    
    /**
     * Forgets a specific spell.
     * @param spellId The ResourceLocation of the spell.
     */
    void forgetSpell(ResourceLocation spellId);
    
    /**
     * Gets all learned spells.
     * @return A set of spell IDs.
     */
    Set<ResourceLocation> getLearnedSpells();
    
    /**
     * Gets all mastery levels.
     * @return A map of school IDs to mastery levels.
     */
    Map<ResourceLocation, Integer> getAllMasteryLevels();
    
    /**
     * Synchronizes data to the client.
     */
    void sync();
}
