package com.complextalents.leveling.service;

import com.complextalents.leveling.data.AssistTracker;
import com.complextalents.leveling.data.LevelStats;
import com.complextalents.leveling.data.PlayerLevelingData;
import com.complextalents.leveling.events.level.PlayerLevelUpEvent;
import com.complextalents.leveling.events.level.PlayerXPResetEvent;
import com.complextalents.leveling.events.xp.XPAwardedEvent;
import com.complextalents.leveling.events.xp.XPContext;
import com.complextalents.leveling.events.xp.XPPreAwardEvent;
import com.complextalents.leveling.events.xp.XPSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.Objects;

/**
 * Service layer for all leveling operations.
 * Provides a clean abstraction over the leveling data storage and event firing.
 *
 * <p>All XP awards, level-ups, and resets must go through this service.
 * This ensures events are fired correctly and data is kept in sync.</p>
 *
 * <p>Singleton pattern with lazy initialization and double-checked locking.</p>
 *
 * <p>Thread-safe.</p>
 */
public class LevelingService {
    private static volatile LevelingService INSTANCE;

    private static final double EPSILON = 0.01; // Minimum XP to process

    private LevelingService() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of LevelingService.
     * Uses double-checked locking for thread-safety and performance.
     *
     * @return The LevelingService singleton
     */
    public static LevelingService getInstance() {
        if (INSTANCE == null) {
            synchronized (LevelingService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LevelingService();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Awards XP to a player with full event chain.
     * Fires XPPreAwardEvent (cancelable) and XPAwardedEvent (immutable).
     *
     * <p>Event Sequence:
     * <ol>
     *   <li>Fire XPPreAwardEvent (cancelable, allows amount modification)</li>
     *   <li>If canceled, return false and exit</li>
     *   <li>Add XP to player data</li>
     *   <li>Fire XPAwardedEvent (immutable, for notifications)</li>
     *   <li>Check for level-ups (LevelUpHandler will handle this)</li>
     * </ol>
     *
     * @param player The player to award XP to (must be on server)
     * @param amount The initial XP amount (before modifications)
     * @param source The source of the XP award
     * @param context The context object with metadata
     * @return true if XP was awarded, false if canceled
     */
    public boolean awardXP(ServerPlayer player, double amount, XPSource source, XPContext context) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(source, "XPSource cannot be null");
        Objects.requireNonNull(context, "XPContext cannot be null");

        // Ignore tiny amounts
        if (amount < EPSILON) {
            return false;
        }

        // Fire pre-event (allows cancellation and modification)
        XPPreAwardEvent preEvent = new XPPreAwardEvent(player, source, amount, context);
        MinecraftForge.EVENT_BUS.post(preEvent);

        if (preEvent.isCanceled()) {
            return false;
        }

        double finalAmount = preEvent.getAmount();

        // Ignore if final amount is too small
        if (finalAmount < EPSILON) {
            return false;
        }

        // Add XP to storage
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        data.addXP(player.getUUID(), finalAmount);

        // Fire post-event (immutable, for notifications)
        XPAwardedEvent awardedEvent = new XPAwardedEvent(player, source, amount, finalAmount, context);
        MinecraftForge.EVENT_BUS.post(awardedEvent);

        return true;
    }

    /**
     * Gets a player's current level.
     *
     * @param player The player to query
     * @return The player's level (minimum 1)
     */
    public int getLevel(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        return data.getLevel(player.getUUID());
    }

    /**
     * Gets a player's current XP progress towards next level.
     *
     * @param player The player to query
     * @return The current XP progress
     */
    public double getCurrentXP(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        return data.getCurrentXP(player.getUUID());
    }

    /**
     * Gets a player's total accumulated XP.
     *
     * @param player The player to query
     * @return The total XP
     */
    public double getTotalXP(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        return data.getTotalXP(player.getUUID());
    }

    /**
     * Gets a player's available skill points.
     *
     * @param player The player to query
     * @return The number of skill points
     */
    public int getSkillPoints(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        return data.getSkillPoints(player.getUUID());
    }

    /**
     * Gets a complete snapshot of a player's leveling statistics.
     *
     * @param player The player to query
     * @return An immutable LevelStats snapshot
     */
    public LevelStats getStats(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        return data.getStats(player.getUUID());
    }

    /**
     * Resets a player's current XP to 0.
     * Used for death penalties - total XP and level remain unchanged.
     * Fires PlayerXPResetEvent.
     *
     * @param player The player whose XP to reset
     * @param reason The reason for the reset
     */
    public void resetCurrentXP(ServerPlayer player, PlayerXPResetEvent.ResetReason reason) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(reason, "ResetReason cannot be null");

        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        double lostXP = data.getCurrentXP(player.getUUID());

        data.resetCurrentXP(player.getUUID());

        // Fire event
        PlayerXPResetEvent event = new PlayerXPResetEvent(player, lostXP, reason);
        MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * Fires a level-up event for a player.
     * This is called by LevelUpHandler when checking for level progression.
     *
     * @param player The player who leveled up
     * @param oldLevel The previous level
     * @param newLevel The new level
     * @param skillPointsAwarded The skill points awarded
     */
    public void fireLevelUpEvent(ServerPlayer player, int oldLevel, int newLevel, int skillPointsAwarded) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerLevelUpEvent event = new PlayerLevelUpEvent(player, oldLevel, newLevel, skillPointsAwarded);
        MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * Removes all leveling data for a player.
     * Called when player data is completely reset.
     *
     * @param player The player to reset
     */
    public void removePlayerData(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
        data.removePlayerData(player.getUUID());
        AssistTracker.clearPlayerAssists(player.getUUID());
    }
}
