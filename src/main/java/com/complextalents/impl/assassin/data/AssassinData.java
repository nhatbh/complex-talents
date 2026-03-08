package com.complextalents.impl.assassin.data;

import com.complextalents.network.PacketHandler;
import com.complextalents.network.assassin.AssassinSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side tracking for Assassin-specific data.
 */
public class AssassinData {

    // Stealth Gauge: 0 to Max (100-300 units)
    private static final ConcurrentHashMap<UUID, Double> STEALTH_GAUGE = new ConcurrentHashMap<>();

    /**
     * Get the current stealth gauge for a player.
     */
    public static double getStealthGauge(UUID playerUuid) {
        return STEALTH_GAUGE.getOrDefault(playerUuid, 100.0);
    }

    public static double getStealthGauge(ServerPlayer player) {
        return getStealthGauge(player.getUUID());
    }

    /**
     * Set the stealth gauge for a player (clamped to 0-Max).
     */
    public static void setStealthGauge(UUID playerUuid, double value, double max) {
        STEALTH_GAUGE.put(playerUuid, Math.min(max, Math.max(0.0, value)));
    }

    public static void setStealthGauge(ServerPlayer player, double value) {
        double max = getMaxGauge(player);
        setStealthGauge(player.getUUID(), value, max);
        syncToClient(player);
    }

    /**
     * Get the player's max gauge from origin stats.
     */
    public static double getMaxGauge(ServerPlayer player) {
        return com.complextalents.skill.SkillManager.getSkillStat(player,
                com.complextalents.impl.assassin.skill.ShadowWalkSkill.ID, "stealthGaugeSize");
    }

    /**
     * Sync data to client.
     */
    public static void syncToClient(ServerPlayer player) {
        double gauge = getStealthGauge(player);
        double max = getMaxGauge(player);
        PacketHandler.sendTo(new AssassinSyncPacket(gauge, max), player);
    }

    /**
     * Clean up data on logout/origin change.
     */
    public static void cleanup(UUID playerUuid) {
        STEALTH_GAUGE.remove(playerUuid);
    }

    public static CompoundTag serializeNBT(UUID playerUuid) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("stealthGauge", getStealthGauge(playerUuid));
        // Cooldowns are typically not persisted across restarts in this mod
        return tag;
    }

    public static void deserializeNBT(UUID playerUuid, CompoundTag tag) {
        if (tag.contains("stealthGauge")) {
            STEALTH_GAUGE.put(playerUuid, tag.getDouble("stealthGauge"));
        }
    }
}
