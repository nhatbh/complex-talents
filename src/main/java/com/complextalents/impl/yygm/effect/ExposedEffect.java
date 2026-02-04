package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.state.PlayerTargetTracker;
import com.complextalents.impl.yygm.state.YinYangState;
import com.complextalents.impl.yygm.util.GateSpawnStrategy;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

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
 * </p>
 * <p>
 * Refactored to extend BaseYinYangEffect and use unified PlayerTargetTracker.
 * </p>
 */
public class ExposedEffect extends BaseYinYangEffect {

    /** NBT root key for exposed data */
    private static final String NBT_ROOT = "yygm_exposed";

    /** NBT key for gate pattern bitmap (8-bit: 1=Yang, 0=Yin per direction) */
    private static final String NBT_GATE_PATTERN = "gate_pattern";

    /** NBT key for completed gates bitmap */
    private static final String NBT_COMPLETED_GATES = "completed_gates";

    /** Compass directions: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW */
    public static final int NUM_DIRECTIONS = 8;

    /** Gate types */
    public static final int GATE_YANG = 0;  // Gold gate
    public static final int GATE_YIN = 1;   // Silver gate
    public static final int GATE_NONE = -1; // No gate

    public ExposedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4444, YinYangState.EXPOSED, NBT_ROOT);
    }

    @Override
    protected CompoundTag initializePlayerData(CompoundTag tag) {
        tag.putInt(NBT_GATE_PATTERN, 0);
        tag.putInt(NBT_COMPLETED_GATES, 0);
        return tag;
    }

    /**
     * Apply the Exposed effect to a target entity.
     * Removes Harmonized effect if present on this target.
     *
     * @param target The target entity
     * @param playerUuid The YYGM player's UUID
     * @param durationTicks Duration in ticks
     */
    @Override
    public void applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks) {
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

                TalentsMod.LOGGER.debug("Removed Harmonized effect from target {} before applying Exposed",
                    target.getName().getString());
            }

            // Set player's exposed target via PlayerTargetTracker
            PlayerTargetTracker.setTarget(playerUuid, YinYangState.EXPOSED, target.getId());
        }

        // Generate random gate pattern using GateSpawnStrategy
        int gatePattern = GateSpawnStrategy.generateEightGatePattern(target.getRandom());

        CompoundTag playerData = getOrCreatePlayerData(target, playerUuid);
        playerData.putInt(NBT_GATE_PATTERN, gatePattern);
        playerData.putInt(NBT_COMPLETED_GATES, 0);
        savePlayerData(target, playerUuid, playerData);

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
    }

    /**
     * Remove the Exposed effect from a target entity.
     */
    @Override
    public void removeFromTarget(LivingEntity entity, UUID playerUuid) {
        // Remove mob effect
        entity.removeEffect(YinYangEffects.EXPOSED.get());

        // Cleanup NBT data
        cleanupPlayerData(entity, playerUuid);

        // Clear player tracking if this was their target
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.entityId() == entity.getId()) {
            PlayerTargetTracker.clearTarget(playerUuid);
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

    // ===== Static API Methods (for backward compatibility) =====

    /**
     * Check if a target is Exposed by a specific player.
     * Relies on Minecraft's effect system for duration tracking.
     */
    public static boolean isExposed(LivingEntity entity, UUID playerUuid) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag rootTag = entity.getPersistentData().getCompound(effect.getNbtRoot());
        return rootTag.contains(playerUuid.toString()) && entity.hasEffect(YinYangEffects.EXPOSED.get());
    }

    /**
     * Get the entity ID of the player's current Exposed target.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static Integer getExposedEntityId(UUID playerUuid) {
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.state() == YinYangState.EXPOSED) {
            return data.entityId();
        }
        return null;
    }

    /**
     * Set the player's Exposed target.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static void setExposedTarget(UUID playerUuid, int entityId) {
        PlayerTargetTracker.setTarget(playerUuid, YinYangState.EXPOSED, entityId);
    }

    /**
     * Clear the player's Exposed target tracking.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static void clearExposedTarget(UUID playerUuid) {
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.state() == YinYangState.EXPOSED) {
            PlayerTargetTracker.clearTarget(playerUuid);
        }
    }

    /**
     * Check if the player has an active Exposed target.
     */
    public static boolean hasExposedTarget(UUID playerUuid) {
        return PlayerTargetTracker.hasTargetInState(playerUuid, YinYangState.EXPOSED);
    }

    /**
     * Get the gate pattern bitmap for a player on this entity.
     */
    public static int getGatePattern(LivingEntity entity, UUID playerUuid) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getInt(NBT_GATE_PATTERN);
    }

    /**
     * Get the gate type at a specific compass direction.
     * @return GATE_YANG or GATE_YIN
     */
    public static int getGateTypeAtDirection(LivingEntity entity, UUID playerUuid, int direction) {
        int pattern = getGatePattern(entity, playerUuid);
        return GateSpawnStrategy.getGateTypeFromPattern(pattern, direction);
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
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        int completedBitmap = playerData.getInt(NBT_COMPLETED_GATES);
        int bit = 1 << compassDirection;

        if ((completedBitmap & bit) != 0) {
            return false; // Already completed
        }

        // Mark as completed
        completedBitmap |= bit;
        playerData.putInt(NBT_COMPLETED_GATES, completedBitmap);
        effect.savePlayerData(entity, playerUuid, playerData);

        return true;
    }

    /**
     * Get the count of completed gates (0-8).
     */
    public static int getCompletedGateCount(LivingEntity entity, UUID playerUuid) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
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
     * Uses Minecraft's effect system for duration tracking.
     */
    public static int getRemainingDurationStatic(LivingEntity entity, UUID playerUuid) {
        net.minecraft.world.effect.MobEffectInstance effectInstance = entity.getEffect(YinYangEffects.EXPOSED.get());
        return effectInstance != null ? effectInstance.getDuration() : 0;
    }

    /**
     * Get the completed gates bitmap.
     */
    public static int getCompletedGatesBitmap(LivingEntity entity, UUID playerUuid) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);
        return playerData.getInt(NBT_COMPLETED_GATES);
    }

    /**
     * Check if this entity has any Exposed data from any player.
     */
    public static boolean hasAnyExposed(LivingEntity entity) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        return effect.hasAnyData(entity);
    }

    /**
     * Get all players who have Exposed data on this entity.
     */
    public static java.util.Set<UUID> getExposedPlayers(LivingEntity entity) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        return effect.getPlayersWithData(entity);
    }

    /**
     * Sync Exposed state to all nearby players.
     * Uses Minecraft's effect system for duration tracking.
     */
    public static void syncExposedState(LivingEntity entity, UUID playerUuid) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        CompoundTag playerData = effect.getOrCreatePlayerData(entity, playerUuid);

        int gatePattern = playerData.getInt(NBT_GATE_PATTERN);
        int completedGates = playerData.getInt(NBT_COMPLETED_GATES);
        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);

        // Get duration from Minecraft's effect system
        net.minecraft.world.effect.MobEffectInstance effectInstance = entity.getEffect(YinYangEffects.EXPOSED.get());
        int remainingDuration = effectInstance != null ? effectInstance.getDuration() : 0;
        long expirationTick = entity.level().getGameTime() + remainingDuration;

        ExposedStateSyncPacket packet = new ExposedStateSyncPacket(
            entity.getId(), playerUuid, gatePattern, completedGates, nextRequired, expirationTick
        );

        PacketHandler.sendToNearby(packet, level, entity.position());
    }

    /**
     * Static cleanup method for static context calls.
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid) {
        BaseYinYangEffect.cleanupPlayerData(entity, playerUuid, NBT_ROOT);
    }

    /**
     * Static wrapper for applyToTarget.
     */
    public static void applyToTargetStatic(LivingEntity target, UUID playerUuid, int durationTicks) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        effect.applyToTarget(target, playerUuid, durationTicks);
    }

    /**
     * Static wrapper for removeFromTarget.
     */
    public static void removeFromTargetStatic(LivingEntity entity, UUID playerUuid) {
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        effect.removeFromTarget(entity, playerUuid);
    }
}
