package com.complextalents.impl.yygm.state;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified player → entity tracking for the YYGM system.
 * Replaces three separate caches (PLAYER_HARMONIZED_CACHE, PLAYER_EXPOSED_CACHE, PLAYER_ANNIHILATION_CACHE).
 * <p>
 * This enforces the constraint that each player can only have ONE target at a time,
 * regardless of which state the target is in.
 * </p>
 */
public final class PlayerTargetTracker {

    private PlayerTargetTracker() {
        // Utility class - prevent instantiation
    }

    /** Unified cache: player UUID → (state + entity ID) */
    private static final ConcurrentHashMap<UUID, TargetData> PLAYER_TARGETS = new ConcurrentHashMap<>();

    /**
     * Combined data for a player's target, including both state and entity ID.
     *
     * @param state The current YYGM state of the target
     * @param entityId The ID of the target entity
     */
    public record TargetData(YinYangState state, int entityId) {

        /**
         * Create a TargetData from state and entity ID.
         */
        public static TargetData of(YinYangState state, int entityId) {
            return new TargetData(state, entityId);
        }

        /**
         * Check if this target data is valid.
         */
        public boolean isValid() {
            return state != null && entityId >= 0;
        }
    }

    /**
     * Set a player's target with a specific state.
     *
     * @param playerUuid The player's UUID
     * @param state The YYGM state
     * @param entityId The target entity's ID
     */
    public static void setTarget(UUID playerUuid, YinYangState state, int entityId) {
        PLAYER_TARGETS.put(playerUuid, new TargetData(state, entityId));
    }

    /**
     * Get a player's current target data.
     *
     * @param playerUuid The player's UUID
     * @return TargetData containing state and entity ID, or null if no target
     */
    public static TargetData getTarget(UUID playerUuid) {
        return PLAYER_TARGETS.get(playerUuid);
    }

    /**
     * Get the entity ID of a player's current target.
     *
     * @param playerUuid The player's UUID
     * @return Entity ID, or null if no target
     */
    public static Integer getEntityId(UUID playerUuid) {
        TargetData data = PLAYER_TARGETS.get(playerUuid);
        return data != null ? data.entityId() : null;
    }

    /**
     * Get the state of a player's current target.
     *
     * @param playerUuid The player's UUID
     * @return The YYGM state, or null if no target
     */
    public static YinYangState getState(UUID playerUuid) {
        TargetData data = PLAYER_TARGETS.get(playerUuid);
        return data != null ? data.state() : null;
    }

    /**
     * Check if a player has an active target.
     *
     * @param playerUuid The player's UUID
     * @return true if player has a target
     */
    public static boolean hasTarget(UUID playerUuid) {
        return PLAYER_TARGETS.containsKey(playerUuid);
    }

    /**
     * Clear a player's target.
     *
     * @param playerUuid The player's UUID
     */
    public static void clearTarget(UUID playerUuid) {
        PLAYER_TARGETS.remove(playerUuid);
    }

    /**
     * Clear all targets for a player (alias for clearTarget).
     *
     * @param playerUuid The player's UUID
     */
    public static void clearAllTargets(UUID playerUuid) {
        clearTarget(playerUuid);
    }

    /**
     * Get all player → target entries for iteration.
     * Used by server tick handlers.
     *
     * @return Set of all entries
     */
    public static Set<java.util.Map.Entry<UUID, TargetData>> getAllEntries() {
        return PLAYER_TARGETS.entrySet();
    }

    /**
     * Find a living entity by its ID across all loaded levels.
     *
     * @param server The Minecraft server
     * @param entityId The entity ID to find
     * @return The living entity, or null if not found/dead
     */
    public static LivingEntity findEntity(MinecraftServer server, int entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    /**
     * Check if a player has a target in a specific state.
     *
     * @param playerUuid The player's UUID
     * @param state The state to check
     * @return true if player's target is in the specified state
     */
    public static boolean hasTargetInState(UUID playerUuid, YinYangState state) {
        TargetData data = PLAYER_TARGETS.get(playerUuid);
        return data != null && data.state() == state;
    }

    /**
     * Check if a player has a specific entity as their target.
     *
     * @param playerUuid The player's UUID
     * @param entityId The entity ID to check
     * @return true if the player's target is the specified entity
     */
    public static boolean hasTargetEntity(UUID playerUuid, int entityId) {
        TargetData data = PLAYER_TARGETS.get(playerUuid);
        return data != null && data.entityId() == entityId;
    }

    /**
     * Get the number of active targets.
     * Useful for debugging and metrics.
     *
     * @return Number of players with active targets
     */
    public static int getActiveTargetCount() {
        return PLAYER_TARGETS.size();
    }
}
