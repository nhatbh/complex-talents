package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yin Yang Annihilation Effect - The reward for completing all 8 Exposed gates.
 * <p>
 * When a YYGM successfully hits all 8 gates of the Exposed effect,
 * the target gains Yin Yang Annihilation. During this state:
 * - All attacks deal true amplified damage from any angle
 * - No gate restrictions
 * - Duration is the remaining Exposed duration
 * </p>
 */
public class YinYangAnnihilationEffect extends MobEffect {

    // NBT keys
    private static final String NBT_ROOT = "yygm_yin_yang_annihilation";
    private static final String NBT_PLAYER_UUID = "player_uuid";
    private static final String NBT_APPLY_TICK = "apply_tick";
    private static final String NBT_DURATION_TICKS = "duration_ticks";

    // Server-side tracking of player's current annihilation target
    private static final ConcurrentHashMap<UUID, Integer> PLAYER_ANNIHILATION_CACHE = new ConcurrentHashMap<>();

    public YinYangAnnihilationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000); // Crimson/red color
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * Get or create the Yin Yang Annihilation data NBT for a specific player on this entity.
     */
    private static CompoundTag getPlayerData(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        String playerKey = playerUuid.toString();

        if (!rootTag.contains(playerKey)) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(NBT_PLAYER_UUID, playerUuid);
            playerTag.putLong(NBT_APPLY_TICK, 0);
            playerTag.putInt(NBT_DURATION_TICKS, 0);
            rootTag.put(playerKey, playerTag);
            entity.getPersistentData().put(NBT_ROOT, rootTag);
        }

        return rootTag.getCompound(playerKey);
    }

    /**
     * Apply Yin Yang Annihilation effect to a target entity.
     * Removes Harmonized effect if present on this target.
     *
     * @param target The target entity
     * @param playerUuid The YYGM player's UUID
     * @param durationTicks Duration in ticks
     */
    public static void applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks) {
        if (!target.level().isClientSide) {
            // Remove Harmonized effect if present on this target
            Integer harmonizedId = HarmonizedEffect.getHarmonizedEntityId(playerUuid);
            if (harmonizedId != null && harmonizedId == target.getId()) {
                HarmonizedEffect.clearHarmonizedTracking(playerUuid);
                target.removeEffect(YinYangEffects.HARMONIZED.get());
                HarmonizedEffect.cleanupPlayerData(target, playerUuid);

                // Send removal packet to clear client-side harmonized gate visualization
                if (target.level() instanceof ServerLevel level) {
                    PacketHandler.sendToNearby(
                        new YinYangGateStateSyncPacket(
                            target.getId(), playerUuid, -2, 0, 0, 0, 0, 0
                        ),
                        level, target.position()
                    );
                }

                TalentsMod.LOGGER.debug("Removed Harmonized effect from target {} before applying Yin Yang Annihilation",
                    target.getName().getString());
            }

            // Set player's annihilation target
            PLAYER_ANNIHILATION_CACHE.put(playerUuid, target.getId());
        }

        long currentTime = target.level().getGameTime();

        CompoundTag playerData = getPlayerData(target, playerUuid);
        playerData.putLong(NBT_APPLY_TICK, currentTime);
        playerData.putInt(NBT_DURATION_TICKS, durationTicks);

        // Save the updated player data
        CompoundTag rootTag = target.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        target.getPersistentData().put(NBT_ROOT, rootTag);

        // Apply the mob effect instance
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            YinYangEffects.YIN_YANG_ANNIHILATION.get(),
            durationTicks,
            0,
            false,
            false  // Hide particles
        ));

        TalentsMod.LOGGER.debug("Applied Yin Yang Annihilation to {} for player {}, duration: {} ticks",
            target.getName().getString(), playerUuid, durationTicks);
    }

    /**
     * Check if a target has Yin Yang Annihilation by a specific player.
     */
    public static boolean hasYinYangAnnihilation(LivingEntity entity, UUID playerUuid) {
        return entity.hasEffect(YinYangEffects.YIN_YANG_ANNIHILATION.get());
    }

    /**
     * Get the entity ID of the player's current Yin Yang Annihilation target.
     */
    public static Integer getAnnihilationEntityId(UUID playerUuid) {
        return PLAYER_ANNIHILATION_CACHE.get(playerUuid);
    }

    /**
     * Set the player's Yin Yang Annihilation target.
     */
    public static void setAnnihilationTarget(UUID playerUuid, int entityId) {
        PLAYER_ANNIHILATION_CACHE.put(playerUuid, entityId);
    }

    /**
     * Clear the player's Yin Yang Annihilation target tracking.
     */
    public static void clearAnnihilationTarget(UUID playerUuid) {
        PLAYER_ANNIHILATION_CACHE.remove(playerUuid);
    }

    /**
     * Check if the player has an active Yin Yang Annihilation target.
     */
    public static boolean hasAnnihilationTarget(UUID playerUuid) {
        return PLAYER_ANNIHILATION_CACHE.containsKey(playerUuid);
    }

    /**
     * Remove the Yin Yang Annihilation effect from a target entity.
     */
    public static void removeFromTarget(LivingEntity entity, UUID playerUuid) {
        // Remove mob effect
        entity.removeEffect(YinYangEffects.YIN_YANG_ANNIHILATION.get());

        // Cleanup NBT data
        cleanupPlayerData(entity, playerUuid);

        // Clear player tracking if this was their target
        Integer cachedId = PLAYER_ANNIHILATION_CACHE.get(playerUuid);
        if (cachedId != null && cachedId == entity.getId()) {
            PLAYER_ANNIHILATION_CACHE.remove(playerUuid);
        }

        TalentsMod.LOGGER.debug("Removed Yin Yang Annihilation from {} for player {}",
            entity.getName().getString(), playerUuid);
    }

    /**
     * Clean up player data from entity NBT.
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        rootTag.remove(playerUuid.toString());
        entity.getPersistentData().put(NBT_ROOT, rootTag);

        if (rootTag.isEmpty()) {
            entity.getPersistentData().remove(NBT_ROOT);
        }

        TalentsMod.LOGGER.debug("Cleaned up Yin Yang Annihilation data for player {} on entity {}",
            playerUuid, entity.getName().getString());
    }

    /**
     * Get the remaining duration in ticks.
     */
    public static int getRemainingDuration(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerData(entity, playerUuid);
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);
        long currentTime = entity.level().getGameTime();
        long elapsed = currentTime - applyTick;
        return Math.max(0, duration - (int) elapsed);
    }

    /**
     * Check if the Yin Yang Annihilation effect has expired.
     */
    public static boolean isExpired(LivingEntity entity, UUID playerUuid, long currentTime) {
        CompoundTag playerData = getPlayerData(entity, playerUuid);
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);
        return currentTime >= applyTick + duration;
    }

    /**
     * Check if this entity has any Yin Yang Annihilation data from any player.
     */
    public static boolean hasAnyAnnihilation(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_ROOT);
    }

    /**
     * Get all players who have Yin Yang Annihilation data on this entity.
     */
    public static java.util.Set<UUID> getAnnihilationPlayers(LivingEntity entity) {
        java.util.Set<UUID> players = new java.util.HashSet<>();
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);

        for (String key : rootTag.getAllKeys()) {
            CompoundTag playerTag = rootTag.getCompound(key);
            if (playerTag.hasUUID(NBT_PLAYER_UUID)) {
                players.add(playerTag.getUUID(NBT_PLAYER_UUID));
            }
        }

        return players;
    }

    /**
     * Server tick handler for expired Yin Yang Annihilation effect checking.
     * This should be called from a server tick event (throttled, e.g., every 5 ticks).
     */
    public static void onServerTick(MinecraftServer server) {
        for (var entry : PLAYER_ANNIHILATION_CACHE.entrySet()) {
            UUID playerUuid = entry.getKey();
            Integer entityId = entry.getValue();

            if (entityId == null) {
                continue;
            }

            // Find the entity in any loaded level
            LivingEntity annihilationEntity = null;
            for (ServerLevel level : server.getAllLevels()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    annihilationEntity = living;
                    break;
                }
            }

            if (annihilationEntity == null) {
                // Entity not found or dead - clear the cache
                clearAnnihilationTarget(playerUuid);
                continue;
            }

            // Check if expired
            long currentTime = annihilationEntity.level().getGameTime();
            if (isExpired(annihilationEntity, playerUuid, currentTime)) {
                removeFromTarget(annihilationEntity, playerUuid);
                TalentsMod.LOGGER.debug("Yin Yang Annihilation effect expired for player {} on entity {}",
                    playerUuid, annihilationEntity.getName().getString());
            }
        }
    }
}
