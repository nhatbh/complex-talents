package com.complextalents.leveling.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory tracker for player assists during combat.
 * Assists are NOT persisted across server restarts - they are session-scoped.
 *
 * <p>This tracker maintains a window of which players have damaged a target recently.
 * When a target is killed, players within the assist window receive XP.</p>
 *
 * <p>Thread-safe using ConcurrentHashMap.</p>
 */
public class AssistTracker {
    // Map<PlayerUUID, Map<TargetUUID, LastInteractionTimestamp>>
    private static final Map<UUID, Map<UUID, Long>> assistData = new ConcurrentHashMap<>();

    /**
     * Records a player's assist on a target.
     * Updates the timestamp of the last interaction.
     *
     * @param playerId The UUID of the player who dealt damage
     * @param targetId The UUID of the entity being damaged
     * @param timestamp The current timestamp in milliseconds
     */
    public static void recordAssist(UUID playerId, UUID targetId, long timestamp) {
        assistData.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(targetId, timestamp);
    }

    /**
     * Checks if a player has an assist on a target within the assist window.
     *
     * @param playerId The UUID of the player
     * @param targetId The UUID of the target
     * @param currentTimestamp The current timestamp in milliseconds
     * @param windowMillis The assist window in milliseconds
     * @return true if the player has an assist within the window, false otherwise
     */
    public static boolean hasAssist(UUID playerId, UUID targetId, long currentTimestamp, long windowMillis) {
        Map<UUID, Long> playerAssists = assistData.get(playerId);
        if (playerAssists == null) return false;

        Long lastTime = playerAssists.get(targetId);
        if (lastTime == null) return false;

        return (currentTimestamp - lastTime) <= windowMillis;
    }

    /**
     * Clears a specific assist record.
     * Called after XP is awarded for an assist.
     *
     * @param playerId The UUID of the player
     * @param targetId The UUID of the target
     */
    public static void clearAssist(UUID playerId, UUID targetId) {
        Map<UUID, Long> playerAssists = assistData.get(playerId);
        if (playerAssists != null) {
            playerAssists.remove(targetId);
            // Clean up empty maps
            if (playerAssists.isEmpty()) {
                assistData.remove(playerId);
            }
        }
    }

    /**
     * Clears all assists for a player.
     * Called when a player logs out to prevent memory leaks.
     *
     * @param playerId The UUID of the player to clear
     */
    public static void clearPlayerAssists(UUID playerId) {
        assistData.remove(playerId);
    }

    /**
     * Clears all assist data.
     * Useful for testing or admin commands.
     */
    public static void clearAll() {
        assistData.clear();
    }

    /**
     * Gets the number of players currently tracked.
     * Useful for statistics/debugging.
     *
     * @return The number of players with active assists
     */
    public static int getTrackedPlayerCount() {
        return assistData.size();
    }

    /**
     * Gets the total number of assist records.
     * Useful for statistics/debugging.
     *
     * @return The total number of assist records
     */
    public static int getTotalAssistCount() {
        return assistData.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
