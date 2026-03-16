package com.complextalents.leveling.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who participated in killing entities within a 30-second window.
 * Used for healer assist XP: when a healer heals a kill participant,
 * the healer receives assist XP too.
 *
 * <p>Thread-safe using ConcurrentHashMap.</p>
 */
public class KillParticipantTracker {
    // Map<VictimUUID, Map<ParticipantUUID, Timestamp>>
    private static final Map<UUID, Map<UUID, Long>> killParticipants = new ConcurrentHashMap<>();

    /**
     * Records a player's participation in a kill.
     * Called when a mob dies to record all players who dealt damage.
     *
     * @param victimId       The UUID of the killed entity
     * @param participantId  The UUID of the player who participated
     * @param timestamp      The current timestamp in milliseconds
     */
    public static void recordParticipant(UUID victimId, UUID participantId, long timestamp) {
        killParticipants.computeIfAbsent(victimId, k -> new ConcurrentHashMap<>())
                .put(participantId, timestamp);
    }

    /**
     * Checks if a player participated in a kill within the 30-second window.
     *
     * @param participantId   The UUID of the player to check
     * @param victimId        The UUID of the killed entity
     * @param currentTimestamp The current timestamp in milliseconds
     * @param windowMillis    The participation window in milliseconds (usually 30000 for 30 seconds)
     * @return true if the player participated within the window, false otherwise
     */
    public static boolean isParticipant(UUID participantId, UUID victimId, long currentTimestamp, long windowMillis) {
        Map<UUID, Long> victims = killParticipants.get(victimId);
        if (victims == null) return false;

        Long participationTime = victims.get(participantId);
        if (participationTime == null) return false;

        return (currentTimestamp - participationTime) <= windowMillis;
    }

    /**
     * Checks if a player has any recent kill participation within the window.
     * Used by healers to check if they should receive assist XP.
     *
     * @param participantId    The UUID of the player to check
     * @param currentTimestamp The current timestamp in milliseconds
     * @param windowMillis     The participation window in milliseconds (usually 30000 for 30 seconds)
     * @return true if the player participated in any kill within the window, false otherwise
     */
    public static boolean hasRecentKillParticipation(UUID participantId, long currentTimestamp, long windowMillis) {
        // Check all tracked kill victims to see if this player participated in any
        for (Map<UUID, Long> participants : killParticipants.values()) {
            Long participationTime = participants.get(participantId);
            if (participationTime != null && (currentTimestamp - participationTime) <= windowMillis) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all participants for a killed entity.
     * Called after processing all healers for a kill.
     *
     * @param victimId The UUID of the killed entity
     */
    public static void clearParticipants(UUID victimId) {
        killParticipants.remove(victimId);
    }

    /**
     * Clears all kill participant data.
     * Useful for testing or admin commands.
     */
    public static void clearAll() {
        killParticipants.clear();
    }

    /**
     * Gets the number of entities currently tracked.
     * Useful for statistics/debugging.
     *
     * @return The number of tracked kill victims
     */
    public static int getTrackedVictimCount() {
        return killParticipants.size();
    }

    /**
     * Gets the total number of kill participant records.
     * Useful for statistics/debugging.
     *
     * @return The total number of kill participant records
     */
    public static int getTotalParticipantCount() {
        return killParticipants.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
