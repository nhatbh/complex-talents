package com.complextalents.impl.highpriest.data;

import com.complextalents.impl.highpriest.entity.SeraphsEdgeEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side tracking for active Seraph's Edge swords.
 * Each player can only have one active sword at a time.
 */
public class SeraphSwordData {
    private static final Map<UUID, SeraphsEdgeEntity> ACTIVE_SWORDS = new HashMap<>();

    /**
     * Get the active sword for a player.
     */
    public static SeraphsEdgeEntity getActiveSword(Player player) {
        return getActiveSword(player.getUUID());
    }

    /**
     * Get the active sword by player UUID.
     */
    public static SeraphsEdgeEntity getActiveSword(UUID playerUuid) {
        SeraphsEdgeEntity sword = ACTIVE_SWORDS.get(playerUuid);
        if (sword != null && (!sword.isAlive() || !sword.level().isClientSide && sword.getOwner() == null)) {
            ACTIVE_SWORDS.remove(playerUuid);
            return null;
        }
        return sword;
    }

    /**
     * Set the active sword for a player.
     * If a sword already exists, it will be discarded.
     */
    public static void setActiveSword(Player player, SeraphsEdgeEntity sword) {
        SeraphsEdgeEntity existing = ACTIVE_SWORDS.get(player.getUUID());
        if (existing != null && existing != sword && existing.isAlive()) {
            existing.discard();
        }
        
        if (sword == null) {
            ACTIVE_SWORDS.remove(player.getUUID());
        } else {
            ACTIVE_SWORDS.put(player.getUUID(), sword);
        }
    }

    /**
     * Clear the active sword for a player.
     */
    public static void clearActiveSword(UUID playerUuid) {
        SeraphsEdgeEntity existing = ACTIVE_SWORDS.remove(playerUuid);
        if (existing != null && existing.isAlive()) {
            existing.discard();
        }
    }

    /**
     * Cleanup on player logout or origin change.
     */
    public static void cleanup(UUID playerUuid) {
        clearActiveSword(playerUuid);
    }
}
