package com.complextalents.elemental;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized tracker for elemental stack data.
 * Stores all element stacks for entities and manages player-to-entity tracking relationships.
 *
 * <p>This class provides a single source of truth for all elemental stack data,
 * allowing multiple handlers and managers to access the same underlying storage.</p>
 */
public class ElementalStackTracker {

    /**
     * Stores element stacks for each entity.
     * Outer key: Entity UUID
     * Inner key: ElementType
     * Value: ElementStack
     */
    private static final Map<UUID, Map<ElementType, ElementStack>> entityElements = new ConcurrentHashMap<>();

    /**
     * Tracks which entities are being tracked by which players.
     * Used for the detonate ability and player logout cleanup.
     * Outer key: Player UUID
     * Inner value: Set of entity UUIDs that this player has affected
     */
    private static final Map<UUID, Set<UUID>> playerToEntitiesMap = new ConcurrentHashMap<>();

    /**
     * Reverse mapping from entity to the player who last affected it.
     * Used for reaction triggering when only the entity is known.
     * Key: Entity UUID
     * Value: Player UUID (may be null if no player involved)
     */
    private static final Map<UUID, UUID> entityToPlayerMap = new ConcurrentHashMap<>();

    // Private constructor to prevent instantiation
    private ElementalStackTracker() {}

    // ==================== Entity Elements Storage ====================

    /**
     * Gets the element stacks for a specific entity.
     *
     * @param entityId The entity UUID
     * @return The map of element types to stacks, or null if none exist
     */
    public static Map<ElementType, ElementStack> getEntityStacks(UUID entityId) {
        return entityElements.get(entityId);
    }

    /**
     * Gets or creates the element stacks map for a specific entity.
     *
     * @param entityId The entity UUID
     * @return The map of element types to stacks
     */
    public static Map<ElementType, ElementStack> getOrCreateEntityStacks(UUID entityId) {
        return entityElements.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Gets all entity elements.
     *
     * @return Unmodifiable view of the entity elements map
     */
    public static Map<UUID, Map<ElementType, ElementStack>> getAllEntityElements() {
        return entityElements;
    }

    /**
     * Removes all element stacks for an entity.
     *
     * @param entityId The entity UUID
     * @return The removed stacks, or null if none existed
     */
    public static Map<ElementType, ElementStack> removeEntityStacks(UUID entityId) {
        return entityElements.remove(entityId);
    }

    /**
     * Clears all element stacks for all entities.
     * Useful for testing or mod reload scenarios.
     */
    public static void clearAll() {
        entityElements.clear();
        playerToEntitiesMap.clear();
        entityToPlayerMap.clear();
    }

    // ==================== Player Tracking ====================

    /**
     * Adds a tracking relationship between a player and an entity.
     *
     * @param playerId The player UUID
     * @param entityId The entity UUID
     */
    public static void addTracking(UUID playerId, UUID entityId) {
        playerToEntitiesMap.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(entityId);
        entityToPlayerMap.put(entityId, playerId);
    }

    /**
     * Removes a tracking relationship for an entity.
     *
     * @param entityId The entity UUID
     */
    public static void removeEntityTracking(UUID entityId) {
        UUID playerId = entityToPlayerMap.remove(entityId);
        if (playerId != null) {
            Set<UUID> entities = playerToEntitiesMap.get(playerId);
            if (entities != null) {
                entities.remove(entityId);
                if (entities.isEmpty()) {
                    playerToEntitiesMap.remove(playerId);
                }
            }
        }
    }

    /**
     * Removes all tracking relationships for a player.
     *
     * @param playerId The player UUID
     */
    public static void removePlayerTracking(UUID playerId) {
        Set<UUID> entities = playerToEntitiesMap.remove(playerId);
        if (entities != null) {
            entities.forEach(entityToPlayerMap::remove);
        }
    }

    /**
     * Gets all entities tracked by a specific player.
     *
     * @param playerId The player UUID
     * @return Set of entity UUIDs, or empty set if none tracked
     */
    public static Set<UUID> getTrackedEntities(UUID playerId) {
        return playerToEntitiesMap.getOrDefault(playerId, Set.of());
    }

    /**
     * Gets the player who is tracking a specific entity.
     *
     * @param entityId The entity UUID
     * @return The player UUID, or null if no player is tracking
     */
    public static UUID getTrackingPlayer(UUID entityId) {
        return entityToPlayerMap.get(entityId);
    }

    // ==================== Statistics ====================

    /**
     * Gets statistics about the tracker state.
     *
     * @return Map containing statistics
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("total_entities_with_stacks", entityElements.size());
        stats.put("total_players_tracking", playerToEntitiesMap.size());
        stats.put("total_entity_player_relationships", entityToPlayerMap.size());

        // Count total stacks
        int totalStacks = entityElements.values().stream()
            .mapToInt(Map::size)
            .sum();
        stats.put("total_stacks", totalStacks);

        return stats;
    }

    /**
     * Checks if the tracker has any data.
     *
     * @return true if there are any entity elements or tracking relationships
     */
    public static boolean isEmpty() {
        return entityElements.isEmpty() && playerToEntitiesMap.isEmpty() && entityToPlayerMap.isEmpty();
    }
}
