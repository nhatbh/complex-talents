package com.complextalents.impl.yygm;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.EquilibriumSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side tracking for YYGM Equilibrium resource.
 * Equilibrium is gained by completing Yin+Yang gate pairs (max 8).
 * Lost by: wrong gate type (all), 10s inactivity (all), empty gate (1).
 */
public class EquilibriumData {

    // Maximum equilibrium stacks
    public static final int MAX_EQUILIBRIUM = 8;

    // Decay time in ticks (10 seconds = 200 ticks)
    public static final long DECAY_TICKS = 200L;

    // Per-player tracking
    private static final ConcurrentHashMap<UUID, Integer> EQUILIBRIUM_STACKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_GATE_HIT_TIME = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> PENDING_YIN_HIT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> PENDING_YANG_HIT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> NEXT_REQUIRED_GATE = new ConcurrentHashMap<>();

    /**
     * Get the current equilibrium stacks for a player.
     */
    public static int getEquilibrium(UUID playerUuid) {
        return EQUILIBRIUM_STACKS.getOrDefault(playerUuid, 0);
    }

    /**
     * Get the current equilibrium stacks for a player.
     */
    public static int getEquilibrium(ServerPlayer player) {
        return getEquilibrium(player.getUUID());
    }

    /**
     * Set equilibrium stacks for a player (clamped to 0-MAX).
     * Syncs to client automatically.
     */
    public static void setEquilibrium(UUID playerUuid, int stacks) {
        int clamped = Math.max(0, Math.min(MAX_EQUILIBRIUM, stacks));
        EQUILIBRIUM_STACKS.put(playerUuid, clamped);

        TalentsMod.LOGGER.debug("YYGM Equilibrium set to {} for player {}", clamped, playerUuid);
    }

    /**
     * Set equilibrium stacks for a player (clamped to 0-MAX).
     * Syncs to client automatically.
     */
    public static void setEquilibrium(ServerPlayer player, int stacks) {
        setEquilibrium(player.getUUID(), stacks);
        syncToClient(player);
    }

    /**
     * Modify equilibrium stacks by a delta amount (clamped to 0-MAX).
     * Syncs to client automatically.
     */
    public static void modifyEquilibrium(UUID playerUuid, int delta) {
        int current = getEquilibrium(playerUuid);
        int newValue = current + delta;
        setEquilibrium(playerUuid, newValue);
    }

    /**
     * Modify equilibrium stacks by a delta amount (clamped to 0-MAX).
     * Syncs to client automatically.
     */
    public static void modifyEquilibrium(ServerPlayer player, int delta) {
        modifyEquilibrium(player.getUUID(), delta);
        syncToClient(player);
    }

    /**
     * Reset equilibrium to 0 for a player.
     */
    public static void resetEquilibrium(UUID playerUuid) {
        setEquilibrium(playerUuid, 0);
    }

    /**
     * Reset equilibrium to 0 for a player. Syncs to client.
     */
    public static void resetEquilibrium(ServerPlayer player) {
        setEquilibrium(player, 0);
        syncToClient(player);
    }

    /**
     * Check if player has hit a Yin gate (for pair completion tracking).
     */
    public static boolean hasPendingYinHit(UUID playerUuid) {
        return PENDING_YIN_HIT.getOrDefault(playerUuid, false);
    }

    /**
     * Check if player has hit a Yang gate (for pair completion tracking).
     */
    public static boolean hasPendingYangHit(UUID playerUuid) {
        return PENDING_YANG_HIT.getOrDefault(playerUuid, false);
    }

    /**
     * Set pending Yin hit state.
     */
    public static void setPendingYinHit(UUID playerUuid, boolean value) {
        if (value) {
            PENDING_YIN_HIT.put(playerUuid, true);
        } else {
            PENDING_YIN_HIT.remove(playerUuid);
        }
    }

