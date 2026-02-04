package com.complextalents.impl.yygm.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.EquilibriumData;
import com.complextalents.impl.yygm.effect.ExposedEffect;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.effect.YinYangAnnihilationEffect;
import com.complextalents.impl.yygm.effect.YinYangEffects;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import com.complextalents.network.yygm.SpawnYinYangGateFXPacket;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import com.complextalents.origin.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Event handlers for Yin Yang Grandmaster origin.
 * Handles damage calculation and fires YYGMGateHitEvent.
 * Gate combat result processing is done in YinYangGrandmasterOrigin.GateCombatEvents.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class YinYangDamageHandler {

    /**
     * Handle melee damage dealt by YYGM players.
     * - Caches damage for Smart AoE target selection (processed in YYGMHarmonizationHandler)
     * - Calculates gate hit result when hitting harmonized target
     * - Fires YYGMGateHitEvent with pre-calculated data
     * - Applies body hit penalty for non-harmonized hits
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!YinYangGrandmasterOrigin.isYinYangGrandmaster(player)) {
            return;
        }

        // Only direct melee damage triggers gate combat
        if (event.getSource().isIndirect()) {
            return;
        }

        LivingEntity target = event.getEntity();

        // Cache damage for Smart AoE Target Selection (processed in tick event by YYGMHarmonizationHandler)
        double distance = player.position().distanceTo(target.position());
        HarmonizedEffect.cacheDamage(player.getUUID(), target.getId(), distance);

        // CASE -1: Hitting Yin Yang Annihilation target - HIGHEST PRIORITY
        // All attacks deal amplified true damage from ANY angle - no gate restrictions
        Integer annihilationId = YinYangAnnihilationEffect.getAnnihilationEntityId(player.getUUID());
        if (annihilationId != null && annihilationId == target.getId()) {
            applyAnnihilationTrueDamage(event, player);

            TalentsMod.LOGGER.debug("YYGM {} hitting Yin Yang Annihilation target {}, applying amplified true damage",
                player.getName().getString(), target.getName().getString());
            return;
        }

        // CASE 0: Hitting Exposed target - handle Exposed gate logic
        Integer exposedId = ExposedEffect.getExposedEntityId(player.getUUID());
        if (exposedId != null && exposedId == target.getId()) {
            handleExposedGateHit(event, target, player);
            return;
        }

        // Get current harmonized target
        Integer harmonizedId = HarmonizedEffect.getHarmonizedEntityId(player.getUUID());

        // CASE 1: Hitting the current harmonized entity - calculate and fire event
        if (harmonizedId != null && harmonizedId == target.getId()) {
            TalentsMod.LOGGER.debug("YYGM {} hitting harmonized entity {}, calculating gate hit",
                player.getName().getString(), target.getName().getString());

            YYGMGateHitEvent gateHitEvent = calculateGateHitResult(event, target, player);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(gateHitEvent);

            // Apply damage based on hit result
            switch (gateHitEvent.getHitResult()) {
                case TRUE_GATE:
                    // Apply true damage with equilibrium bonus
                    applyTrueDamage(event, player, target);
                    break;
                case FALSE_GATE:
                    // Wrong gate = ZERO damage
                    event.setAmount(0);
                    break;
                case EMPTY_GATE:
                    // Body hit penalty
                    applyBodyHitPenalty(event, player);
                    break;
            }
            return;
        }

        // CASE 2: All other cases - apply body hit penalty
        // (No harmonized target, hitting different target, etc.)
        TalentsMod.LOGGER.debug("YYGM {} hitting non-harmonized entity {}, applying body hit penalty (harmonizedId: {})",
            player.getName().getString(), target.getName().getString(), harmonizedId);
        applyBodyHitPenalty(event, player);
    }

    /**
     * Calculate the gate hit result (angle, direction, gate type, hit result).
     * This does NOT modify game state - only calculates and returns event data.
     */
    private static YYGMGateHitEvent calculateGateHitResult(LivingDamageEvent event, LivingEntity target, ServerPlayer player) {
        // Calculate attack angle
        double attackAngle = HarmonizedEffect.calculateAttackAngle(target, player);
        int compassDirection = HarmonizedEffect.angleToCompassDirection(attackAngle);

        // Get gate data
        int yangGate = HarmonizedEffect.getYangGateDirection(target, player.getUUID());
        int yinGate = HarmonizedEffect.getYinGateDirection(target, player.getUUID());
        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(player.getUUID());
        long currentTime = target.level().getGameTime();

        // Check cooldown
        long cooldownEnd = HarmonizedEffect.getGateCooldown(target, player.getUUID());
        if (currentTime < cooldownEnd) {
            // In cooldown - treat as empty gate hit
            return new YYGMGateHitEvent(player, target,
                YYGMGateHitEvent.HitResult.EMPTY_GATE,
                YYGMGateHitEvent.GATE_NONE,
                compassDirection, attackAngle, nextRequired);
        }

        // Determine which gate was hit (if any)
        int hitGateType = YYGMGateHitEvent.GATE_NONE;
        if (compassDirection == yangGate && yangGate != HarmonizedEffect.GATE_NONE) {
            hitGateType = YYGMGateHitEvent.GATE_YANG;
        } else if (compassDirection == yinGate && yinGate != HarmonizedEffect.GATE_NONE) {
            hitGateType = YYGMGateHitEvent.GATE_YIN;
        }

        // Determine hit result
        YYGMGateHitEvent.HitResult hitResult;
        if (hitGateType == YYGMGateHitEvent.GATE_NONE) {
            hitResult = YYGMGateHitEvent.HitResult.EMPTY_GATE;
        } else if (hitGateType == nextRequired) {
            hitResult = YYGMGateHitEvent.HitResult.TRUE_GATE;
        } else {
            hitResult = YYGMGateHitEvent.HitResult.FALSE_GATE;
        }

        return new YYGMGateHitEvent(player, target, hitResult, hitGateType,
            compassDirection, attackAngle, nextRequired);
    }

    /**
     * Handle Exposed gate hit logic for Eight Formation Battle Array Ultimate.
     * All 8 gates are active - each direction has a specific Yin or Yang gate.
     * Player must hit gates in alternating Yin/Yang order (like Harmonized).
     * Already completed gates are treated as empty gates (body hit penalty).
     * Wrong gate type clears Exposed immediately.
     */
    private static void handleExposedGateHit(LivingDamageEvent event, LivingEntity target, ServerPlayer player) {
        // Calculate attack angle and compass direction
        double attackAngle = ExposedEffect.calculateAttackAngle(target, player);
        int compassDirection = ExposedEffect.angleToCompassDirection(attackAngle);

        // CHECK 1: Is this gate already completed?
        int completedBitmap = ExposedEffect.getCompletedGatesBitmap(target, player.getUUID());
        boolean isGateCompleted = (completedBitmap & (1 << compassDirection)) != 0;

        if (isGateCompleted) {
            // Already completed gate - treat as empty gate (body hit penalty)
            applyBodyHitPenalty(event, player);

            TalentsMod.LOGGER.debug("YYGM {} hit already completed Exposed gate at direction {}, applying body hit penalty",
                player.getName().getString(), compassDirection);
            return;
        }

        // CHECK 2: Get gate type at the hit direction and next required from global EquilibriumData
        int targetGateType = ExposedEffect.getGateTypeAtDirection(target, player.getUUID(), compassDirection);
        int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(player.getUUID());

        // CHECK 3: Is this a correct gate hit? (gate type must match next required)
        boolean isCorrect = (targetGateType == nextRequired);

        if (isCorrect) {
            // Mark gate as completed
            boolean wasCompleted = ExposedEffect.markGateCompleted(target, player.getUUID(), compassDirection);

            if (wasCompleted) {
                // First time hitting this gate
                int completedCount = ExposedEffect.getCompletedGateCount(target, player.getUUID());

                // Apply Yin Yang Annihilation: True damage from any angle
                applyYinYangAnnihilation(event, player);

                // FX for gate completion
                if (player.level() instanceof ServerLevel level) {
                    PacketHandler.sendToNearby(
                        new SpawnYinYangGateFXPacket(target.position(), SpawnYinYangGateFXPacket.EffectType.GATE_SPAWN),
                        level, target.position()
                    );
                }

                // Check if all 8 gates completed
                if (completedCount >= 8) {
                    convertToYinYangAnnihilation(target, player);
                }
            }

            // Toggle next required gate (same as Harmonized) - now player-global in EquilibriumData
            int newNextRequired = (targetGateType == ExposedEffect.GATE_YANG)
                ? YYGMGateHitEvent.GATE_YIN
                : YYGMGateHitEvent.GATE_YANG;
            com.complextalents.impl.yygm.EquilibriumData.setNextRequired(player.getUUID(), newNextRequired);

            // Sync state (includes updated nextRequired)
            ExposedEffect.syncExposedState(target, player.getUUID());

            TalentsMod.LOGGER.debug("YYGM {} hit correct Exposed gate at direction {}, target gate: {}, next was: {}, next now: {}, completed count: {}",
                player.getName().getString(), compassDirection, targetGateType, nextRequired,
                newNextRequired == YYGMGateHitEvent.GATE_YANG ? "Yang" : "Yin",
                ExposedEffect.getCompletedGateCount(target, player.getUUID()));
        } else {
            // WRONG gate hit - clear Exposed immediately
            ExposedEffect.removeFromTarget(target, player.getUUID());
            ExposedEffect.clearExposedTarget(player.getUUID());

            // Apply body hit penalty
            applyBodyHitPenalty(event, player);

            // FX for failure
            if (player.level() instanceof ServerLevel level) {
                PacketHandler.sendToNearby(
                    new SpawnYinYangGateFXPacket(target.position(), SpawnYinYangGateFXPacket.EffectType.DISCORD),
                    level, target.position()
                );
            }

            TalentsMod.LOGGER.debug("YYGM {} hit wrong Exposed gate at direction {}, target gate: {}, next required: {}, effect cleared",
                player.getName().getString(), compassDirection, targetGateType,
                nextRequired == YYGMGateHitEvent.GATE_YANG ? "Yang" : "Yin");
        }
    }

    /**
     * Get the player's current attack type (Yin or Yang) based on pending hit state.
     * Returns the type the player would hit with next attack.
     * If player has no pending state, defaults to alternating (Yin -> Yang -> Yin...)
     */
    private static int getPlayerAttackType(UUID playerUuid) {
        boolean hasPendingYin = EquilibriumData.hasPendingYinHit(playerUuid);
        boolean hasPendingYang = EquilibriumData.hasPendingYangHit(playerUuid);

        // Determine which gate type player needs to hit next
        if (hasPendingYin && !hasPendingYang) {
            // Has Yin pending, needs Yang to complete the pair
            return HarmonizedEffect.GATE_YANG;
        } else if (hasPendingYang && !hasPendingYin) {
            // Has Yang pending, needs Yin to complete the pair
            return HarmonizedEffect.GATE_YIN;
        } else if (!hasPendingYin && !hasPendingYang) {
            // No pending hits - check equilibrium parity to determine next
            // Even equilibrium (0, 2, 4, 6, 8) = Yang first, Odd (1, 3, 5, 7) = Yin first
            int equilibrium = EquilibriumData.getEquilibrium(playerUuid);
            return (equilibrium % 2 == 0) ? HarmonizedEffect.GATE_YANG : HarmonizedEffect.GATE_YIN;
        } else {
            // Both pending (shouldn't happen normally) - default to Yang
            return HarmonizedEffect.GATE_YANG;
        }
    }

    /**
     * Apply amplified true damage for Yin Yang Annihilation targets.
     * Called when hitting an entity that has Yin Yang Annihilation effect.
     * All attacks from any angle deal true damage with equilibrium bonus.
     * Formula: originalDamage * (1 + (trueDamageMultiplier - 1) + equilibrium * equilibriumBonusPercent)
     */
    private static void applyAnnihilationTrueDamage(LivingDamageEvent event, ServerPlayer player) {
        float originalDamage = event.getAmount();
        int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());

        // Yin Yang Annihilation: True damage from ANY angle
        // Uses same formula as normal true damage but without angle requirement
        double trueDamageMult = OriginManager.getOriginStat(player, "trueDamageMultiplier");
        double equilibriumBonusPercent = OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");
        float totalMultiplier = 1.0f + (float) (trueDamageMult - 1.0) + (float) (equilibrium * equilibriumBonusPercent);
        float trueDamage = originalDamage * totalMultiplier;
        event.setAmount(trueDamage);

        TalentsMod.LOGGER.debug("YYGM {} Annihilation True Damage: {} -> {} (eq: {}, mult: {})",
            player.getName().getString(), originalDamage, trueDamage, equilibrium, totalMultiplier);
    }

    /**
     * Apply Yin Yang Annihilation: True damage from ANY angle.
     * Uses the same formula as successful gate hits.
     */
    private static void applyYinYangAnnihilation(LivingDamageEvent event, ServerPlayer player) {
        float originalDamage = event.getAmount();
        int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());

        // Yin Yang Annihilation: True damage from ANY angle
        // Uses same formula as normal true damage but without angle requirement
        double trueDamageMult = OriginManager.getOriginStat(player, "trueDamageMultiplier");
        double equilibriumBonusPercent = OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");
        float totalMultiplier = 1.0f + (float) (trueDamageMult - 1.0) + (float) (equilibrium * equilibriumBonusPercent);
        float trueDamage = originalDamage * totalMultiplier;
        event.setAmount(trueDamage);

        TalentsMod.LOGGER.debug("YYGM {} Yin Yang Annihilation: {} -> {}",
            player.getName().getString(), originalDamage, trueDamage);
    }

    /**
     * Convert Exposed effect to Yin Yang Annihilation effect.
     * Called when all 8 gates are completed successfully.
     */
    private static void convertToYinYangAnnihilation(LivingEntity target, ServerPlayer player) {
        // Calculate remaining duration
        int remainingDuration = ExposedEffect.getRemainingDuration(target, player.getUUID());

        // Remove Exposed
        ExposedEffect.removeFromTarget(target, player.getUUID());
        ExposedEffect.clearExposedTarget(player.getUUID());

        // Apply Yin Yang Annihilation with remaining duration
        YinYangAnnihilationEffect.applyToTarget(target, player.getUUID(), remainingDuration);

        // FX for completion
        if (player.level() instanceof ServerLevel level) {
            PacketHandler.sendToNearby(
                new SpawnYinYangGateFXPacket(target.position(), SpawnYinYangGateFXPacket.EffectType.YANG_HIT),
                level, target.position()
            );
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, player.getSoundSource(), 1.0f, 1.0f);
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("All gates broken! Yin Yang Annihilation activated!"));

        TalentsMod.LOGGER.debug("YYGM {} completed all Exposed gates! Converted to Yin Yang Annihilation, duration: {} ticks",
            player.getName().getString(), remainingDuration);
    }

    /**
     * Apply true damage with equilibrium bonus.
     * Formula: originalDamage * (1 + (trueDamageMultiplier - 1) + equilibrium * equilibriumBonusPercent)
     * This makes trueDamageMultiplier and equilibriumBonus additive.
     */
    private static void applyTrueDamage(LivingDamageEvent event, ServerPlayer player, LivingEntity target) {
        float originalDamage = event.getAmount();

        // Get current Equilibrium from player-global data
        int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());

        // Apply true damage multiplier additively with equilibrium bonus
        // Total multiplier = 1 (base) + (trueDamageMultiplier - 1) + (equilibrium * equilibriumBonusPercent)
        double trueDamageMult = OriginManager.getOriginStat(player, "trueDamageMultiplier");
        double equilibriumBonusPercent = OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");
        float totalMultiplier = 1.0f + (float) (trueDamageMult - 1.0) + (float) (equilibrium * equilibriumBonusPercent);
        float trueDamage = originalDamage * totalMultiplier;
        event.setAmount(trueDamage);

        TalentsMod.LOGGER.debug("YYGM {} applying true damage: {} -> {} (eq: {}, mult: {})",
            player.getName().getString(), originalDamage, trueDamage, totalMultiplier);
    }

    /**
     * Apply body hit damage reduction.
     */
    private static void applyBodyHitPenalty(LivingDamageEvent event, ServerPlayer player) {
        float originalDamage = event.getAmount();
        double reduction = OriginManager.getOriginStat(player, "bodyHitDamageReduction");
        float reducedDamage = originalDamage * (1.0f - (float) reduction);
        event.setAmount(reducedDamage);
    }

    /**
     * Handle server tick for expired harmonized, exposed, and annihilation effect fail-safe checking.
     * Delegates to HarmonizedEffect, ExposedEffect, and YinYangAnnihilationEffect which use internal caches for efficient iteration.
     * Gate respawns and equilibrium decay are handled in YinYangGrandmasterOrigin.GateCombatEvents.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Check every 5 ticks for performance
        if (event.getServer().getTickCount() % 5 != 0) {
            return;
        }

        HarmonizedEffect.onServerTick(event.getServer());
        ExposedEffect.onServerTick(event.getServer());
        YinYangAnnihilationEffect.onServerTick(event.getServer());
    }

    /**
     * Sync all nearby YYGM gates to a player when they log in or change dimensions.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        HarmonizedEffect.clearHarmonizedTracking(player.getUUID());
        ExposedEffect.clearExposedTarget(player.getUUID());
        YinYangAnnihilationEffect.clearAnnihilationTarget(player.getUUID());
        EquilibriumData.cleanup(player.getUUID());
        syncAllNearbyGates(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        syncAllNearbyGates(player);
    }

    private static void syncAllNearbyGates(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double searchRange = 128.0;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                    net.minecraft.world.phys.Vec3.ZERO,
                    new net.minecraft.world.phys.Vec3(searchRange, 256.0, searchRange)
                ).inflate(searchRange).move(player.position()))) {

            // Sync Harmonized gates
            if (HarmonizedEffect.hasAnyGates(entity)) {
                for (UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                    int yangGate = HarmonizedEffect.getYangGateDirection(entity, playerUuid);
                    int yinGate = HarmonizedEffect.getYinGateDirection(entity, playerUuid);
                    // nextRequired is now player-global in EquilibriumData
                    int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);
                    long cooldownEnd = HarmonizedEffect.getGateCooldown(entity, playerUuid);
                    long yangRespawn = HarmonizedEffect.getYangRespawnTick(entity, playerUuid);
                    long yinRespawn = HarmonizedEffect.getYinRespawnTick(entity, playerUuid);
                    int usedSlotsBitmap = HarmonizedEffect.getUsedSlotsBitmap(entity, playerUuid);

                    PacketHandler.sendTo(new YinYangGateStateSyncPacket(
                        entity.getId(), playerUuid, yangGate, yinGate, nextRequired,
                        cooldownEnd, yangRespawn, yinRespawn, usedSlotsBitmap
                    ), player);
                }
            }

            // Sync Exposed gates (Eight Formation Battle Array Ultimate)
            if (ExposedEffect.hasAnyExposed(entity)) {
                for (UUID playerUuid : ExposedEffect.getExposedPlayers(entity)) {
                    int gatePattern = ExposedEffect.getGatePattern(entity, playerUuid);
                    int completedGates = ExposedEffect.getCompletedGatesBitmap(entity, playerUuid);
                    // nextRequired is now player-global in EquilibriumData
                    int nextRequired = com.complextalents.impl.yygm.EquilibriumData.getNextRequired(playerUuid);
                    int remainingDuration = ExposedEffect.getRemainingDuration(entity, playerUuid);
                    long currentTime = entity.level().getGameTime();
                    long expirationTick = currentTime + remainingDuration;

                    PacketHandler.sendTo(new ExposedStateSyncPacket(
                        entity.getId(), playerUuid, gatePattern, completedGates, nextRequired, expirationTick
                    ), player);
                }
            }
        }

        // Note: Equilibrium is player-global and synced via EquilibriumSyncPacket
        // It's already loaded from EquilibriumData, no need to sync from entity NBT
        // Same for nextRequired - also player-global in EquilibriumData

        TalentsMod.LOGGER.debug("Synced YYGM gates to player {} on login/dimension change",
            player.getName().getString());
    }

    /**
     * Clean up YYGM tracking when a player logs out.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Integer harmonizedEntityId = HarmonizedEffect.getHarmonizedEntityId(player.getUUID());
        Integer exposedEntityId = ExposedEffect.getExposedEntityId(player.getUUID());
        Integer annihilationEntityId = YinYangAnnihilationEffect.getAnnihilationEntityId(player.getUUID());

        HarmonizedEffect.clearHarmonizedTracking(player.getUUID());
        ExposedEffect.clearExposedTarget(player.getUUID());
        YinYangAnnihilationEffect.clearAnnihilationTarget(player.getUUID());
        EquilibriumData.cleanup(player.getUUID());

        if (harmonizedEntityId != null) {
            LivingEntity harmonizedEntity = findEntityById(player.serverLevel(), harmonizedEntityId);
            if (harmonizedEntity != null && harmonizedEntity.isAlive()) {
                harmonizedEntity.removeEffect(com.complextalents.impl.yygm.effect.YinYangEffects.HARMONIZED.get());
                HarmonizedEffect.cleanupPlayerData(harmonizedEntity, player.getUUID());

                if (harmonizedEntity.level() instanceof ServerLevel level) {
                    PacketHandler.sendToNearby(
                        new YinYangGateStateSyncPacket(
                            harmonizedEntity.getId(),
                            player.getUUID(),
                            -2, 0, 0, 0, 0, 0
                        ),
                        level,
                        harmonizedEntity.position()
                    );
                }
            }
        }

        if (exposedEntityId != null) {
            LivingEntity exposedEntity = findEntityById(player.serverLevel(), exposedEntityId);
            if (exposedEntity != null && exposedEntity.isAlive()) {
                ExposedEffect.removeFromTarget(exposedEntity, player.getUUID());
            }
        }

        if (annihilationEntityId != null) {
            LivingEntity annihilationEntity = findEntityById(player.serverLevel(), annihilationEntityId);
            if (annihilationEntity != null && annihilationEntity.isAlive()) {
                YinYangAnnihilationEffect.removeFromTarget(annihilationEntity, player.getUUID());
            }
        }
    }

    /**
     * Clean up YYGM tracking when a harmonized entity dies.
     * Preserves player's equilibrium stacks (they are player-global).
     * Only clears gate tracking and resets pending hits.
     */
    @SubscribeEvent
    public static void onEntityDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // Handle Harmonized targets
        if (HarmonizedEffect.hasAnyGates(entity)) {
            for (UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                Integer trackedId = HarmonizedEffect.getHarmonizedEntityId(playerUuid);
                if (trackedId != null && trackedId == entity.getId()) {
                    // Clear harmonized tracking so player can harmonize a new target
                    HarmonizedEffect.clearHarmonizedTracking(playerUuid);
                    // Reset pending hits (must complete pairs on the same target)
                    EquilibriumData.resetPendingHits(playerUuid);
                    // Note: Equilibrium stacks are preserved - they are player-global
                    TalentsMod.LOGGER.debug("YYGM player {}'s harmonized target died, gates cleared, equilibrium preserved",
                        playerUuid);
                }
            }
        }

        // Handle Exposed targets
        java.util.Set<UUID> exposedPlayers = new java.util.HashSet<>();
        if (entity.getPersistentData().contains("yygm_exposed")) {
            CompoundTag rootTag = entity.getPersistentData().getCompound("yygm_exposed");
            for (String key : rootTag.getAllKeys()) {
                CompoundTag playerTag = rootTag.getCompound(key);
                if (playerTag.hasUUID("player_uuid")) {
                    UUID playerUuid = playerTag.getUUID("player_uuid");
                    exposedPlayers.add(playerUuid);
                }
            }
        }

        for (UUID playerUuid : exposedPlayers) {
            Integer exposedId = ExposedEffect.getExposedEntityId(playerUuid);
            if (exposedId != null && exposedId == entity.getId()) {
                ExposedEffect.clearExposedTarget(playerUuid);
                TalentsMod.LOGGER.debug("YYGM player {}'s exposed target died, cleared",
                    playerUuid);
            }
        }

        // Handle Yin Yang Annihilation targets
        java.util.Set<UUID> annihilationPlayers = new java.util.HashSet<>();
        if (entity.getPersistentData().contains("yygm_yin_yang_annihilation")) {
            CompoundTag rootTag = entity.getPersistentData().getCompound("yygm_yin_yang_annihilation");
            for (String key : rootTag.getAllKeys()) {
                CompoundTag playerTag = rootTag.getCompound(key);
                if (playerTag.hasUUID("player_uuid")) {
                    UUID playerUuid = playerTag.getUUID("player_uuid");
                    annihilationPlayers.add(playerUuid);
                }
            }
        }

        for (UUID playerUuid : annihilationPlayers) {
            Integer annihilationId = YinYangAnnihilationEffect.getAnnihilationEntityId(playerUuid);
            if (annihilationId != null && annihilationId == entity.getId()) {
                YinYangAnnihilationEffect.clearAnnihilationTarget(playerUuid);
                TalentsMod.LOGGER.debug("YYGM player {}'s Yin Yang Annihilation target died, cleared",
                    playerUuid);
            }
        }
    }

    private static LivingEntity findEntityById(ServerLevel level, int entityId) {
        net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /**
     * Handle Harmonized effect removal immediately when it expires or is removed.
     * This is more immediate than the tick-based fail-safe in onServerTick.
     * Ensures that gate data and client overlay are cleaned up promptly.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectRemoved(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // Check if the removed effect is Harmonized
        if (event.getEffectInstance().getEffect() == YinYangEffects.HARMONIZED.get()) {
            // Find all players who have gates on this entity and clean up
            // Multiple YYGM players can have gates on the same entity
            if (HarmonizedEffect.hasAnyGates(entity)) {
                for (UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                    Integer trackedId = HarmonizedEffect.getHarmonizedEntityId(playerUuid);
                    if (trackedId != null && trackedId == entity.getId()) {
                        HarmonizedEffect.handleEffectExpiration(entity, playerUuid);
                    }
                }
            }
        }

        // Check if the removed effect is Yin Yang Annihilation
        if (event.getEffectInstance().getEffect() == YinYangEffects.YIN_YANG_ANNIHILATION.get()) {
            // Find all players who have annihilation data on this entity and clean up
            if (YinYangAnnihilationEffect.hasAnyAnnihilation(entity)) {
                for (UUID playerUuid : YinYangAnnihilationEffect.getAnnihilationPlayers(entity)) {
                    Integer trackedId = YinYangAnnihilationEffect.getAnnihilationEntityId(playerUuid);
                    if (trackedId != null && trackedId == entity.getId()) {
                        YinYangAnnihilationEffect.removeFromTarget(entity, playerUuid);
                        TalentsMod.LOGGER.debug("YYGM Yin Yang Annihilation effect expired for player {} on entity {}",
                            playerUuid, entity.getName().getString());
                    }
                }
            }
        }
    }
}
