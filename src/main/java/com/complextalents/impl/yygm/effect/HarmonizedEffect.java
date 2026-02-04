package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.state.PlayerTargetTracker;
import com.complextalents.impl.yygm.state.YinYangState;
import com.complextalents.impl.yygm.util.GateSpawnStrategy;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harmonized Effect - The default combat state for Yin Yang Grandmaster.
 * <p>
 * When a YYGM attacks an enemy, the Harmonized effect is applied.
 * This effect manages the dual-gate system where:
 * - 2 gates spawn (1 Yang, 1 Yin) at different compass directions
 * - Gates respawn after being hit
 * - Player must alternate between Yin and Yang gates to gain Equilibrium
 * - Wrong gate hit loses all Equilibrium
 * - Empty gate hit loses 1 Equilibrium
 * </p>
 * <p>
 * Refactored to extend BaseYinYangEffect and use unified PlayerTargetTracker.
 * </p>
 */
public class HarmonizedEffect extends BaseYinYangEffect {

    /** NBT root key for harmonized data */
    private static final String NBT_ROOT = "yygm_gates";

    /** NBT keys for Harmonized-specific data */
    private static final String NBT_YANG_GATE = "yang_gate";
    private static final String NBT_YIN_GATE = "yin_gate";
    private static final String NBT_GATE_COOLDOWN = "gate_cooldown";
    private static final String NBT_YANG_RESPAWN = "yang_respawn";
    private static final String NBT_YIN_RESPAWN = "yin_respawn";
    private static final String NBT_FIRST_APPLY_TIME = "first_apply_time";
    private static final String NBT_USED_SLOTS_BITMAP = "used_slots_bitmap";

    /** Gate types */
    public static final int GATE_YANG = 0;  // Gold gate
    public static final int GATE_YIN = 1;   // Silver gate
    public static final int GATE_NONE = -1; // No gate

    /** Compass directions: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW */
    public static final int NUM_DIRECTIONS = 8;

    /** Bitmap value when all 8 slots have been used */
    public static final int ALL_SLOTS_USED = 0xFF;

