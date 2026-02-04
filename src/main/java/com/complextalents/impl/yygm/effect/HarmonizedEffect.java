package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harmonized Effect - The "Tao of Harmony" mechanic for Yin Yang Grandmaster.
 * <p>
 * When a YYGM melee attacks an enemy, the Harmonized effect is applied.
 * This effect manages the dual-gate system where:
 * - Two gates spawn: Gold (Yang) and Silver (Yin)
 * - Must strike gates in alternating order (Yang->Yin->Yang->Yin or Yin->Yang->Yin->Yang)
 * - Hitting correct gate deals True Damage + generates charge
 * - Hitting wrong gate triggers Discord (Nausea + Weakness, 15s)
 * - Gates are at random compass directions (never the same direction)
 * </p>
 * <p>
 * Gate data is stored per-player in entity NBT under "yygm_gates".
 * Charges are tracked via PassiveStack system ("yang" and "yin").
 * </p>
 */
public class HarmonizedEffect extends MobEffect {

    // NBT keys
    private static final String NBT_ROOT = "yygm_gates";
    private static final String NBT_PLAYER_UUID = "player_uuid";
    private static final String NBT_YANG_GATE = "yang_gate";
    private static final String NBT_YIN_GATE = "yin_gate";
    private static final String NBT_NEXT_REQUIRED = "next_required";
    private static final String NBT_GATE_COOLDOWN = "gate_cooldown";
    private static final String NBT_YANG_RESPAWN = "yang_respawn";
    private static final String NBT_YIN_RESPAWN = "yin_respawn";
    private static final String NBT_FIRST_APPLY_TIME = "first_apply_time";
    private static final String NBT_USED_SLOTS_BITMAP = "used_slots_bitmap"; // Bitmap of which compass slots have been used
    // Note: Equilibrium, pending hits, and last hit time are now player-global in EquilibriumData, not stored per-entity

    // Bitmap values for all 8 slots used (0-7 bits set)
    private static final int ALL_SLOTS_USED = 0xFF; // 255 = all 8 bits set

    // Compass directions: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW
    public static final int NUM_DIRECTIONS = 8;

    // Gate types
    public static final int GATE_YANG = 0;  // Gold gate
    public static final int GATE_YIN = 1;   // Silver gate
    public static final int GATE_NONE = -1; // No gate

    // Player NBT key for tracking current harmonized entity
    private static final String PLAYER_HARMONIZED_ENTITY = "harmonized_entity_id";

    // Server-side tracking
    private static final ConcurrentHashMap<UUID, Integer> PLAYER_HARMONIZED_CACHE = new ConcurrentHashMap<>();

    // Smart AoE target selection - damage caching
    private static final ConcurrentHashMap<UUID, java.util.List<DamageCandidate>> DAMAGE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> DAMAGE_HAS_DATA = new ConcurrentHashMap<>();

    /**
     * Represents a candidate for harmonization when multiple enemies are hit.
     */
    public static class DamageCandidate {
        public final int entityId;
        public final double distance;

        public DamageCandidate(int entityId, double distance) {
            this.entityId = entityId;
            this.distance = distance;
        }
    }

