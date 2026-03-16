package com.complextalents.leveling.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

/**
 * Interface for player leveling data.
 * Tracks level, XP, skill points, and assist data.
 */
public interface IPlayerLevelData extends INBTSerializable<CompoundTag> {

    int getLevel();
    void setLevel(int level);

    double getCurrentLevelXP();
    void setCurrentLevelXP(double xp);

    double getTotalXP();
    void setTotalXP(double xp);

    int getSkillPoints();
    void setSkillPoints(int points);

    /**
     * Adds XP to the player. Handles level-up logic.
     * @param amount The amount of XP to add.
     * @param silent If true, no level-up notifications will be sent (useful for loading/syncing).
     */
    void addXP(double amount, boolean silent);

    /**
     * Records a player's interaction with a mob or ally for assist XP.
     * @param entityId The UUID of the mob or ally.
     * @param timestamp The current server time.
     */
    void recordAssist(UUID entityId, long timestamp);

    /**
     * Checks if a player has a valid assist for an entity.
     * @param entityId The UUID of the entity.
     * @param currentTimestamp The current server time.
     * @param windowMillis The assist window in milliseconds.
     * @return True if a valid assist exists.
     */
    boolean hasAssist(UUID entityId, long currentTimestamp, long windowMillis);

    /**
     * Clears assist data for a specific entity.
     */
    void clearAssist(UUID entityId);

    /**
     * Syncs data to the client.
     */
    void sync();

    /**
     * Copies data from another instance (e.g., on player clone).
     */
    void copyFrom(IPlayerLevelData other);

    /**
     * Resets current level XP to 0 (death penalty).
     */
    void resetCurrentLevelXP();
}