    /** Smart AoE target selection - damage caching (unique to Harmonized) */
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
        super(MobEffectCategory.HARMFUL, 0xFFFFD700, YinYangState.HARMONIZED, NBT_ROOT);
    }

    @Override
    protected CompoundTag initializePlayerData(CompoundTag tag) {
        tag.putInt(NBT_YANG_GATE, GATE_NONE);
        tag.putInt(NBT_YIN_GATE, GATE_NONE);
        tag.putLong(NBT_GATE_COOLDOWN, 0);
        tag.putLong(NBT_YANG_RESPAWN, 0);
        tag.putLong(NBT_YIN_RESPAWN, 0);
        tag.putLong(NBT_FIRST_APPLY_TIME, 0);
        tag.putInt(NBT_USED_SLOTS_BITMAP, 0);
        return tag;
    }

    // Note: BaseYinYangEffect.applyToTarget takes (target, playerUuid, durationTicks)
    // but HarmonizedEffect ignores duration and uses fixed 200 ticks (10 seconds)
    @Override
    public void applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks) {
        applyToTargetInternal(target, playerUuid);
    }

    /**
     * Initialize or refresh the Harmonized effect for a YYGM player.
     * Spawns dual gates (Yang + Yin) at different compass directions.
     * Does NOT switch targets if already harmonized.
     * Blocked if player has an active Yin Yang Annihilation target.
     *
     * @return true if this was a new application, false otherwise
     */
    public static boolean applyToTarget(LivingEntity target, UUID playerUuid) {
        return applyToTargetInternal(target, playerUuid);
    }

    private static boolean applyToTargetInternal(LivingEntity target, UUID playerUuid) {
        boolean targetChanged = false;
        if (!target.level().isClientSide) {
            // Check if player has an active Yin Yang Annihilation target
            if (YinYangAnnihilationEffect.hasAnnihilationTarget(playerUuid)) {
                TalentsMod.LOGGER.debug("YYGM player {} has active Yin Yang Annihilation target, Harmonized application blocked",
                    playerUuid);
                return false;
            }

            // Check if player already has a harmonized target
            Integer currentHarmonizedId = PlayerTargetTracker.getEntityId(playerUuid);

            // If trying to harmonize the same target, just refresh
            if (currentHarmonizedId != null && currentHarmonizedId == target.getId()) {
                targetChanged = false;
            } else if (currentHarmonizedId != null) {
                // Different target - don't switch
                targetChanged = false;
            } else {
                // No current target - set this one via PlayerTargetTracker
                PlayerTargetTracker.setTarget(playerUuid, YinYangState.HARMONIZED, target.getId());
                targetChanged = true;
            }
        }

        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(target, playerUuid);
        long currentTime = target.level().getGameTime();
        long firstApplyTime = playerData.getLong(NBT_FIRST_APPLY_TIME);
        boolean isNew = false;

        if (targetChanged || firstApplyTime == 0) {
            // New application - set initial cooldown and spawn gates
            playerData.putLong(NBT_GATE_COOLDOWN, currentTime + 20); // 1 second cooldown
            playerData.putLong(NBT_FIRST_APPLY_TIME, currentTime);

            // Spawn dual gates at different compass directions using GateSpawnStrategy
            int usedBitmap = playerData.getInt(NBT_USED_SLOTS_BITMAP);
            if (usedBitmap == ALL_SLOTS_USED) {
                usedBitmap = 0;
            }
            GateSpawnStrategy.GateSpawnResult result = GateSpawnStrategy.spawnDualGates(
                target, playerUuid, target.getRandom(), usedBitmap);

            playerData.putInt(NBT_YANG_GATE, result.yangDirection());
            playerData.putInt(NBT_YIN_GATE, result.yinDirection());
            playerData.putInt(NBT_USED_SLOTS_BITMAP, result.newUsedSlotsBitmap());
            effect.savePlayerData(target, playerUuid, playerData);

            isNew = true;
        }

        // Refresh the effect duration (fixed 200 ticks / 10 seconds)
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            YinYangEffects.HARMONIZED.get(),
            200,
            0,
            false,
            false  // Hide particles
        ));

        if (isNew) {
            syncGateState(target, playerUuid);
        }

        return isNew;
    }

    /**
     * Remove the Harmonized effect from a target entity.
     */
    @Override
    public void removeFromTarget(LivingEntity entity, UUID playerUuid) {
        // Remove mob effect
        entity.removeEffect(YinYangEffects.HARMONIZED.get());

        // Cleanup NBT data
        cleanupPlayerData(entity, playerUuid);

        // Clear player tracking if this was their target
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.entityId() == entity.getId() && data.state() == YinYangState.HARMONIZED) {
            PlayerTargetTracker.clearTarget(playerUuid);
        }

        // Cleanup damage cache
        DAMAGE_CACHE.remove(playerUuid);
        DAMAGE_HAS_DATA.remove(playerUuid);
    }

    // ===== Static API Methods (for backward compatibility) =====

    /**
     * Get the entity ID of the player's current harmonized target.
     */
    public static Integer getHarmonizedEntityId(UUID playerUuid) {
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.state() == YinYangState.HARMONIZED) {
            return data.entityId();
        }
        return null;
    }

    /**
     * Clear the player's harmonized tracking.
     */
    public static void clearHarmonizedTracking(UUID playerUuid) {
        PlayerTargetTracker.clearTarget(playerUuid);
        DAMAGE_CACHE.remove(playerUuid);
        DAMAGE_HAS_DATA.remove(playerUuid);
    }

    // ===== Gate Data Getters/Setters =====

    public static int getYangGateDirection(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getInt(NBT_YANG_GATE);
    }

    public static int getYinGateDirection(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getInt(NBT_YIN_GATE);
    }

    public static void setYangGateDirection(LivingEntity entity, UUID playerUuid, int direction) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putInt(NBT_YANG_GATE, direction);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    public static void setYinGateDirection(LivingEntity entity, UUID playerUuid, int direction) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putInt(NBT_YIN_GATE, direction);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    public static long getGateCooldown(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getLong(NBT_GATE_COOLDOWN);
    }

    public static void setGateCooldown(LivingEntity entity, UUID playerUuid, long cooldownEnd) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putLong(NBT_GATE_COOLDOWN, cooldownEnd);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    public static long getYangRespawnTick(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getLong(NBT_YANG_RESPAWN);
    }

    public static void setYangRespawnTick(LivingEntity entity, UUID playerUuid, long respawnTick) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putLong(NBT_YANG_RESPAWN, respawnTick);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    public static long getYinRespawnTick(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getLong(NBT_YIN_RESPAWN);
    }

    public static void setYinRespawnTick(LivingEntity entity, UUID playerUuid, long respawnTick) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putLong(NBT_YIN_RESPAWN, respawnTick);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    public static int getUsedSlotsBitmap(LivingEntity entity, UUID playerUuid) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getInt(NBT_USED_SLOTS_BITMAP);
    }

    public static void setUsedSlotsBitmap(LivingEntity entity, UUID playerUuid, int bitmap) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        playerData.putInt(NBT_USED_SLOTS_BITMAP, bitmap);
        effect.savePlayerData(entity, playerUuid, playerData);
    }

    /**
     * Check if this entity has any harmonized data from any player.
     */
    public static boolean hasAnyGates(LivingEntity entity) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        return effect.hasAnyData(entity);
    }

    /**
     * Get all players who have harmonized data on this entity.
     */
    public static java.util.Set<UUID> getGatePlayers(LivingEntity entity) {
        HarmonizedEffect effect = (HarmonizedEffect) YinYangEffects.HARMONIZED.get();
        return effect.getPlayersWithData(entity);
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
        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);
        long cooldownEnd = getGateCooldown(entity, playerUuid);
        long yangRespawn = getYangRespawnTick(entity, playerUuid);
        long yinRespawn = getYinRespawnTick(entity, playerUuid);
        int usedSlotsBitmap = getUsedSlotsBitmap(entity, playerUuid);

        PacketHandler.sendToNearby(
            new YinYangGateStateSyncPacket(
                entity.getId(), playerUuid, yangGate, yinGate, nextRequired,
                cooldownEnd, yangRespawn, yinRespawn, usedSlotsBitmap
            ),
            level, entity.position()
        );
    }

    // ===== Smart AoE Target Selection (unique to Harmonized) =====

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
     * Returns the entity ID of the closest enemy, or null if no candidates.
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
     */
    public static void handleEffectExpiration(LivingEntity entity, UUID playerUuid) {
        // Remove from player's harmonized tracking
        Integer cachedId = getHarmonizedEntityId(playerUuid);
        if (cachedId != null && cachedId == entity.getId()) {
            clearHarmonizedTracking(playerUuid);
        }

        // Cleanup entity NBT data
        BaseYinYangEffect.cleanupPlayerData(entity, playerUuid, NBT_ROOT);

        // Reset pending hits (tied to harmonized target)
        com.complextalents.impl.yygm.EquilibriumData.resetPendingHits(playerUuid);

        // Send removal sync to clients
        if (entity.level() instanceof ServerLevel level) {
            PacketHandler.sendToNearby(
                new YinYangGateStateSyncPacket(
                    entity.getId(), playerUuid, -2, 0, 0, 0, 0, 0
                ),
                level, entity.position()
            );
        }

        TalentsMod.LOGGER.debug("YYGM Harmonized effect expired for player {} on entity {}",
            playerUuid, entity.getName().getString());
    }

    /**
     * Static cleanup method for static context calls.
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid) {
        BaseYinYangEffect.cleanupPlayerData(entity, playerUuid, NBT_ROOT);
    }
}