    /**
     * Set pending Yang hit state.
     */
    public static void setPendingYangHit(UUID playerUuid, boolean value) {
        if (value) {
            PENDING_YANG_HIT.put(playerUuid, true);
        } else {
            PENDING_YANG_HIT.remove(playerUuid);
        }
    }

    /**
     * Check if both Yin and Yang have been hit (pair complete).
     */
    public static boolean isPairComplete(UUID playerUuid) {
        return hasPendingYinHit(playerUuid) && hasPendingYangHit(playerUuid);
    }

    /**
     * Reset pending hits after gaining Equilibrium.
     */
    public static void resetPendingHits(UUID playerUuid) {
        PENDING_YIN_HIT.remove(playerUuid);
        PENDING_YANG_HIT.remove(playerUuid);
    }

    /**
     * Get the next required gate type for a player.
     * @return GATE_YANG (0) or GATE_YIN (1)
     */
    public static int getNextRequired(UUID playerUuid) {
        return NEXT_REQUIRED_GATE.getOrDefault(playerUuid, com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YANG);
    }

    /**
     * Set the next required gate type for a player.
     */
    public static void setNextRequired(UUID playerUuid, int gateType) {
        if (gateType == com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YANG
            || gateType == com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YIN) {
            NEXT_REQUIRED_GATE.put(playerUuid, gateType);
        }
    }

    /**
     * Toggle the next required gate (Yang <-> Yin).
     */
    public static void toggleNextRequired(UUID playerUuid) {
        int current = getNextRequired(playerUuid);
        int newRequired = (current == com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YANG)
            ? com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YIN
            : com.complextalents.impl.yygm.events.YYGMGateHitEvent.GATE_YANG;
        setNextRequired(playerUuid, newRequired);
    }

    /**
     * Update the last gate hit time for decay tracking.
     */
    public static void updateLastHitTime(UUID playerUuid, long currentTime) {
        LAST_GATE_HIT_TIME.put(playerUuid, currentTime);
    }

    /**
     * Update the last gate hit time for decay tracking.
     */
    public static void updateLastHitTime(ServerPlayer player) {
        LAST_GATE_HIT_TIME.put(player.getUUID(), player.level().getGameTime());
    }

    /**
     * Get the last gate hit time for a player.
     */
    public static long getLastHitTime(UUID playerUuid) {
        Long time = LAST_GATE_HIT_TIME.get(playerUuid);
        return time != null ? time : 0L;
    }

    /**
     * Check if equilibrium should decay (10 seconds since last gate hit).
     */
    public static boolean shouldDecay(UUID playerUuid, long currentTime) {
        Long lastHit = LAST_GATE_HIT_TIME.get(playerUuid);
        if (lastHit == null) {
            return false;
        }
        return (currentTime - lastHit) >= DECAY_TICKS;
    }

    /**
     * Check if equilibrium should decay (10 seconds since last gate hit).
     */
    public static boolean shouldDecay(ServerPlayer player) {
        return shouldDecay(player.getUUID(), player.level().getGameTime());
    }

    /**
     * Sync equilibrium data to a specific client.
     */
    public static void syncToClient(ServerPlayer player) {
        int equilibrium = getEquilibrium(player.getUUID());
        long lastHitTime = getLastHitTime(player.getUUID());
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
            new EquilibriumSyncPacket(equilibrium, lastHitTime));
    }

    /**
     * Clean up all data for a player (on logout/origin change).
     */
    public static void cleanup(UUID playerUuid) {
        EQUILIBRIUM_STACKS.remove(playerUuid);
        LAST_GATE_HIT_TIME.remove(playerUuid);
        PENDING_YIN_HIT.remove(playerUuid);
        PENDING_YANG_HIT.remove(playerUuid);
        NEXT_REQUIRED_GATE.remove(playerUuid);
        TalentsMod.LOGGER.debug("Cleaned up YYGM Equilibrium data for player {}", playerUuid);
    }

    /**
     * Clean up all data for a player (on logout/origin change).
     */
    public static void cleanup(ServerPlayer player) {
        cleanup(player.getUUID());
    }
}
