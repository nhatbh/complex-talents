package com.complextalents.impl.yygm.state;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.effect.ExposedEffect;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.effect.YinYangAnnihilationEffect;
import com.complextalents.impl.yygm.effect.YinYangEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Centralized state management for the YYGM system.
 * <p>
 * This class consolidates all state transitions and provides a single source of truth
 * for YYGM state management. It replaces the scattered transition logic that was
 * previously in ExposedEffect, YinYangAnnihilationEffect, and YinYangDamageHandler.
 * </p>
 */
public final class YinYangStateManager {

    private YinYangStateManager() {
        // Utility class - prevent instantiation
    }

    /**
     * Transition a target entity to a new state for a player.
     * Handles all cleanup, effect removal, and state validation.
     *
     * @param target The target entity
     * @param playerUuid The YYGM player's UUID
     * @param newState The state to transition to
     * @param durationTicks Duration in ticks for the new state
     * @return true if transition succeeded, false if invalid
     */
    public static boolean transitionState(LivingEntity target, UUID playerUuid,
                                         YinYangState newState, int durationTicks) {
        YinYangState currentState = getStateForTarget(target, playerUuid);

        // Validate transition
        if (currentState != null && !currentState.canTransitionTo(newState)) {
            TalentsMod.LOGGER.warn("Invalid YYGM state transition: {} -> {} for player {} on entity {}",
                currentState, newState, playerUuid, target.getName().getString());
            return false;
        }

        // Clear current state if any
        if (currentState != null) {
            clearState(target, playerUuid, currentState);
        }

        // Apply new state
        boolean success = switch (newState) {
            case HARMONIZED -> applyHarmonized(target, playerUuid, durationTicks);
            case EXPOSED -> applyExposed(target, playerUuid, durationTicks);
            case ANNIHILATION -> applyAnnihilation(target, playerUuid, durationTicks);
        };

        if (success) {
            TalentsMod.LOGGER.debug("YYGM state transition successful: player {} on entity {} -> {}",
                playerUuid, target.getName().getString(), newState);
        }

        return success;
    }

    /**
     * Clear all YYGM effects from a target for a player.
     *
     * @param target The target entity
     * @param playerUuid The player's UUID
     */
    public static void clearState(LivingEntity target, UUID playerUuid) {
        YinYangState currentState = getStateForTarget(target, playerUuid);
        if (currentState != null) {
            clearState(target, playerUuid, currentState);
        }
    }

    /**
     * Clear all YYGM effects from a target for a player (with known state).
     *
     * @param target The target entity
     * @param playerUuid The player's UUID
     * @param state The current state to clear
     */
    public static void clearState(LivingEntity target, UUID playerUuid, YinYangState state) {
        // Remove the mob effect
        target.removeEffect(switch (state) {
            case HARMONIZED -> YinYangEffects.HARMONIZED.get();
            case EXPOSED -> YinYangEffects.EXPOSED.get();
            case ANNIHILATION -> YinYangEffects.YIN_YANG_ANNIHILATION.get();
        });

        // Cleanup data based on state
        switch (state) {
            case HARMONIZED -> {
                HarmonizedEffect.cleanupPlayerData(target, playerUuid);
                sendRemovalPacket(target, playerUuid, YinYangState.HARMONIZED);
            }
            case EXPOSED -> {
                ExposedEffect.cleanupPlayerData(target, playerUuid);
                sendRemovalPacket(target, playerUuid, YinYangState.EXPOSED);
            }
            case ANNIHILATION -> {
                YinYangAnnihilationEffect.cleanupPlayerData(target, playerUuid);
                // No packet needed for Annihilation removal
            }
        }

        // Clear player tracking
        PlayerTargetTracker.clearTarget(playerUuid);

        TalentsMod.LOGGER.debug("YYGM state cleared: player {} on entity {} (was {})",
            playerUuid, target.getName().getString(), state);
    }

    /**
     * Get the current state of a target for a player.
     *
     * @param target The target entity
     * @param playerUuid The player's UUID
     * @return The current state, or null if no YYGM effect is active
     */
    public static YinYangState getStateForTarget(LivingEntity target, UUID playerUuid) {
        if (target.hasEffect(YinYangEffects.HARMONIZED.get())) {
            return YinYangState.HARMONIZED;
        }
        if (target.hasEffect(YinYangEffects.EXPOSED.get())) {
            return YinYangState.EXPOSED;
        }
        if (target.hasEffect(YinYangEffects.YIN_YANG_ANNIHILATION.get())) {
            return YinYangState.ANNIHILATION;
        }
        return null;
    }

