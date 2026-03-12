package com.complextalents.impl.highpriest.data;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.highpriest.FaithSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side tracking for High Priest Faith stacks.
 * Faith is UNCAPPED - no maximum limit.
 * Gained when spending mana on Holy spells while at 10 Grace stacks.
 */
public class FaithData {

    // Uncapped faith storage (can grow indefinitely) - stored as decimal for precision
    private static final ConcurrentHashMap<UUID, Double> FAITH_STACKS = new ConcurrentHashMap<>();

    /**
     * Get the current faith count for a player (as decimal).
     */
    public static double getFaith(UUID playerUuid) {
        return FAITH_STACKS.getOrDefault(playerUuid, 0.0);
    }

    /**
     * Get the current faith count for a player (as decimal).
     */
    public static double getFaith(ServerPlayer player) {
        return getFaith(player.getUUID());
    }

    /**
     * Get the current faith count floored to integer (for display/packet purposes).
     */
    public static int getFaithInt(UUID playerUuid) {
        return (int) getFaith(playerUuid);
    }

    /**
     * Get the current faith count floored to integer (for display/packet purposes).
     */
    public static int getFaithInt(ServerPlayer player) {
        return (int) getFaith(player.getUUID());
    }

    /**
     * Set faith count for a player (no cap - can be any positive value).
     */
    public static void setFaith(UUID playerUuid, double faith) {
        double clamped = Math.max(0.0, faith);
        FAITH_STACKS.put(playerUuid, clamped);
        TalentsMod.LOGGER.debug("High Priest faith set to {} for player {}", clamped, playerUuid);
    }

    /**
     * Set faith count for a player and sync to client.
     */
    public static void setFaith(ServerPlayer player, double faith) {
        setFaith(player.getUUID(), faith);
        syncToClient(player);
    }

    /**
     * Add faith to a player's count (no cap).
     */
    public static void addFaith(UUID playerUuid, double amount) {
        double current = getFaith(playerUuid);
        setFaith(playerUuid, current + amount);
    }

    /**
     * Add faith to a player's count and sync to client.
     */
    public static void addFaith(ServerPlayer player, double amount) {
        addFaith(player.getUUID(), amount);
        syncToClient(player);
    }

    /**
     * Sync faith data to a specific client.
     */
    public static void syncToClient(ServerPlayer player) {
        double faith = getFaith(player.getUUID());
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new FaithSyncPacket(faith));
    }

    /**
     * Clean up all data for a player (on logout/origin change).
     */
    public static void cleanup(UUID playerUuid) {
        FAITH_STACKS.remove(playerUuid);
        TalentsMod.LOGGER.debug("Cleaned up High Priest faith data for player {}", playerUuid);
    }

    /**
     * Clean up all data for a player.
     */
    public static void cleanup(ServerPlayer player) {
        cleanup(player.getUUID());
    }

    // --- NBT Serialization for Persistence ---

    /**
     * Serialize faith data for a player to NBT.
     * Used for saving to PlayerPersistentData.
     */
    public static CompoundTag serializeNBT(UUID playerUuid) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("faith", getFaith(playerUuid));
        return tag;
    }

    /**
     * Serialize faith data for a player to NBT.
     */
    public static CompoundTag serializeNBT(ServerPlayer player) {
        return serializeNBT(player.getUUID());
    }

    /**
     * Deserialize faith data from NBT.
     * Used for restoring from PlayerPersistentData.
     */
    public static void deserializeNBT(UUID playerUuid, CompoundTag tag) {
        if (tag.contains("faith")) {
            double faith = tag.getDouble("faith");
            setFaith(playerUuid, faith);
            TalentsMod.LOGGER.info("Restored {} faith for player {}", faith, playerUuid);
        }
    }

    /**
     * Deserialize faith data from NBT and sync to client.
     */
    public static void deserializeNBT(ServerPlayer player, CompoundTag tag) {
        deserializeNBT(player.getUUID(), tag);
        syncToClient(player);
    }
}
