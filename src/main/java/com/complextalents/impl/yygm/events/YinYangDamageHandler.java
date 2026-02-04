package com.complextalents.impl.yygm.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.EquilibriumData;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import com.complextalents.origin.OriginManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
        int nextRequired = HarmonizedEffect.getNextRequiredGate(target, player.getUUID());
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
     * Apply true damage with equilibrium bonus.
     */
    private static void applyTrueDamage(LivingDamageEvent event, ServerPlayer player, LivingEntity target) {
        float originalDamage = event.getAmount();

        // Get current Equilibrium from player-global data
        int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());

        // Apply true damage multiplier base + Equilibrium bonus
        double trueDamageMult = OriginManager.getOriginStat(player, "trueDamageMultiplier");
        double equilibriumBonusPercent = OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");
        double equilibriumBonus = 1.0 + (equilibrium * equilibriumBonusPercent);
        float trueDamage = originalDamage * (float) trueDamageMult * (float) equilibriumBonus;
        event.setAmount(trueDamage);

        TalentsMod.LOGGER.debug("YYGM {} applying true damage: {} -> {} (eq: {})",
            player.getName().getString(), originalDamage, trueDamage, equilibrium);
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
     * Handle server tick for expired harmonized effect fail-safe checking.
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

        for (ServerLevel level : event.getServer().getAllLevels()) {
            double searchRange = 128.0;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                        net.minecraft.world.phys.Vec3.ZERO,
                        new net.minecraft.world.phys.Vec3(searchRange, 256.0, searchRange)
                    ).inflate(searchRange))) {

                if (!HarmonizedEffect.hasAnyGates(entity)) {
                    continue;
                }

                for (UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                    // Check for expired Harmonized effect (data exists but effect is gone)
                    // This is a fail-safe to clean up orphaned data
                    if (HarmonizedEffect.getHarmonizedEntityId(playerUuid) != null
                            && !entity.hasEffect(com.complextalents.impl.yygm.effect.YinYangEffects.HARMONIZED.get())) {
                        HarmonizedEffect.handleEffectExpiration(entity, playerUuid);
                        continue;
                    }
                }
            }
        }
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

            if (!HarmonizedEffect.hasAnyGates(entity)) {
                continue;
            }

            for (UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                int yangGate = HarmonizedEffect.getYangGateDirection(entity, playerUuid);
                int yinGate = HarmonizedEffect.getYinGateDirection(entity, playerUuid);
                int nextRequired = HarmonizedEffect.getNextRequiredGate(entity, playerUuid);
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

        // Note: Equilibrium is player-global and synced via EquilibriumSyncPacket
        // It's already loaded from EquilibriumData, no need to sync from entity NBT

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

        HarmonizedEffect.clearHarmonizedTracking(player.getUUID());
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

        if (!HarmonizedEffect.hasAnyGates(entity)) {
            return;
        }

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

    private static LivingEntity findEntityById(ServerLevel level, int entityId) {
        net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
