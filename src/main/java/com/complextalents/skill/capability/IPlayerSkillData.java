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
    int SLOT_COUNT = 1;

    /**
     * Get the skill assigned to a specific slot.
     *
     * @param slotIndex 0 (single slot)
     * @return Skill ID or null if empty
     */
    @Nullable
    ResourceLocation getSkillInSlot(int slotIndex);

    /**
     * Assign a skill to a slot.
     *
     * @param slotIndex 0 (single slot)
     * @param skillId   The skill to assign, or null to clear
     */
    void setSkillInSlot(int slotIndex, @Nullable ResourceLocation skillId);

    /**
     * Clear a slot.
     *
     * @param slotIndex 0 (single slot)
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
     * Get the cooldown expiration game time for a skill.
     *
     * @param skillId The skill ID
     * @return The expiration game time, or null if not on cooldown
     */
    @Nullable
    Long getCooldownExpiration(ResourceLocation skillId);

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
     * Get the game time when a toggle skill was activated.
     *
     * @param skillId The skill ID
     * @return The game time when activated, or 0 if not active or not found
     */
    long getToggleActivationTime(ResourceLocation skillId);

    /**
     * Set the game time when a toggle skill was activated.
     * This is called automatically by setToggleActive when setting to true.
     *
     * @param skillId The skill ID
     * @param gameTime The game time when activated
     */
    void setToggleActivationTime(ResourceLocation skillId, long gameTime);

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

    /**
     * Get the skill level for a specific skill.
     * Default is 1 if the skill is assigned but no level is set.
     *
     * @param skillId The skill ID
     * @return The skill level (1 or higher)
     */
    int getSkillLevel(ResourceLocation skillId);

    /**
     * Set the skill level for a specific skill.
     * The skill must be assigned to a slot.
     *
     * @param skillId The skill ID
     * @param level The skill level (must be >= 1)
     * @throws IllegalArgumentException if level < 1
     */
    void setSkillLevel(ResourceLocation skillId, int level);

    /**
     * Reset a skill's level to the default (1).
     *
     * @param skillId The skill ID
     */
    default void resetSkillLevel(ResourceLocation skillId) {
        setSkillLevel(skillId, 1);
    }

    /**
     * Get the currently active form skill ID.
     *
     * @return The form skill ID, or null if no form is active
     */
    @Nullable
    ResourceLocation getActiveForm();

    /**
     * Set the currently active form skill ID.
     *
     * @param formSkillId The form skill ID, or null to clear
     */
    void setActiveForm(@Nullable ResourceLocation formSkillId);

    /**
     * Get the expiration game time for the active form.
     *
     * @return The game time when the form expires, or 0 if no form
     */
    long getFormExpiration();

    /**
     * Set the expiration game time for the active form.
     *
     * @param expirationTime The game time when the form expires
     */
    void setFormExpiration(long expirationTime);

    /**
     * Copy all skill data from another instance.
     * Used during player clone/respawn to persist data.
     *
     * @param other The source data to copy from
     */
    void copyFrom(IPlayerSkillData other);
}
