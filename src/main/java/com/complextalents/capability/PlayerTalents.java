package com.complextalents.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

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
}
