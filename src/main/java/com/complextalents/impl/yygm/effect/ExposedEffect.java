package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exposed Effect - The "Eight Formation Battle Array" Ultimate for Yin Yang Grandmaster.
 * <p>
 * When a YYGM uses their Ultimate ability on an enemy, the Exposed effect is applied.
 * This effect manages the 8-gate system where:
 * - All 8 gates are active simultaneously (one at each compass direction)
 * - Gate types are randomly assigned (4 Yang, 4 Yin) at cast time
 * - Gates do NOT respawn after being hit
 * - Must hit all 8 gates correctly to convert to Yin Yang Annihilation
 * - Wrong gate hit clears Exposed immediately
 * - All attacks deal true amplified damage from any angle (Yin Yang Annihilation)
 * </p>
 * <p>
 * Gate data is stored per-player in entity NBT under "yygm_exposed".
 * </p>
 */
public class ExposedEffect extends MobEffect {

    // NBT keys
    private static final String NBT_ROOT = "yygm_exposed";
    private static final String NBT_PLAYER_UUID = "player_uuid";
    private static final String NBT_APPLY_TICK = "apply_tick";
    private static final String NBT_DURATION_TICKS = "duration_ticks";
    private static final String NBT_GATE_PATTERN = "gate_pattern"; // 8-bit bitmap: 1=Yang, 0=Yin per direction
    private static final String NBT_COMPLETED_GATES = "completed_gates"; // 8-bit bitmap
    // Note: nextRequired is now player-global in EquilibriumData, not stored per-entity

    // Compass directions: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW
    public static final int NUM_DIRECTIONS = 8;

    // Gate types
    public static final int GATE_YANG = 0;  // Gold gate
    public static final int GATE_YIN = 1;   // Silver gate
    public static final int GATE_NONE = -1; // No gate

    // Server-side tracking of player's current exposed target
    private static final ConcurrentHashMap<UUID, Integer> PLAYER_EXPOSED_CACHE = new ConcurrentHashMap<>();