    /**
     * Check if a transition is valid.
     *
     * @param target The target entity
     * @param playerUuid The player's UUID
     * @param newState The desired new state
     * @return true if transition is valid
     */
    public static boolean canTransition(LivingEntity target, UUID playerUuid, YinYangState newState) {
        YinYangState currentState = getStateForTarget(target, playerUuid);
        return currentState == null || currentState.canTransitionTo(newState);
    }

    /**
     * Server tick handler for cleaning up missing entities.
     * Minecraft's effect system handles expiration via MobEffectEvent.OnEffectRemove.
     * This only handles the edge case of entities being unloaded/dead.
     *
     * @param server The Minecraft server
     */
    public static void onServerTick(MinecraftServer server) {
        for (var entry : PlayerTargetTracker.getAllEntries()) {
            UUID playerUuid = entry.getKey();
            PlayerTargetTracker.TargetData targetData = entry.getValue();

            // Find the entity in any loaded level
            LivingEntity entity = PlayerTargetTracker.findEntity(server, targetData.entityId());

            if (entity == null) {
                // Entity not found or dead - clear the tracking
                PlayerTargetTracker.clearTarget(playerUuid);
                TalentsMod.LOGGER.debug("YYGM entity {} not found for player {}, clearing target",
                    targetData.entityId(), playerUuid);
            }
        }
    }

    // ===== Private State Application Methods =====

    private static boolean applyHarmonized(LivingEntity target, UUID playerUuid, int durationTicks) {
        // Check if player has an active Annihilation target (block Harmonized)
        if (PlayerTargetTracker.hasTargetInState(playerUuid, YinYangState.ANNIHILATION)) {
            TalentsMod.LOGGER.debug("YYGM player {} has active Annihilation target, Harmonized blocked", playerUuid);
            return false;
        }

        // Set tracking before applying effect
        PlayerTargetTracker.setTarget(playerUuid, YinYangState.HARMONIZED, target.getId());

        // Note: HarmonizedEffect.applyToTarget takes only (target, playerUuid)
        // Duration is handled internally by the effect (200 ticks / 10 seconds)
        boolean success = HarmonizedEffect.applyToTarget(target, playerUuid);
        if (success) {
            HarmonizedEffect.syncGateState(target, playerUuid);
        } else {
            // Rollback tracking if effect application failed
            PlayerTargetTracker.clearTarget(playerUuid);
        }
        return success;
    }

    private static boolean applyExposed(LivingEntity target, UUID playerUuid, int durationTicks) {
        // Set tracking before applying effect
        PlayerTargetTracker.setTarget(playerUuid, YinYangState.EXPOSED, target.getId());

        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        effect.applyToTarget(target, playerUuid, durationTicks);
        ExposedEffect.syncExposedState(target, playerUuid);
        return true;
    }

    private static boolean applyAnnihilation(LivingEntity target, UUID playerUuid, int durationTicks) {
        // Set tracking before applying effect
        PlayerTargetTracker.setTarget(playerUuid, YinYangState.ANNIHILATION, target.getId());

        YinYangAnnihilationEffect effect = (YinYangAnnihilationEffect) YinYangEffects.YIN_YANG_ANNIHILATION.get();
        effect.applyToTarget(target, playerUuid, durationTicks);
        return true;
    }

    // ===== Private Helper Methods =====

    private static void sendRemovalPacket(LivingEntity entity, UUID playerUuid, YinYangState state) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        // Send the appropriate removal packet based on state
        switch (state) {
            case HARMONIZED -> {
                PacketHandler.sendToNearby(
                    new YinYangGateStateSyncPacket(
                        entity.getId(), playerUuid, -2, 0, 0, 0, 0, 0, 0
                    ),
                    level, entity.position()
                );
            }
            case EXPOSED -> {
                PacketHandler.sendToNearby(
                    new ExposedStateSyncPacket(entity.getId(), playerUuid, 0, 0, 0, 0),
                    level, entity.position()
                );
            }
            case ANNIHILATION -> {
                // No packet needed for Annihilation removal
            }
        }
    }
}