    public HarmonizedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFD700); // Gold color
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * Get or create the gate data NBT for a specific player on this entity.
     */
    private static CompoundTag getPlayerGateData(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        String playerKey = playerUuid.toString();

        if (!rootTag.contains(playerKey)) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(NBT_PLAYER_UUID, playerUuid);
            playerTag.putInt(NBT_YANG_GATE, GATE_NONE);
            playerTag.putInt(NBT_YIN_GATE, GATE_NONE);
            playerTag.putInt(NBT_NEXT_REQUIRED, GATE_NONE); // Will be set on first apply
            playerTag.putLong(NBT_GATE_COOLDOWN, 0);
            playerTag.putLong(NBT_YANG_RESPAWN, 0);
            playerTag.putLong(NBT_YIN_RESPAWN, 0);
            playerTag.putLong(NBT_FIRST_APPLY_TIME, entity.level().getGameTime());
            playerTag.putInt(NBT_USED_SLOTS_BITMAP, 0); // No slots used yet
            // Note: Equilibrium, pending hits, and last hit time are player-global in EquilibriumData
            rootTag.put(playerKey, playerTag);
            entity.getPersistentData().put(NBT_ROOT, rootTag);
        }

        return rootTag.getCompound(playerKey);
    }

    // Note: Equilibrium, pending hits, and last hit time are now player-global in EquilibriumData
    // The methods below have been removed - use EquilibriumData instead:
    // - getEquilibrium(), setEquilibrium()
    // - getLastHitTime(), setLastHitTime()
    // - getPendingYin(), setPendingYin()
    // - getPendingYang(), setPendingYang()
    // - isPairComplete(), resetPendingHits()

    /**
     * Initialize or refresh the Harmonized effect for a YYGM player.
     * Spawns dual gates (Yang + Yin) at different compass directions.
     * Randomly selects which gate is required first (50/50).
     * Does NOT switch targets if already harmonized.
     *
     * @return true if this was a new application, false otherwise
     */
    public static boolean applyToTarget(LivingEntity target, UUID playerUuid) {
        boolean targetChanged = false;
        if (!target.level().isClientSide) {
            // Check if player already has a harmonized target
            Integer currentHarmonizedId = PLAYER_HARMONIZED_CACHE.get(playerUuid);

            // If trying to harmonize the same target, just refresh (no switching)
            if (currentHarmonizedId != null && currentHarmonizedId == target.getId()) {
                // Same target - just refresh effect duration below
                targetChanged = false;
            } else if (currentHarmonizedId != null) {
                // Different target - don't switch, just refresh current effect
                targetChanged = false;
            } else {
                // No current target - set this one
                PLAYER_HARMONIZED_CACHE.put(playerUuid, target.getId());
                targetChanged = true;
            }
        }

        CompoundTag playerData = getPlayerGateData(target, playerUuid);
        long currentTime = target.level().getGameTime();
        long firstApplyTime = playerData.getLong(NBT_FIRST_APPLY_TIME);
        boolean isNew = false;

        if (targetChanged || firstApplyTime == 0) {
            // New application - set initial cooldown and spawn gates
            playerData.putLong(NBT_GATE_COOLDOWN, currentTime + 20); // 1 second cooldown

            // Randomly select first required gate (50/50)
            int firstRequired = target.getRandom().nextBoolean() ? GATE_YANG : GATE_YIN;
            playerData.putInt(NBT_NEXT_REQUIRED, firstRequired);

            // Spawn dual gates at different compass directions
            spawnDualGates(target, playerUuid, playerData, target.getRandom());

            isNew = true;
        }

        // Save the updated player data
        CompoundTag rootTag = target.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        target.getPersistentData().put(NBT_ROOT, rootTag);

        // Refresh the effect duration
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            YinYangEffects.HARMONIZED.get(),
            200, // 10 seconds
            0,
            false,
            false  // Hide particles
        ));

        return isNew;
    }

    /**
     * Spawn two gates (Yang and Yin) at different compass directions.
     * Only spawns in slots that haven't been used yet. Resets when all 8 slots are used.
     */
    public static void spawnDualGates(LivingEntity entity, UUID playerUuid,
                                       CompoundTag playerData, RandomSource random) {
        int usedBitmap = getUsedSlotsBitmap(entity, playerUuid);

        // Reset if all slots have been used
        if (usedBitmap == ALL_SLOTS_USED) {
            usedBitmap = 0;
            setUsedSlotsBitmap(entity, playerUuid, 0);
            TalentsMod.LOGGER.debug("All YYGM slots used, resetting for player {}", playerUuid);
        }

        // Get list of available (unused) slots
        java.util.List<Integer> availableSlots = getAvailableSlots(usedBitmap);

        // Need at least 2 available slots
        if (availableSlots.size() < 2) {
            // Edge case: reset and try again
            usedBitmap = 0;
            setUsedSlotsBitmap(entity, playerUuid, 0);
            availableSlots = getAvailableSlots(0);
        }

        // Pick two different random slots from available list
        int yangIndex = random.nextInt(availableSlots.size());
        int yangDir = availableSlots.get(yangIndex);

        int yinIndex;
        do {
            yinIndex = random.nextInt(availableSlots.size());
        } while (yinIndex == yangIndex);
        int yinDir = availableSlots.get(yinIndex);

        // Mark both slots as used
        markSlotsAsUsed(entity, playerUuid, yangDir, yinDir);

        playerData.putInt(NBT_YANG_GATE, yangDir);
        playerData.putInt(NBT_YIN_GATE, yinDir);

        // Save to entity NBT
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        entity.getPersistentData().put(NBT_ROOT, rootTag);

        TalentsMod.LOGGER.debug("Spawned YYGM gates: Yang at {}, Yin at {} for player {} on entity {} (used bitmap: {})",
            yangDir, yinDir, playerUuid, entity.getName().getString(), getUsedSlotsBitmap(entity, playerUuid));

        // Sync to client
        syncGateState(entity, playerUuid);
    }

    /**
     * Clear the Harmonized effect from the player's previous target.
     */
    // ==================== GATE DATA GETTERS/SETTERS ====================

    public static Integer getHarmonizedEntityId(UUID playerUuid) {
        return PLAYER_HARMONIZED_CACHE.get(playerUuid);
    }

    public static void clearHarmonizedTracking(UUID playerUuid) {
        PLAYER_HARMONIZED_CACHE.remove(playerUuid);
        DAMAGE_CACHE.remove(playerUuid);
        DAMAGE_HAS_DATA.remove(playerUuid);
    }

    // ==================== SMART AOE TARGET SELECTION ====================

    /**
     * Cache damage information for smart AoE target selection.
     */
    public static void cacheDamage(UUID playerUuid, int entityId, double distance) {
        DAMAGE_HAS_DATA.put(playerUuid, true);

        java.util.List<DamageCandidate> cache = DAMAGE_CACHE.computeIfAbsent(
            playerUuid, k -> new java.util.ArrayList<>());

        cache.add(new DamageCandidate(entityId, distance));
    }

    /**
     * Process and select the best target for harmonization from cached damage.
     * This should be called from a tick event.
     * Returns the entity ID of the closest enemy, or null if no candidates.
     * Clears the cache after processing.
     */
    public static Integer processAndSelectBestTarget(UUID playerUuid) {
        if (!DAMAGE_HAS_DATA.containsKey(playerUuid)) {
            return null;
        }

        java.util.List<DamageCandidate> candidates = DAMAGE_CACHE.get(playerUuid);
        DAMAGE_CACHE.remove(playerUuid);
        DAMAGE_HAS_DATA.remove(playerUuid);

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Find closest candidate
        DamageCandidate closest = null;
        for (DamageCandidate candidate : candidates) {
            if (closest == null || candidate.distance < closest.distance) {
                closest = candidate;
            }
        }

        return closest != null ? closest.entityId : null;
    }

    /**
     * Check if there is cached damage data for a player.
     */
    public static boolean hasCachedDamage(UUID playerUuid) {
        return DAMAGE_HAS_DATA.containsKey(playerUuid);
    }

    /**
     * Handle cleanup when Harmonized effect expires naturally.
     * Called from tick handler when effect is detected as expired.
     * Clears entity-specific tracking data only.
     * Note: Equilibrium is player-global and is preserved when effect expires.
     */
    public static void handleEffectExpiration(LivingEntity entity, UUID playerUuid) {
        // Remove from player's harmonized cache
        Integer cachedId = PLAYER_HARMONIZED_CACHE.get(playerUuid);
        if (cachedId != null && cachedId == entity.getId()) {
            PLAYER_HARMONIZED_CACHE.remove(playerUuid);
        }

        // Cleanup damage cache
        DAMAGE_CACHE.remove(playerUuid);
        DAMAGE_HAS_DATA.remove(playerUuid);

        // Cleanup entity NBT data (gate positions, etc.)
        cleanupPlayerData(entity, playerUuid);

        // Reset pending hits (these are tied to the harmonized target)
        com.complextalents.impl.yygm.EquilibriumData.resetPendingHits(playerUuid);

        // Send removal sync to clients
        if (entity.level() instanceof ServerLevel level) {
            PacketHandler.sendToNearby(
                new YinYangGateStateSyncPacket(
                    entity.getId(),
                    playerUuid,
                    -2, 0, 0, 0, 0, 0
                ),
                level,
                entity.position()
            );
        }

        // Note: Equilibrium stacks are player-global and preserved on effect expiration

        TalentsMod.LOGGER.debug("YYGM Harmonized effect expired for player {} on entity {}, gate tracking cleared",
            playerUuid, entity.getName().getString());
    }

    public static int getYangGateDirection(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getInt(NBT_YANG_GATE);
    }

    public static int getYinGateDirection(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getInt(NBT_YIN_GATE);
    }

    public static void setYangGateDirection(LivingEntity entity, UUID playerUuid, int direction) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putInt(NBT_YANG_GATE, direction);
        savePlayerData(entity, playerUuid, playerData);
    }

    public static void setYinGateDirection(LivingEntity entity, UUID playerUuid, int direction) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putInt(NBT_YIN_GATE, direction);
        savePlayerData(entity, playerUuid, playerData);
    }

    public static int getNextRequiredGate(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getInt(NBT_NEXT_REQUIRED);
    }

    public static void setNextRequiredGate(LivingEntity entity, UUID playerUuid, int nextRequired) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putInt(NBT_NEXT_REQUIRED, nextRequired);
        savePlayerData(entity, playerUuid, playerData);
    }

    public static long getGateCooldown(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getLong(NBT_GATE_COOLDOWN);
    }

    public static void setGateCooldown(LivingEntity entity, UUID playerUuid, long cooldownEnd) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putLong(NBT_GATE_COOLDOWN, cooldownEnd);
        savePlayerData(entity, playerUuid, playerData);
    }

    public static long getYangRespawnTick(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getLong(NBT_YANG_RESPAWN);
    }

    public static void setYangRespawnTick(LivingEntity entity, UUID playerUuid, long respawnTick) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putLong(NBT_YANG_RESPAWN, respawnTick);
        savePlayerData(entity, playerUuid, playerData);
    }

    public static long getYinRespawnTick(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getLong(NBT_YIN_RESPAWN);
    }

    public static void setYinRespawnTick(LivingEntity entity, UUID playerUuid, long respawnTick) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putLong(NBT_YIN_RESPAWN, respawnTick);
        savePlayerData(entity, playerUuid, playerData);
    }

    private static void savePlayerData(LivingEntity entity, UUID playerUuid, CompoundTag playerData) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        rootTag.put(playerUuid.toString(), playerData);
        entity.getPersistentData().put(NBT_ROOT, rootTag);
    }

    public static boolean hasAnyGates(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_ROOT);
    }

    public static java.util.Set<UUID> getGatePlayers(LivingEntity entity) {
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

    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid) {
        CompoundTag rootTag = entity.getPersistentData().getCompound(NBT_ROOT);
        rootTag.remove(playerUuid.toString());
        entity.getPersistentData().put(NBT_ROOT, rootTag);

        if (rootTag.isEmpty()) {
            entity.getPersistentData().remove(NBT_ROOT);
        }

        TalentsMod.LOGGER.debug("Cleaned up YYGM gate data for player {} on entity {}",
            playerUuid, entity.getName().getString());
    }

    // ==================== USED SLOTS TRACKING ====================

    /**
     * Get the bitmap of used compass slots for a player.
     * Each bit represents a slot: bit 0 = N, bit 1 = NE, ..., bit 7 = NW
     */
    public static int getUsedSlotsBitmap(LivingEntity entity, UUID playerUuid) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        return playerData.getInt(NBT_USED_SLOTS_BITMAP);
    }

    /**
     * Set the bitmap of used compass slots for a player.
     */
    public static void setUsedSlotsBitmap(LivingEntity entity, UUID playerUuid, int bitmap) {
        CompoundTag playerData = getPlayerGateData(entity, playerUuid);
        playerData.putInt(NBT_USED_SLOTS_BITMAP, bitmap);
        savePlayerData(entity, playerUuid, playerData);
    }

    /**
     * Mark specific slots as used in the bitmap.
     */
    public static void markSlotsAsUsed(LivingEntity entity, UUID playerUuid, int... slots) {
        int bitmap = getUsedSlotsBitmap(entity, playerUuid);
        for (int slot : slots) {
            bitmap |= (1 << slot);
        }
        setUsedSlotsBitmap(entity, playerUuid, bitmap);
    }

    /**
     * Get a list of available (unused) slot indices.
     */
    public static java.util.List<Integer> getAvailableSlots(int usedBitmap) {
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int slot = 0; slot < NUM_DIRECTIONS; slot++) {
            if ((usedBitmap & (1 << slot)) == 0) {
                available.add(slot);
            }
        }
        return available;
    }

    /**
     * Get a random available slot that is not the same as the excluded slot.
     * Resets bitmap if all slots are used.
     */
    public static int getRandomAvailableSlot(LivingEntity entity, UUID playerUuid, int excludeSlot, RandomSource random) {
        int usedBitmap = getUsedSlotsBitmap(entity, playerUuid);

        // Reset if all slots have been used
        if (usedBitmap == ALL_SLOTS_USED) {
            usedBitmap = 0;
            setUsedSlotsBitmap(entity, playerUuid, 0);
            TalentsMod.LOGGER.debug("All YYGM slots used, resetting for player {}", playerUuid);
        }

        java.util.List<Integer> availableSlots = getAvailableSlots(usedBitmap);

        // Remove the excluded slot from available list
        availableSlots.remove(Integer.valueOf(excludeSlot));

        // If no available slots (edge case), reset
        if (availableSlots.isEmpty()) {
            setUsedSlotsBitmap(entity, playerUuid, 0);
            availableSlots = getAvailableSlots(0);
            availableSlots.remove(Integer.valueOf(excludeSlot));
        }

        // Pick a random available slot
        int chosenSlot = availableSlots.get(random.nextInt(availableSlots.size()));

        // Mark the slot as used
        markSlotsAsUsed(entity, playerUuid, chosenSlot);

        return chosenSlot;
    }

    /**
     * Sync gate state to all nearby players.
     */
    public static void syncGateState(LivingEntity entity, UUID playerUuid) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        int yangGate = getYangGateDirection(entity, playerUuid);
        int yinGate = getYinGateDirection(entity, playerUuid);
        int nextRequired = getNextRequiredGate(entity, playerUuid);
        long cooldownEnd = getGateCooldown(entity, playerUuid);
        long yangRespawn = getYangRespawnTick(entity, playerUuid);
        long yinRespawn = getYinRespawnTick(entity, playerUuid);
        int usedSlotsBitmap = getUsedSlotsBitmap(entity, playerUuid);

        YinYangGateStateSyncPacket packet = new YinYangGateStateSyncPacket(
            entity.getId(), playerUuid, yangGate, yinGate, nextRequired,
            cooldownEnd, yangRespawn, yinRespawn, usedSlotsBitmap
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

    // ==================== SERVER TICK HANDLING ====================

    /**
     * Server tick handler for expired harmonized effect fail-safe checking.
     * Uses the internal player->entity cache for efficient iteration.
     * This should be called from a server tick event (throttled, e.g., every 5 ticks).
     *
     * @param server The Minecraft server instance
     */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (var entry : PLAYER_HARMONIZED_CACHE.entrySet()) {
            UUID playerUuid = entry.getKey();
            Integer entityId = entry.getValue();

            if (entityId == null) {
                continue;
            }

            // Find the entity in any loaded level
            LivingEntity harmonizedEntity = null;
            for (ServerLevel level : server.getAllLevels()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    harmonizedEntity = living;
                    break;
                }
            }

            if (harmonizedEntity == null) {
                // Entity not found or dead - clear the cache
                clearHarmonizedTracking(playerUuid);
                continue;
            }

            // Check for expired Harmonized effect (data exists but effect is gone)
            // This is a fail-safe to clean up orphaned data
            if (!harmonizedEntity.hasEffect(YinYangEffects.HARMONIZED.get())) {
                handleEffectExpiration(harmonizedEntity, playerUuid);
            }
        }
    }
}
