package com.complextalents.leveling.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks healing relationships between healers and recipients.
 * When a healed player deals damage or kills an entity, the healer receives assist XP.
 *
 * <p>Structure: Map<HealedPlayerUUID, Map<HealerUUID, LastHealTimestamp>>
 * This tracks the most recent healer for each recipient.</p>
 *
 * <p>Thread-safe using ConcurrentHashMap.</p>
 */
public class HealerAssistTracker {
    // Map<HealedPlayerUUID, Map<HealerUUID, LastHealTimestamp>>
    private static final Map<UUID, Map<UUID, Long>> healingRelationships = new ConcurrentHashMap<>();

    /**
     * Records that a healer healed a player.
     * Updates the timestamp of the most recent heal from this healer.
     *
     * @param healedPlayerId The UUID of the player who was healed
     * @param healerUUID     The UUID of the healer
     * @param timestamp      The current timestamp in milliseconds
     */
    public static void recordHeal(UUID healedPlayerId, UUID healerUUID, long timestamp) {
        healingRelationships.computeIfAbsent(healedPlayerId, k -> new ConcurrentHashMap<>())
                .put(healerUUID, timestamp);
    }

    /**
     * Gets the most recent healer for a player within the window.
     * Returns null if no recent heal found.
     *
     * @param healedPlayerId   The UUID of the player
     * @param currentTimestamp The current timestamp in milliseconds
     * @param windowMillis     The healing window in milliseconds (usually 30000 for 30 seconds)
     * @return The UUID of the most recent healer, or null if none found
     */
    public static UUID getMostRecentHealer(UUID healedPlayerId, long currentTimestamp, long windowMillis) {
        Map<UUID, Long> healers = healingRelationships.get(healedPlayerId);
        if (healers == null || healers.isEmpty()) return null;

        // Find the healer with the most recent heal within the window
        UUID mostRecentHealer = null;
        long mostRecentTime = Long.MIN_VALUE;

        for (Map.Entry<UUID, Long> entry : healers.entrySet()) {
            long healTime = entry.getValue();
            if ((currentTimestamp - healTime) <= windowMillis && healTime > mostRecentTime) {
                mostRecentTime = healTime;
                mostRecentHealer = entry.getKey();
            }
        }

        return mostRecentHealer;
    }

    /**
     * Clears all healing records for a healed player.
     * Called when the window expires or when cleaning up.
     *
     * @param healedPlayerId The UUID of the healed player
     */
    public static void clearHeals(UUID healedPlayerId) {
        healingRelationships.remove(healedPlayerId);
    }

    /**
     * Clears all healing records.
     * Useful for testing or admin commands.
     */
    public static void clearAll() {
        healingRelationships.clear();
    }

    /**
     * Gets the number of players currently tracked.
     * Useful for statistics/debugging.
     *
     * @return The number of healed players with active healing relationships
     */
    public static int getTrackedPlayerCount() {
        return healingRelationships.size();
    }

    /**
     * Gets the total number of healer records.
     * Useful for statistics/debugging.
     *
     * @return The total number of healer records
     */
    public static int getTotalHealerCount() {
        return healingRelationships.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
