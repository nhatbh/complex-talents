package com.complextalents.capability;

import com.complextalents.talent.ResourceBarConfig;
import com.complextalents.talent.ResourceBarRenderer;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;
import java.util.Map;

public interface PlayerTalents {
    /**
     * Check if player has a specific talent
     */
    boolean hasTalent(ResourceLocation talentId);

    /**
     * Get the level of a talent for a player
     * @return The level of the talent, or 0 if the player doesn't have it
     */
    int getTalentLevel(ResourceLocation talentId);

    /**
     * Unlock a talent at a specific level
     */
    void unlockTalent(ResourceLocation talentId, int level);

    /**
     * Upgrade a talent by one level
     */
    void upgradeTalent(ResourceLocation talentId);

    /**
     * Set a talent to a specific level
     */
    void setTalentLevel(ResourceLocation talentId, int level);

    /**
     * Remove a talent from the player
     */
    void removeTalent(ResourceLocation talentId);

    /**
     * Get all unlocked talent IDs
     */
    List<ResourceLocation> getUnlockedTalents();

    /**
     * Set an active/hybrid talent as active
     */
    void setActiveTalent(ResourceLocation talentId, boolean active);

    /**
     * Check if a talent is currently active
     */
    boolean isTalentActive(ResourceLocation talentId);

    /**
     * Get the remaining cooldown ticks for a talent
     */
    int getTalentCooldown(ResourceLocation talentId);

    /**
     * Set the cooldown for a talent
     */
    void setTalentCooldown(ResourceLocation talentId, int cooldownTicks);

    /**
     * Sync talent data to client
     */
    void sync();

    /**
     * Get the talent equipped in a specific slot
     * @return The talent ID in the slot, or null if empty
     */
    ResourceLocation getTalentInSlot(TalentSlotType slotType);

    /**
     * Equip a talent in a specific slot
     * The talent must already be unlocked via unlockTalent
     * @return true if successful, false if the talent doesn't match the slot type or isn't unlocked
     */
    boolean equipTalentToSlot(ResourceLocation talentId, TalentSlotType slotType);

    /**
     * Unequip a talent from a specific slot
     */
    void unequipTalentFromSlot(TalentSlotType slotType);

    /**
     * Get all equipped talents (slot -> talent ID)
     */
    Map<TalentSlotType, ResourceLocation> getEquippedTalents();

    /**
     * Check if a specific slot is filled
     */
    boolean isSlotFilled(TalentSlotType slotType);

    /**
     * Check if a talent can be equipped to a slot
     * Validates: talent is unlocked, slot type matches, and required definition is equipped
     * @return true if the talent can be equipped to the slot
     */
    boolean canEquipTalent(ResourceLocation talentId, TalentSlotType slotType);

    /**
     * Get all talents that depend on a specific Definition talent
     * @param definitionId The Definition talent ID to check
     * @return List of equipped talent IDs that require this definition
     */
    List<ResourceLocation> getDependentTalents(ResourceLocation definitionId);

    // ===== Resource Bar Management =====

    /**
     * Get the current resource bar configuration (from equipped Definition talent)
     * @return The resource bar config, or null if no Definition with a resource bar is equipped
     */
    ResourceBarConfig getResourceBarConfig();

    /**
     * Get the current resource value
     * @return The current resource value, or 0 if no resource bar is active
     */
    float getResource();

    /**
     * Set the resource value (clamped between 0 and max)
     * @param value The new resource value
     */
    void setResource(float value);

    /**
     * Add to the current resource value
     * @param amount The amount to add (can be negative to subtract)
     * @return The actual amount added (may be less due to clamping)
     */
    float addResource(float amount);

    /**
     * Get the maximum resource value
     * @return The maximum resource value from the ResourceBarConfig, or 0 if no config
     */
    float getMaxResource();

    /**
     * Check if the player has enough resource
     * @param amount The amount to check
     * @return true if current resource >= amount
     */
    boolean hasResource(float amount);

    /**
     * Consume resource (only if enough is available)
     * @param amount The amount to consume
     * @return true if the resource was consumed, false if not enough resource
     */
    boolean consumeResource(float amount);

    /**
     * Get the custom renderer for the current resource bar
     * @return The custom renderer, or null if using default renderer
     */
    @OnlyIn(Dist.CLIENT)
    ResourceBarRenderer getResourceBarRenderer();

    // ===== Combat Mode Management =====

    /**
     * Check if Combat Mode is currently enabled
     * @return true if Combat Mode is active
     */
    boolean isCombatModeEnabled();

    /**
     * Toggle Combat Mode on or off
     * @param enabled true to enable Combat Mode, false to disable
     */
    void setCombatMode(boolean enabled);

    /**
     * Toggle Combat Mode (switch between enabled/disabled)
     * @return The new Combat Mode state
     */
    boolean toggleCombatMode();
}
