package com.complextalents.impl.highpriest.skills.covenantofprotection;

import com.complextalents.impl.highpriest.effect.CovenantProtectionEffect;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.impl.highpriest.events.CovenantDamageMitigatedEvent;
import com.complextalents.impl.highpriest.events.CovenantProtectionEffectRemovedEvent;
import com.complextalents.network.DeactivateBeamPacket;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.PulseBeamPacket;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.event.SkillToggleTerminationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * Event handlers for Covenant of Protection skill (simplified).
 * <p>
 * Handles:
 * <ul>
 *   <li>Damage mitigation - reducing damage taken by protected ally</li>
 *   <li>Piety deduction - converting mitigated damage to Piety drain</li>
 *   <li>Range checking - every second, verify caster is in range</li>
 *   <li>Piety checking - remove effect if caster runs out of Piety</li>
 *   <li>Cleanup - handling disconnect and effect removal</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class CovenantOfProtectionEvents {

    // Range check interval (in ticks) - check every 2 ticks
    private static final int RANGE_CHECK_INTERVAL = 2;

    /**
     * Handle death events for Covenant of Protection.
     * When the protected entity dies, clear the caster's toggle state.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // Check if this entity has an active covenant
        if (!CovenantProtectionEffect.hasActiveCovenant(entity)) {
            return;
        }

        UUID casterId = CovenantProtectionEffect.getStoredCasterUUID(entity);
        if (casterId == null) {
            return;
        }

        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
        if (caster != null) {
            // Fire termination event to clear toggle state
            MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                    caster,
                    CovenantOfProtectionSkill.ID,
                    SkillToggleTerminationEvent.TerminationReason.TARGET_DIED
            ));

            // Clear stored target UUID
            CovenantOfProtectionSkill.clearStoredTargetUUID(caster);
        }

        // Clean up effect data
        CovenantProtectionEffect.cleanupCovenantData(entity);
    }

    /**
     * Handle damage events for Covenant of Protection.
     * Reduces damage taken by the protected ally and fires event for Piety deduction.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        // Check if target has Covenant Protection effect
        if (!CovenantProtectionEffect.hasActiveCovenant(event.getEntity())) {
            return;
        }

        // Get the caster
        UUID casterId = CovenantProtectionEffect.getStoredCasterUUID(event.getEntity());
        if (casterId == null) {
            return;
        }

        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
        if (caster == null || !caster.isAlive()) {
            // Caster is gone - fire termination event to clear toggle
            // If caster is null, we can't fire event, but effect will be removed naturally
            if (caster != null) {
                MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                        caster,
                        CovenantOfProtectionSkill.ID,
                        SkillToggleTerminationEvent.TerminationReason.CASTER_DIED
                ));
            }
            event.getEntity().removeEffect(HighPriestEffects.COVENANT_PROTECTION.get());
            return;
        }

        // Apply damage mitigation
        float originalDamage = event.getAmount();
        float mitigatedAmount = CovenantProtectionEffect.applyMitigation(event.getEntity(), originalDamage);

        // The mitigation event may modify the amount (e.g., if Piety is insufficient)
        // Get the actual mitigated amount from the event
        float newDamage = originalDamage - mitigatedAmount;

        // Reduce the damage the ally takes
        event.setAmount(Math.max(0, newDamage));

        // Send visual pulse packet
        PacketHandler.sendToNearby(
                new PulseBeamPacket(casterId, event.getEntity().getUUID()),
                level,
                caster.position()
        );
    }

    /**
     * Handle Piety deduction when damage is mitigated.
     * Also handles partial mitigation when Piety is insufficient.
     */
    @SubscribeEvent
    public static void onDamageMitigated(CovenantDamageMitigatedEvent event) {
        LivingEntity target = event.getTarget();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(event.getCasterId());
        if (caster == null || !caster.isAlive()) {
            return;
        }

        // Get the piety drain rate from the target's NBT
        double pietyDrainRate = CovenantProtectionEffect.getPietyDrainRate(target);
        if (pietyDrainRate <= 0) {
            pietyDrainRate = 1.0;
        }

        // Calculate Piety drain: mitigatedDamage / pietyRate
        double pietyDrain = event.getMitigatedDamage() / pietyDrainRate;

        // Get current Piety
        double currentPiety = OriginManager.getResource(caster);

        if (currentPiety <= 0) {
            // No Piety at all - fire termination event and don't mitigate any damage
            event.setMitigatedDamage(0);
            MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                    caster,
                    CovenantOfProtectionSkill.ID,
                    SkillToggleTerminationEvent.TerminationReason.INSUFFICIENT_RESOURCE
            ));
            return;
        }

        if (pietyDrain > currentPiety) {
            // Not enough Piety for full mitigation
            // Calculate how much damage we CAN mitigate with remaining Piety
            float actualMitigatedDamage = (float) (currentPiety * pietyDrainRate);
            event.setMitigatedDamage(actualMitigatedDamage);

            // Deduct all remaining Piety
            OriginManager.modifyResource(caster, -currentPiety);

            // Fire termination event due to insufficient Piety
            MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                    caster,
                    CovenantOfProtectionSkill.ID,
                    SkillToggleTerminationEvent.TerminationReason.INSUFFICIENT_RESOURCE
            ));
            return;
        }

        // Enough Piety - deduct the full amount
        if (pietyDrain > 0) {
            OriginManager.modifyResource(caster, -pietyDrain);
        }
    }

    /**
     * Handle Covenant Protection effect removal.
     * Cleans up visual effects and notifies players.
     */
    @SubscribeEvent
    public static void onEffectRemoved(CovenantProtectionEffectRemovedEvent event) {
        if (!(event.getTarget().level() instanceof ServerLevel level)) {
            return;
        }

        UUID casterId = event.getCasterId();
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);

        // Send link broken packet
        PacketHandler.sendToNearby(
                new DeactivateBeamPacket(casterId, event.getTarget().getUUID()),
                level,
                event.getTarget().position()
        );

        // Clear caster's stored target UUID and sync toggle state
        if (caster != null) {
            UUID storedTarget = CovenantOfProtectionSkill.getStoredTargetUUID(caster);
            if (storedTarget != null && storedTarget.equals(event.getTarget().getUUID())) {
                CovenantOfProtectionSkill.clearStoredTargetUUID(caster);
            }

            // If effect expired naturally (duration ran out), fire termination event to clear toggle
            // This ensures consistent cleanup through the termination event handler
            if (event.getReason() == CovenantProtectionEffectRemovedEvent.RemovalReason.EXPIRED) {
                MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                        caster,
                        CovenantOfProtectionSkill.ID,
                        SkillToggleTerminationEvent.TerminationReason.UNKNOWN
                ));
                // Termination event handler will show message, return early
                return;
            }

            // Notify based on removal reason
            String reasonMessage = getRemovalMessage(event.getReason());
            if (reasonMessage != null) {
                caster.sendSystemMessage(Component.literal(reasonMessage));
            }
        }

        if (event.getTarget() instanceof ServerPlayer ally) {
            String allyMessage = getAllyRemovalMessage(event.getReason());
            if (allyMessage != null) {
                ally.sendSystemMessage(Component.literal(allyMessage));
            }
        }
    }

    /**
     * Range check - runs every 2 ticks to verify caster is within range.
     * If out of range, removes the effect.
     * <p>
     * Checks ALL LivingEntities with the effect, not just players.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        long gameTime = server.getTickCount();

        // Only check every 2 ticks
        if (gameTime % RANGE_CHECK_INTERVAL != 0) {
            return;
        }

        // Check all levels for entities with active covenants
        for (ServerLevel level : server.getAllLevels()) {
            // Check all entities that could have the covenant effect
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }

                // Check if this entity has an active covenant
                if (!CovenantProtectionEffect.hasActiveCovenant(living)) {
                    continue;
                }

                UUID casterId = CovenantProtectionEffect.getStoredCasterUUID(living);
                if (casterId == null) {
                    continue;
                }

                double range = CovenantProtectionEffect.getRange(living);
                ServerPlayer caster = server.getPlayerList().getPlayer(casterId);

                // Check if caster is still valid and in range
                if (caster == null || !caster.isAlive() || caster.level() != living.level()) {
                    // Caster is gone - fire termination event
                    if (caster != null) {
                        MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                                caster,
                                CovenantOfProtectionSkill.ID,
                                SkillToggleTerminationEvent.TerminationReason.CASTER_DIED
                        ));
                    } else {
                        // Caster is offline, just remove the effect
                        living.removeEffect(HighPriestEffects.COVENANT_PROTECTION.get());
                    }
                    continue;
                }

                // Check distance
                double distance = living.position().distanceTo(caster.position());
                if (distance > range) {
                    // Out of range - fire termination event
                    MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                            caster,
                            CovenantOfProtectionSkill.ID,
                            SkillToggleTerminationEvent.TerminationReason.OUT_OF_RANGE
                    ));
                }
            }
        }
    }

    /**
     * Handle player logout - clean up any covenants involving this player.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check if this player is a caster with an active covenant
        UUID targetId = CovenantOfProtectionSkill.getStoredTargetUUID(player);
        if (targetId != null) {
            if (player.level() instanceof ServerLevel level) {
                // Find the target entity (may be a player or any LivingEntity)
                LivingEntity target = null;
                ServerPlayer playerTarget = level.getServer().getPlayerList().getPlayer(targetId);
                if (playerTarget != null) {
                    target = playerTarget;
                } else {
                    // Search for entities in the level
                    for (var entity : level.getAllEntities()) {
                        if (entity.getUUID().equals(targetId) && entity instanceof LivingEntity living) {
                            target = living;
                            break;
                        }
                    }
                }
                if (target != null && CovenantProtectionEffect.hasActiveCovenant(target)) {
                    target.removeEffect(HighPriestEffects.COVENANT_PROTECTION.get());
                }
            }
            CovenantOfProtectionSkill.clearStoredTargetUUID(player);
            return;
        }

        // Check if this player is a protected target
        if (CovenantProtectionEffect.hasActiveCovenant(player)) {
            // Find the caster and clear their stored target
            UUID casterId = CovenantProtectionEffect.getStoredCasterUUID(player);
            if (casterId != null && player.level() instanceof ServerLevel level) {
                ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
                if (caster != null) {
                    UUID storedTarget = CovenantOfProtectionSkill.getStoredTargetUUID(caster);
                    if (storedTarget != null && storedTarget.equals(player.getUUID())) {
                        CovenantOfProtectionSkill.clearStoredTargetUUID(caster);
                    }
                }
            }
            player.removeEffect(HighPriestEffects.COVENANT_PROTECTION.get());
        }
    }

    // ========== Helper Methods ==========

    private static String getRemovalMessage(CovenantProtectionEffectRemovedEvent.RemovalReason reason) {
        return switch (reason) {
            case OUT_OF_RANGE -> "§cCovenant of Protection broken: Target out of range.";
            case NO_PIETY -> "§cCovenant of Protection broken: Insufficient Piety.";
            case EXPIRED -> "§eCovenant of Protection has expired.";
            case MANUALLY_TOGGLED_OFF -> "§eCovenant of Protection deactivated.";
            case CASTER_GONE -> null; // Don't send message if caster is gone
            default -> null;
        };
    }

    private static String getAllyRemovalMessage(CovenantProtectionEffectRemovedEvent.RemovalReason reason) {
        return switch (reason) {
            case OUT_OF_RANGE -> "§cCovenant of Protection broken: Caster out of range.";
            case NO_PIETY -> "§cCovenant of Protection broken: Caster has insufficient Piety.";
            case EXPIRED -> "§eCovenant of Protection has expired.";
            case MANUALLY_TOGGLED_OFF -> "§eCovenant of Protection deactivated by caster.";
            case CASTER_GONE -> "§cCovenant of Protection broken: Caster disconnected.";
            default -> null;
        };
    }
}
