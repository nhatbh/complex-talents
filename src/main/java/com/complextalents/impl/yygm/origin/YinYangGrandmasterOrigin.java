package com.complextalents.impl.yygm.origin;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.EquilibriumData;
import com.complextalents.impl.yygm.client.renderer.YinYangRenderer;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.events.YYGMGateHitEvent;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.SpawnYinYangGateFXPacket;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Yin Yang Grandmaster Origin - A rhythm-based melee class that uses
 * alternating Yin/Yang gates with Equilibrium as a damage amplifier.
 *
 * <h3>Resource: Equilibrium (0-8 stacks)</h3>
 * <ul>
 *   <li><strong>Gained by:</strong> Completing Yin+Yang gate hit pairs</li>
 *   <li><strong>Lost by:</strong> Wrong gate type (all), 10s inactivity (all), empty gate hit (1)</li>
 *   <li><strong>Effect:</strong> Each stack adds true damage percent to gate hits</li>
 * </ul>
 *
 * <h3>Core Mechanic: The Tao of Harmony</h3>
 * <p>
 * When a Yin Yang Grandmaster melee attacks an enemy:
 * </p>
 * <ul>
 *   <li>The "Harmonized" effect is applied to the target</li>
 *   <li>Two Gates spawn: One Gold (Yang) and one Silver (Yin) at different compass directions</li>
 *   <li>Must strike gates in alternating order (Yang→Yin→Yang or Yin→Yang→Yin)</li>
 *   <li>After hitting 1 Yin AND 1 Yang: gain 1 Equilibrium stack, then tracking resets for next pair</li>
 *   <li>All players can see the Bagua gates under the entity's feet</li>
 * </ul>
 *
 * <h3>Damage System</h3>
 * <ul>
 *   <li><strong>Correct Gate Hit:</strong> True damage boosted by Equilibrium stacks (+5/8/12/15% per stack by level)</li>
 *   <li><strong>Wrong Gate Hit:</strong> Lose all Equilibrium, trigger Discord (Nausea + Weakness, 15s)</li>
 *   <li><strong>Empty Gate Hit:</strong> Lose 1 Equilibrium, reduced body damage</li>
 *   <li><strong>No Gate Hit:</strong> Body hit damage reduction</li>
 * </ul>
 */
