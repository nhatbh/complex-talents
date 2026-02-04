package com.complextalents.impl.yygm.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility class for unified NBT data management in the YYGM system.
 * Consolidates duplicate NBT patterns from HarmonizedEffect, ExposedEffect, and YinYangAnnihilationEffect.
 * <p>
 * Note: Duration tracking is handled by Minecraft's MobEffectInstance system, not NBT.
 * This class only manages effect-specific state data (gate positions, patterns, etc.).
 * </p>
 */
public final class YinYangNbtUtil {

    private YinYangNbtUtil() {
        // Utility class - prevent instantiation
    }

    /** Standard NBT key for player UUID */
    public static final String NBT_PLAYER_UUID = "player_uuid";

    /**
     * Get or create player data NBT for a specific player on this entity.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @param nbtRoot The root NBT key for this effect type
     * @param initializer Consumer to initialize new data if it doesn't exist
     * @return The player's data tag
     */
    public static CompoundTag getOrCreatePlayerData(LivingEntity entity, UUID playerUuid,
                                                     String nbtRoot,
                                                     Consumer<CompoundTag> initializer) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(nbtRoot);
        String playerKey = playerUuid.toString();

        if (!rootTag.contains(playerKey)) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(NBT_PLAYER_UUID, playerUuid);
            initializer.accept(playerTag);
            rootTag.put(playerKey, playerTag);
            entity.getPersistentData().put(nbtRoot, rootTag);
        }

        return rootTag.getCompound(playerKey);
    }

    /**
     * Save player data to entity NBT.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @param playerData The data to save
     * @param nbtRoot The root NBT key for this effect type
     */
    public static void savePlayerData(LivingEntity entity, UUID playerUuid,
                                      CompoundTag playerData, String nbtRoot) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(nbtRoot);
        rootTag.put(playerUuid.toString(), playerData);
        entity.getPersistentData().put(nbtRoot, rootTag);
    }

    /**
     * Clean up player data from entity NBT.
     * Also removes the root tag if it becomes empty.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @param nbtRoot The NBT root key for this effect type
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid, String nbtRoot) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(nbtRoot);
        rootTag.remove(playerUuid.toString());
        entity.getPersistentData().put(nbtRoot, rootTag);

        if (rootTag.isEmpty()) {
            entity.getPersistentData().remove(nbtRoot);
        }
    }

    /**
     * Get all players who have data of this type on this entity.
     *
     * @param entity The entity to check
     * @param nbtRoot The root NBT key for this effect type
     * @param playerUuidKey The NBT key for player UUID (may differ from NBT_PLAYER_UUID)
     * @return Set of player UUIDs
     */
    public static Set<UUID> getPlayersWithData(LivingEntity entity, String nbtRoot, String playerUuidKey) {
        Set<UUID> players = new HashSet<>();
        CompoundTag rootTag = entity.getPersistentData().getCompound(nbtRoot);

        for (String key : rootTag.getAllKeys()) {
            CompoundTag playerTag = rootTag.getCompound(key);
            if (playerTag.hasUUID(playerUuidKey)) {
                players.add(playerTag.getUUID(playerUuidKey));
            }
        }

        return players;
    }

    /**
     * Check if this entity has any data of this type.
     *
     * @param entity The entity to check
     * @param nbtRoot The root NBT key for this effect type
     * @return true if any data exists
     */
    public static boolean hasAnyData(LivingEntity entity, String nbtRoot) {
        return entity.getPersistentData().contains(nbtRoot);
    }
}
