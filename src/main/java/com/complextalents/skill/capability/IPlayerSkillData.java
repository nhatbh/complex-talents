package com.complextalents.skill.capability;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Capability interface for player skill data.
 * Attached to all ServerPlayer entities.
 */
public interface IPlayerSkillData {

    /**
     * Number of skill slots available
     */
    int SLOT_COUNT = 4;

    /**
     * Get the skill assigned to a specific slot.
     *
     * @param slotIndex 0-3
     * @return Skill ID or null if empty
     */
    @Nullable
    ResourceLocation getSkillInSlot(int slotIndex);

    /**
     * Assign a skill to a slot.
     *
     * @param slotIndex 0-3
     * @param skillId   The skill to assign, or null to clear
     */
    void setSkillInSlot(int slotIndex, @Nullable ResourceLocation skillId);

    /**
     * Clear a slot.
     *
     * @param slotIndex 0-3
     */
    default void clearSlot(int slotIndex) {
        setSkillInSlot(slotIndex, null);
    }

    /**
     * Get all assigned skill slots.
     *
     * @return Array of skill IDs (may contain nulls)
     */
    ResourceLocation[] getAssignedSlots();

    /**
     * Check if a skill is on cooldown for active casting.
     *
     * @param skillId The skill ID
     * @return true if on cooldown
     */
    boolean isOnCooldown(ResourceLocation skillId);

    /**
     * Get remaining cooldown for active casting (in seconds).
     *
     * @param skillId The skill ID
     * @return Remaining seconds, or 0 if not on cooldown
     */
    double getCooldown(ResourceLocation skillId);

    /**
     * Set cooldown for active casting.
     *
     * @param skillId The skill ID
     * @param seconds Cooldown duration in seconds
     */
    void setCooldown(ResourceLocation skillId, double seconds);

    /**
     * Clear cooldown for a skill.
     *
     * @param skillId The skill ID
     */
    void clearCooldown(ResourceLocation skillId);

    /**
     * Check if passive cooldown is active (for hybrid skills).
     *
     * @param skillId The skill ID
     * @return true if passive is on cooldown
     */
    boolean isPassiveOnCooldown(ResourceLocation skillId);

    /**
     * Get remaining passive cooldown (in seconds).
     *
     * @param skillId The skill ID
     * @return Remaining seconds, or 0 if not on cooldown
     */
    double getPassiveCooldown(ResourceLocation skillId);

    /**
     * Set passive cooldown.
     *
     * @param skillId The skill ID
     * @param seconds Cooldown duration in seconds
     */
    void setPassiveCooldown(ResourceLocation skillId, double seconds);

    /**
     * Clear passive cooldown for a skill.
     *
     * @param skillId The skill ID
     */
    void clearPassiveCooldown(ResourceLocation skillId);

    /**
     * Check if a toggle skill is currently active.
     *
     * @param skillId The skill ID
     * @return true if the toggle is active
     */
    boolean isToggleActive(ResourceLocation skillId);

    /**
     * Set toggle state for a skill.
     *
     * @param skillId The skill ID
     * @param active  The new toggle state
     */
    void setToggleActive(ResourceLocation skillId, boolean active);

    /**
     * Toggle a skill's state.
     *
     * @param skillId The skill ID
     * @return The new toggle state
     */
    default boolean toggle(ResourceLocation skillId) {
        boolean newState = !isToggleActive(skillId);
        setToggleActive(skillId, newState);
        return newState;
    }

    /**
     * Update all cooldowns and toggle resource consumption.
     * Called each server tick.
     */
    void tick();

    /**
     * Sync data to client.
     */
    void sync();

    /**
     * Clear all data (respawn/death).
     */
    void clear();
}