    public ExposedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4444); // Red color for Exposed
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * Get or create the Exposed data NBT for a specific player on this entity.
     */
    private static CompoundTag getPlayerExposedData(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        String playerKey = playerUuid.toString();

        if (!rootTag.contains(playerKey)) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(NBT_PLAYER_UUID, playerUuid);
            playerTag.putLong(NBT_APPLY_TICK, 0);
            playerTag.putInt(NBT_DURATION_TICKS, 0);
            playerTag.putInt(NBT_GATE_PATTERN, 0);
            playerTag.putInt(NBT_COMPLETED_GATES, 0);
            // Note: nextRequired is now player-global in EquilibriumData
            rootTag.put(playerKey, playerTag);
            entity.getPersistentData().put(NBT_ROOT, rootTag);
        }

        return rootTag.getCompound(playerKey);
    }

    /**
     * Generate a random gate pattern with exactly 4 Yang (1) and 4 Yin (0).
     * Returns an 8-bit bitmap where bit position = compass direction (0-7).
     */
    public static int generateRandomGatePattern(RandomSource random) {
        // Create array with 4 Yangs and 4 Yins
        int[] gates = {GATE_YANG, GATE_YANG, GATE_YANG, GATE_YANG,
                       GATE_YIN, GATE_YIN, GATE_YIN, GATE_YIN};

        // Shuffle using Fisher-Yates algorithm
        for (int i = gates.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = gates[i];
            gates[i] = gates[j];
            gates[j] = temp;
        }

        // Convert to bitmap (1 = Yang, 0 = Yin)
        int pattern = 0;
        for (int i = 0; i < 8; i++) {
            if (gates[i] == GATE_YANG) {
                pattern |= (1 << i);  // Set bit for Yang
            }
        }
        return pattern;
    }

    /**
     * Apply the Exposed effect to a target entity.
     * Removes Harmonized effect if present on this target.
     *
     * @param target The target entity
     * @param playerUuid The YYGM player's UUID
     * @param durationTicks Duration in ticks
     * @return true if successfully applied
     */
    public static boolean applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks) {
        // Note: nextRequired is now player-global in EquilibriumData
        // It carries over automatically from Harmonized without special handling
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
                        new com.complextalents.network.yygm.YinYangGateStateSyncPacket(
                            target.getId(), playerUuid, -2, 0, 0, 0, 0, 0
                        ),
                        level, target.position()
                    );
                }

                TalentsMod.LOGGER.debug("Removed Harmonized effect from target {} before applying Exposed, nextRequired carries over from EquilibriumData",
                    target.getName().getString());
            }

            // Set player's exposed target
            PLAYER_EXPOSED_CACHE.put(playerUuid, target.getId());
        }

        long currentTime = target.level().getGameTime();

        // Generate random gate pattern
        int gatePattern = generateRandomGatePattern(target.getRandom());

        CompoundTag playerData = getPlayerExposedData(target, playerUuid);
        playerData.putLong(NBT_APPLY_TICK, currentTime);
        playerData.putInt(NBT_DURATION_TICKS, durationTicks);
        playerData.putInt(NBT_GATE_PATTERN, gatePattern);
        playerData.putInt(NBT_COMPLETED_GATES, 0);

        // Save the updated player data
        CompoundTag rootTag = target.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        target.getPersistentData().put(NBT_ROOT, rootTag);

        // Apply the mob effect instance
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            YinYangEffects.EXPOSED.get(),
            durationTicks,
            0,
            false,
            false  // Hide particles
        ));

        // Sync to client
        syncExposedState(target, playerUuid);

        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);
        TalentsMod.LOGGER.debug("Applied Exposed effect to {} for player {}, duration: {} ticks, pattern: 0x{}, nextRequired: {}",
            target.getName().getString(), playerUuid, durationTicks, Integer.toHexString(gatePattern), nextRequired);

        return true;
    }

    /**
     * Remove the Exposed effect from a target entity.
     */
    public static void removeFromTarget(LivingEntity entity, UUID playerUuid) {
        // Remove mob effect
        entity.removeEffect(YinYangEffects.EXPOSED.get());

        // Cleanup NBT data
        cleanupPlayerData(entity, playerUuid);

        // Clear player tracking if this was their target
        Integer cachedId = PLAYER_EXPOSED_CACHE.get(playerUuid);
        if (cachedId != null && cachedId == entity.getId()) {
            PLAYER_EXPOSED_CACHE.remove(playerUuid);
        }

        // Send removal sync to clients
        if (entity.level() instanceof ServerLevel level) {
            PacketHandler.sendToNearby(
                new ExposedStateSyncPacket(entity.getId(), playerUuid, 0, 0, 0, 0),
                level, entity.position()
            );
        }

        TalentsMod.LOGGER.debug("Removed Exposed effect from {} for player {}",
            entity.getName().getString(), playerUuid);
    }

    /**
     * Check if a target is Exposed by a specific player.
     */
    public static boolean isExposed(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        if (!rootTag.contains(playerUuid.toString())) {
            return false;
        }
        // Check if still within duration
        CompoundTag playerData = rootTag.getCompound(playerUuid.toString());
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);
        long currentTime = entity.level().getGameTime();
        return currentTime < applyTick + duration;
    }

    /**
     * Get the entity ID of the player's current Exposed target.
     */
    public static Integer getExposedEntityId(UUID playerUuid) {
        return PLAYER_EXPOSED_CACHE.get(playerUuid);
    }

    /**
     * Set the player's Exposed target.
     */
    public static void setExposedTarget(UUID playerUuid, int entityId) {
        PLAYER_EXPOSED_CACHE.put(playerUuid, entityId);
    }

    /**
     * Clear the player's Exposed target tracking.
     */
    public static void clearExposedTarget(UUID playerUuid) {
        PLAYER_EXPOSED_CACHE.remove(playerUuid);
    }

    /**
     * Check if the player has an active Exposed target.
     */
    public static boolean hasExposedTarget(UUID playerUuid) {
        return PLAYER_EXPOSED_CACHE.containsKey(playerUuid);
    }

    /**
     * Get the gate pattern bitmap for a player on this entity.
     */
    public static int getGatePattern(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        return playerData.getInt(NBT_GATE_PATTERN);
    }

    /**
     * Get the gate type at a specific compass direction.
     * @return GATE_YANG or GATE_YIN
     */
    public static int getGateTypeAtDirection(LivingEntity entity, UUID playerUuid, int direction) {
        int pattern = getGatePattern(entity, playerUuid);
        if ((pattern & (1 << direction)) != 0) {
            return GATE_YANG;
        }
        return GATE_YIN;
    }

    /**
     * Check if a hit at the given compass direction is a correct gate hit.
     * @param requiredGateType GATE_YANG or GATE_YIN - the gate type the player must hit
     */
    public static boolean isCorrectGateHit(LivingEntity entity, UUID playerUuid, int compassDirection, int requiredGateType) {
        int actualGateType = getGateTypeAtDirection(entity, playerUuid, compassDirection);
        return actualGateType == requiredGateType;
    }

    /**
     * Check if a hit at the given compass direction is a correct gate hit (auto-detect required type).
     */
    public static boolean isCorrectGateHit(LivingEntity entity, UUID playerUuid, int compassDirection) {
        return isCorrectGateHit(entity, playerUuid, compassDirection, getGateTypeAtDirection(entity, playerUuid, compassDirection));
    }

    /**
     * Mark a gate as completed.
     * @return true if the gate was newly completed, false if already completed
     */
    public static boolean markGateCompleted(LivingEntity entity, UUID playerUuid, int compassDirection) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        int completedBitmap = playerData.getInt(NBT_COMPLETED_GATES);
        int bit = 1 << compassDirection;

        if ((completedBitmap & bit) != 0) {
            return false; // Already completed
        }

        // Mark as completed
        completedBitmap |= bit;
        playerData.putInt(NBT_COMPLETED_GATES, completedBitmap);
        savePlayerData(entity, playerUuid, playerData);

        return true;
    }

    /**
     * Get the count of completed gates (0-8).
     */
    public static int getCompletedGateCount(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        int completedBitmap = playerData.getInt(NBT_COMPLETED_GATES);
        return Integer.bitCount(completedBitmap);
    }

    /**
     * Check if all 8 gates have been completed.
     */
    public static boolean isAllGatesCompleted(LivingEntity entity, UUID playerUuid) {
        return getCompletedGateCount(entity, playerUuid) >= 8;
    }

    /**
     * Get the remaining duration in ticks.
     */
    public static int getRemainingDuration(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);
        long currentTime = entity.level().getGameTime();
        long elapsed = currentTime - applyTick;
        return Math.max(0, duration - (int) elapsed);
    }

    /**
     * Check if the Exposed effect has expired.
     */
    public static boolean isExpired(LivingEntity entity, UUID playerUuid, long currentTime) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);
        return currentTime >= applyTick + duration;
    }

    /**
     * Get the completed gates bitmap.
     */
    public static int getCompletedGatesBitmap(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        return playerData.getInt(NBT_COMPLETED_GATES);
    }

    /**
     * Get the next required gate type.
     * @return GATE_YANG (0) or GATE_YIN (1)
     */
    // Note: getNextRequired() and setNextRequired() removed
    // Use EquilibriumData.getNextRequired() and EquilibriumData.setNextRequired() instead

    /**
     * Save player data to entity NBT.
     */
    private static void savePlayerData(LivingEntity entity, UUID playerUuid, CompoundTag playerData) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        entity.getPersistentData().put(NBT_ROOT, rootTag);
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

        TalentsMod.LOGGER.debug("Cleaned up Exposed data for player {} on entity {}",
            playerUuid, entity.getName().getString());
    }

    /**
     * Check if this entity has any Exposed data from any player.
     * Used for syncing gates on login/dimension change.
     */
    public static boolean hasAnyExposed(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_ROOT);
    }

    /**
     * Get all players who have Exposed data on this entity.
     * Used for syncing gates on login/dimension change.
     */
    public static java.util.Set<UUID> getExposedPlayers(LivingEntity entity) {
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
     * Sync Exposed state to all nearby players.
     */
    public static void syncExposedState(LivingEntity entity, UUID playerUuid) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        int gatePattern = getGatePattern(entity, playerUuid);
        int completedGates = getCompletedGatesBitmap(entity, playerUuid);
        // nextRequired is now player-global in EquilibriumData
        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);
        CompoundTag playerData = getPlayerExposedData(entity, playerUuid);
        long applyTick = playerData.getLong(NBT_APPLY_TICK);
        int duration = playerData.getInt(NBT_DURATION_TICKS);

        ExposedStateSyncPacket packet = new ExposedStateSyncPacket(
            entity.getId(), playerUuid, gatePattern, completedGates, nextRequired, applyTick + duration
        );

        PacketHandler.sendToNearby(packet, level, entity.position());
    }

    /**
     * Calculate the compass direction (0-7) from an attack angle.
     * 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW
     */
    public static int angleToCompassDirection(double angleRadians) {
        double angleDegrees = Math.toDegrees(angleRadians);

        while (angleDegrees < 0) {
            angleDegrees += 360;
        }
        while (angleDegrees >= 360) {
            angleDegrees -= 360;
        }

        int direction = (int) Math.floor((angleDegrees + 22.5) / 45.0);
        return direction % 8;
    }

    /**
     * Calculate the attack angle from target to attacker.
     * Returns angle in radians where 0 = North (positive Z), clockwise positive.
     */
    public static double calculateAttackAngle(LivingEntity target, LivingEntity attacker) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();

        double angle = Math.atan2(dx, -dz);

        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }

    /**
     * Server tick handler for expired Exposed effect checking.
     * This should be called from a server tick event (throttled, e.g., every 5 ticks).
     */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (var entry : PLAYER_EXPOSED_CACHE.entrySet()) {
            UUID playerUuid = entry.getKey();
            Integer entityId = entry.getValue();

            if (entityId == null) {
                continue;
            }

            // Find the entity in any loaded level
            LivingEntity exposedEntity = null;
            for (ServerLevel level : server.getAllLevels()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    exposedEntity = living;
                    break;
                }
            }

            if (exposedEntity == null) {
                // Entity not found or dead - clear the cache
                clearExposedTarget(playerUuid);
                continue;
            }

            // Check if expired
            long currentTime = exposedEntity.level().getGameTime();
            if (isExpired(exposedEntity, playerUuid, currentTime)) {
                removeFromTarget(exposedEntity, playerUuid);
                TalentsMod.LOGGER.debug("Exposed effect expired for player {} on entity {}",
                    playerUuid, exposedEntity.getName().getString());
            }
        }
    }
}