public class YinYangGrandmasterOrigin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "yygm");

    /**
     * Register the Yin Yang Grandmaster origin.
     * Call this during mod initialization.
     */
    public static void register() {
        OriginBuilder.create("complextalents", "yygm")
                .displayName("Yin Yang Grandmaster")
                .description(Component.literal("The Tao of Harmony - The laws of the universe dictate the flow of battle"))
                .maxLevel(5)
                // Custom HUD renderer for Equilibrium stacks
                .renderer(new YinYangRenderer())
                // True damage multiplier base on correct gate hit: [1.5, 1.7, 2.0, 2.5, 3.0]
                .scaledStat("trueDamageMultiplier", new double[]{1.5, 1.7, 2.0, 2.5, 3.0})
                // True damage percent bonus per Equilibrium stack: [5%, 8%, 12%, 15%, 15%]
                .scaledStat("equilibriumTrueDamagePercent", new double[]{0.05, 0.08, 0.12, 0.15, 0.15})
                // Gate cooldown in ticks before next gate spawns: [40, 35, 30, 25, 20]
                .scaledStat("gateCooldownTicks", new double[]{40.0, 35.0, 30.0, 25.0, 20.0})
                // Damage reduction percentage on body hit: [90%, 85%, 80%, 75%, 70%]
                .scaledStat("bodyHitDamageReduction", new double[]{0.90, 0.85, 0.80, 0.75, 0.70})
                .register();

        TalentsMod.LOGGER.info("Yin Yang Grandmaster origin registered");
    }

    /**
     * Get the Yin Yang Grandmaster origin ID.
     */
    public static ResourceLocation getId() {
        return ID;
    }

    /**
     * Check if a player is a Yin Yang Grandmaster.
     */
    public static boolean isYinYangGrandmaster(net.minecraft.server.level.ServerPlayer player) {
        return ID.equals(OriginManager.getOriginId(player));
    }

    /**
     * Event handlers for YYGM gate combat logic.
     * Listens to YYGMGateHitEvent fired by the damage handler.
     */
    @Mod.EventBusSubscriber(modid = TalentsMod.MODID)
    public static class GateCombatEvents {

        /**
         * Listen to YYGMGateHitEvent (fired by damage handler with pre-calculated data).
         * Refreshes the Harmonized effect and routes to appropriate handler.
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onGateHit(YYGMGateHitEvent event) {
            ServerPlayer player = event.getPlayer();
            LivingEntity target = event.getTarget();

            // Refresh Harmonized effect duration
            HarmonizedEffect.applyToTarget(target, player.getUUID());
            HarmonizedEffect.syncGateState(target, player.getUUID());

            // Update last hit time for decay tracking (player-global in EquilibriumData)
            long currentTime = target.level().getGameTime();
            EquilibriumData.updateLastHitTime(player.getUUID(), currentTime);

            // Route to appropriate handler based on HitResult
            switch (event.getHitResult()) {
                case TRUE_GATE:
                    handleTrueGateHit(event);
                    break;
                case FALSE_GATE:
                    handleFalseGateHit(event);
                    break;
                case EMPTY_GATE:
                    handleEmptyGateHit(event);
                    break;
            }
        }

        /**
         * Handle successful true gate hit.
         * Applies true damage with equilibrium bonus, tracks pending hits,
         * gains Equilibrium on pair completion, toggles next gate, schedules respawn.
         */
        private static void handleTrueGateHit(YYGMGateHitEvent event) {
            ServerPlayer player = event.getPlayer();
            LivingEntity target = event.getTarget();
            int gateType = event.getGateType();

            // Get current Equilibrium from player-global data
            int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());

            // Apply true damage multiplier base + Equilibrium bonus
            double trueDamageMult = OriginManager.getOriginStat(player, "trueDamageMultiplier");
            double equilibriumBonusPercent = OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");
            double equilibriumBonus = 1.0 + (equilibrium * equilibriumBonusPercent);

            // Get original damage from event context (stored in LivingDamageEvent)
            // For now, we'll let the damage event handle the actual damage modification
            // This handler just updates Equilibrium and gate state

            // Track pending hit for pair completion (player-global)
            if (gateType == YYGMGateHitEvent.GATE_YANG) {
                EquilibriumData.setPendingYangHit(player.getUUID(), true);
                TalentsMod.LOGGER.debug("YYGM {} hit Yang gate, pending Yin: {}",
                    player.getName().getString(), EquilibriumData.hasPendingYinHit(player.getUUID()));
            } else {
                EquilibriumData.setPendingYinHit(player.getUUID(), true);
                TalentsMod.LOGGER.debug("YYGM {} hit Yin gate, pending Yang: {}",
                    player.getName().getString(), EquilibriumData.hasPendingYangHit(player.getUUID()));
            }

            // Check if pair is complete (both Yin and Yang hit)
            if (EquilibriumData.isPairComplete(player.getUUID())) {
                // Gain 1 Equilibrium
                int currentEquilibrium = EquilibriumData.getEquilibrium(player.getUUID());
                if (currentEquilibrium < EquilibriumData.MAX_EQUILIBRIUM) {
                    int newEquilibrium = currentEquilibrium + 1;
                    EquilibriumData.setEquilibrium(player, newEquilibrium);
                    TalentsMod.LOGGER.debug("YYGM {} gained Equilibrium! Now: {}",
                        player.getName().getString(), newEquilibrium);
                }

                // Reset pending hits for next pair
                EquilibriumData.resetPendingHits(player.getUUID());
            }

            // Sync to client to reset timer (even if equilibrium didn't change)
            EquilibriumData.syncToClient(player);

            // Toggle next required gate
            int newNextRequired = (gateType == YYGMGateHitEvent.GATE_YANG)
                ? YYGMGateHitEvent.GATE_YIN
                : YYGMGateHitEvent.GATE_YANG;
            HarmonizedEffect.setNextRequiredGate(target, player.getUUID(), newNextRequired);

            // Schedule gate respawn using scaled stat
            long currentTime = target.level().getGameTime();
            double respawnTicks = OriginManager.getOriginStat(player, "gateCooldownTicks");
            long respawnTick = currentTime + (long) respawnTicks;

            if (gateType == YYGMGateHitEvent.GATE_YANG) {
                HarmonizedEffect.setYangRespawnTick(target, player.getUUID(), respawnTick);
                HarmonizedEffect.setYangGateDirection(target, player.getUUID(), HarmonizedEffect.GATE_NONE);
            } else {
                HarmonizedEffect.setYinRespawnTick(target, player.getUUID(), respawnTick);
                HarmonizedEffect.setYinGateDirection(target, player.getUUID(), HarmonizedEffect.GATE_NONE);
            }

            // Sync and FX
            HarmonizedEffect.syncGateState(target, player.getUUID());

            SpawnYinYangGateFXPacket.EffectType fxType = (gateType == YYGMGateHitEvent.GATE_YANG)
                ? SpawnYinYangGateFXPacket.EffectType.YANG_HIT
                : SpawnYinYangGateFXPacket.EffectType.YIN_HIT;

            PacketHandler.sendToNearby(
                new SpawnYinYangGateFXPacket(target.position(), fxType),
                (ServerLevel) target.level(), target.position()
            );

            String gateName = (gateType == YYGMGateHitEvent.GATE_YANG) ? "Yang" : "Yin";
            TalentsMod.LOGGER.debug("YYGM {} SUCCESS: hit {} gate, eq: {}, next: {}",
                player.getName().getString(), gateName,
                EquilibriumData.getEquilibrium(player.getUUID()),
                newNextRequired == YYGMGateHitEvent.GATE_YANG ? "Yang" : "Yin");
        }

        /**
         * Handle failed gate hit (wrong gate type).
         * Sets damage to ZERO, loses ALL Equilibrium, applies Discord.
         */
        private static void handleFalseGateHit(YYGMGateHitEvent event) {
            ServerPlayer player = event.getPlayer();
            LivingEntity target = event.getTarget();
            int gateType = event.getGateType();

            // Wrong gate = ZERO damage (handled in damage handler)
            // Lose ALL Equilibrium (player-global)
            int lostEquilibrium = EquilibriumData.getEquilibrium(player.getUUID());
            if (lostEquilibrium > 0) {
                EquilibriumData.setEquilibrium(player, 0);
                TalentsMod.LOGGER.debug("YYGM {} lost all {} Equilibrium from wrong gate",
                    player.getName().getString(), lostEquilibrium);
            }

            // Reset pending hits
            EquilibriumData.resetPendingHits(player.getUUID());

            // Apply Discord to player: Nausea + Weakness for 15 seconds (300 ticks)
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 300, 0, false, false));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 300, 0, false, false));

            // Respawn the wrong gate that was hit using scaled stat
            long currentTime = target.level().getGameTime();
            double respawnTicks = OriginManager.getOriginStat(player, "gateCooldownTicks");
            long respawnTick = currentTime + (long) respawnTicks;

            if (gateType == YYGMGateHitEvent.GATE_YANG) {
                HarmonizedEffect.setYangRespawnTick(target, player.getUUID(), respawnTick);
                HarmonizedEffect.setYangGateDirection(target, player.getUUID(), HarmonizedEffect.GATE_NONE);
            } else {
                HarmonizedEffect.setYinRespawnTick(target, player.getUUID(), respawnTick);
                HarmonizedEffect.setYinGateDirection(target, player.getUUID(), HarmonizedEffect.GATE_NONE);
            }

            // Sync and FX
            HarmonizedEffect.syncGateState(target, player.getUUID());

            PacketHandler.sendToNearby(
                new SpawnYinYangGateFXPacket(target.position(), SpawnYinYangGateFXPacket.EffectType.DISCORD),
                (ServerLevel) target.level(), target.position()
            );

            TalentsMod.LOGGER.debug("YYGM {} FAILURE: hit wrong gate, Discord applied",
                player.getName().getString());
        }

        /**
         * Handle empty gate hit (hit neither gate).
         * Lose 1 Equilibrium (if any), apply body penalty.
         * Exception: Sword Dance hits don't lose Equilibrium.
         */
        private static void handleEmptyGateHit(YYGMGateHitEvent event) {
            // Skip Equilibrium loss if from Sword Dance
            if (event.isFromSwordDance()) {
                TalentsMod.LOGGER.debug("YYGM {} hit empty gate via Sword Dance, no Equilibrium loss",
                    event.getPlayer().getName().getString());
                return;
            }

            ServerPlayer player = event.getPlayer();
            LivingEntity target = event.getTarget();

            // Lose 1 Equilibrium (player-global)
            int currentEquilibrium = EquilibriumData.getEquilibrium(player.getUUID());
            if (currentEquilibrium > 0) {
                int newEquilibrium = currentEquilibrium - 1;
                EquilibriumData.setEquilibrium(player, newEquilibrium);
                TalentsMod.LOGGER.debug("YYGM {} hit empty gate, lost 1 Equilibrium (now: {})",
                    player.getName().getString(), newEquilibrium);
            }

            // Body hit penalty is applied in the damage handler
        }

        /**
         * Gate respawn tick handler.
         * Runs every 5 ticks to check for gates that need respawning.
         */
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // Check every 5 ticks for gate respawn performance
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

                    long currentTime = level.getGameTime();

                    for (java.util.UUID playerUuid : HarmonizedEffect.getGatePlayers(entity)) {
                        // Check Yang gate respawn
                        long yangRespawn = HarmonizedEffect.getYangRespawnTick(entity, playerUuid);
                        int yangGate = HarmonizedEffect.getYangGateDirection(entity, playerUuid);

                        if (yangGate == HarmonizedEffect.GATE_NONE && yangRespawn > 0 && currentTime >= yangRespawn) {
                            respawnGate(entity, playerUuid, HarmonizedEffect.GATE_YANG);
                        }

                        // Check Yin gate respawn
                        long yinRespawn = HarmonizedEffect.getYinRespawnTick(entity, playerUuid);
                        int yinGate = HarmonizedEffect.getYinGateDirection(entity, playerUuid);

                        if (yinGate == HarmonizedEffect.GATE_NONE && yinRespawn > 0 && currentTime >= yinRespawn) {
                            respawnGate(entity, playerUuid, HarmonizedEffect.GATE_YIN);
                        }
                    }
                }
            }
        }

        /**
         * Equilibrium decay tick handler.
         * Runs every 5 ticks to check for equilibrium decay (10 seconds = 200 ticks).
         * Uses player-global EquilibriumData instead of entity NBT.
         */
        @SubscribeEvent
        public static void onEquilibriumDecayTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // Check every 5 ticks
            if (event.getServer().getTickCount() % 5 != 0) {
                return;
            }

            // Check all online players for equilibrium decay
            for (ServerLevel level : event.getServer().getAllLevels()) {
                long currentTime = level.getGameTime();

                for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                    // Only check YYGM players
                    if (!isYinYangGrandmaster(player)) {
                        continue;
                    }

                    // Check if equilibrium should decay (10 seconds since last gate hit)
                    if (EquilibriumData.shouldDecay(player.getUUID(), currentTime)) {
                        int currentEq = EquilibriumData.getEquilibrium(player.getUUID());
                        if (currentEq > 0) {
                            EquilibriumData.setEquilibrium(player, 0);
                            TalentsMod.LOGGER.debug("YYGM Equilibrium decayed for {} (had {} stacks)",
                                player.getName().getString(), currentEq);
                        }
                    }
                }
            }
        }

        /**
         * Respawn a gate at a new random direction (different from the other gate).
         * Only spawns in slots that haven't been used yet. Resets when all 8 slots are used.
         */
        private static void respawnGate(LivingEntity entity, java.util.UUID playerUuid, int gateType) {
            int otherGateDir = (gateType == HarmonizedEffect.GATE_YANG)
                ? HarmonizedEffect.getYinGateDirection(entity, playerUuid)
                : HarmonizedEffect.getYangGateDirection(entity, playerUuid);

            // Pick a random available slot (excluding the other gate's position)
            int newDir = HarmonizedEffect.getRandomAvailableSlot(entity, playerUuid, otherGateDir, entity.getRandom());

            if (gateType == HarmonizedEffect.GATE_YANG) {
                HarmonizedEffect.setYangGateDirection(entity, playerUuid, newDir);
                HarmonizedEffect.setYangRespawnTick(entity, playerUuid, 0);
            } else {
                HarmonizedEffect.setYinGateDirection(entity, playerUuid, newDir);
                HarmonizedEffect.setYinRespawnTick(entity, playerUuid, 0);
            }

            HarmonizedEffect.syncGateState(entity, playerUuid);

            // Spawn FX
            PacketHandler.sendToNearby(
                new SpawnYinYangGateFXPacket(entity.position(), SpawnYinYangGateFXPacket.EffectType.GATE_SPAWN),
                (ServerLevel) entity.level(), entity.position()
            );

            TalentsMod.LOGGER.debug("Respawned {} gate at direction {} for player {} on entity {} (used bitmap: {})",
                gateType == HarmonizedEffect.GATE_YANG ? "Yang" : "Yin", newDir, playerUuid,
                entity.getName().getString(), HarmonizedEffect.getUsedSlotsBitmap(entity, playerUuid));
        }
    }
}
