package com.complextalents.leveling.data;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SavedData for persisting player leveling statistics across server restarts.
 * Each player's level, XP, and skill points are stored here.
 *
 * <p>This is the primary data store for leveling information, separate from
 * {@link com.complextalents.persistence.PlayerPersistentData} which handles
 * origin, skill, and other persistent data.</p>
 *
 * <p>Thread-safe using ConcurrentHashMap.</p>
 */
public class PlayerLevelingData extends SavedData {
    private static final String DATA_NAME = TalentsMod.MODID + "_player_leveling";

    // Storage: Map<PlayerUUID, LevelStats>
    private final Map<UUID, LevelStats> playerStats = new ConcurrentHashMap<>();

    /**
     * Gets or creates the PlayerLevelingData for a server level.
     * Uses the level's DataStorage system for persistence.
     *
     * @param level The ServerLevel to get data for
     * @return The PlayerLevelingData for this level
     */
    public static PlayerLevelingData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlayerLevelingData::load,
                PlayerLevelingData::new,
                DATA_NAME
        );
    }

    /**
     * Loads PlayerLevelingData from NBT.
     * Called by the SavedData system.
     *
     * @param tag The CompoundTag to load from
     * @return A new PlayerLevelingData with loaded stats
     */
    public static PlayerLevelingData load(CompoundTag tag) {
        PlayerLevelingData data = new PlayerLevelingData();

        CompoundTag statsTag = tag.getCompound("playerStats");
        for (String uuidStr : statsTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag statTag = statsTag.getCompound(uuidStr);
                LevelStats stats = LevelStats.deserializeNBT(statTag);
                data.playerStats.put(uuid, stats);
            } catch (IllegalArgumentException e) {
                TalentsMod.LOGGER.warn("Failed to load stats for player {}: {}", uuidStr, e.getMessage());
            }
        }

        return data;
    }

    /**
     * Saves PlayerLevelingData to NBT.
     * Called by the SavedData system.
     *
     * @param tag The CompoundTag to save to
     * @return The modified tag
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag statsTag = new CompoundTag();

        for (var entry : playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            LevelStats stats = entry.getValue();
            statsTag.put(uuid.toString(), stats.serializeNBT());
        }

        tag.put("playerStats", statsTag);
        return tag;
    }

    /**
     * Gets a snapshot of player's leveling statistics.
     * Creates a new copy if player doesn't exist.
     *
     * @param playerId The UUID of the player
     * @return An immutable LevelStats snapshot
     */
    public LevelStats getStats(UUID playerId) {
        return playerStats.getOrDefault(playerId, new LevelStats());
    }

    /**
     * Gets the player's current level.
     *
     * @param playerId The UUID of the player
     * @return The player's level (minimum 1)
     */
    public int getLevel(UUID playerId) {
        return getStats(playerId).getLevel();
    }

    /**
     * Gets the player's current XP towards next level.
     *
     * @param playerId The UUID of the player
     * @return The current XP progress
     */
    public double getCurrentXP(UUID playerId) {
        return getStats(playerId).getCurrentXP();
    }

    /**
     * Gets the player's total accumulated XP.
     *
     * @param playerId The UUID of the player
     * @return The total XP
     */
    public double getTotalXP(UUID playerId) {
        return getStats(playerId).getTotalXP();
    }

    /**
     * Gets the player's available skill points.
     *
     * @param playerId The UUID of the player
     * @return The number of skill points
     */
    public int getSkillPoints(UUID playerId) {
        return getStats(playerId).getSkillPoints();
    }

    /**
     * Adds XP to a player and handles level-ups.
     *
     * @param playerId The UUID of the player
     * @param amount The XP amount to add (must be non-negative)
     */
    public void addXP(UUID playerId, double amount) {
        if (amount <= 0) return;

        LevelStats oldStats = getStats(playerId);
        double currentXP = oldStats.getCurrentXP() + amount;
        double totalXP = oldStats.getTotalXP() + amount;
        int level = oldStats.getLevel();
        int skillPoints = oldStats.getSkillPoints();

        // XP required for next level: 100 + (level^1.5 * 50)
        final int SKILL_POINTS_PER_LEVEL = 2;

        // Level-up loop
        double xpForNext = 100 + (Math.pow(level, 1.5) * 50);
        while (currentXP >= xpForNext) {
            currentXP -= xpForNext;
            level++;
            skillPoints += SKILL_POINTS_PER_LEVEL;
            xpForNext = 100 + (Math.pow(level, 1.5) * 50);
        }

        LevelStats newStats = new LevelStats(level, currentXP, totalXP, skillPoints);
        playerStats.put(playerId, newStats);
        setDirty();
    }

    /**
     * Resets player's current XP to 0.
     * Used for death penalties - total XP and level remain unchanged.
     *
     * @param playerId The UUID of the player
     */
    public void resetCurrentXP(UUID playerId) {
        LevelStats oldStats = getStats(playerId);
        LevelStats newStats = new LevelStats(
                oldStats.getLevel(),
                0.0, // Reset current XP
                oldStats.getTotalXP(), // Keep total
                oldStats.getSkillPoints() // Keep skill points
        );
        playerStats.put(playerId, newStats);
        setDirty();
    }

    /**
     * Removes all leveling data for a player.
     * Called when player data is completely wiped.
     *
     * @param playerId The UUID of the player
     */
    public void removePlayerData(UUID playerId) {
        playerStats.remove(playerId);
        setDirty();
    }

    /**
     * Checks if player has any leveling data.
     *
     * @param playerId The UUID of the player
     * @return true if player has leveling data, false otherwise
     */
    public boolean hasPlayerData(UUID playerId) {
        return playerStats.containsKey(playerId);
    }
}
