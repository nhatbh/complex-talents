package com.complextalents.impl.highpriest.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.effect.DivineExaltationEffect;
import com.complextalents.origin.OriginManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event handlers for Divine Grace skill effects.
 * <p>
 * Handles piety generation from Divine Exaltation effect.
 * Maintains per-second tracking to enforce global piety gain cap.
 * </p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DivineGraceEvents {

    /**
     * Per-second piety tracking for each caster.
     * Maps caster UUID -> PietyCapTracker
     */
    private static final Map<UUID, PietyCapTracker> PIETY_CAP_TRACKERS = new HashMap<>();

    /**
     * Handle damage dealt by entities with Divine Exaltation.
     * Generates piety for the caster with global per-second cap.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        // Check if attacker is a LivingEntity with Divine Exaltation
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        if (!DivineExaltationEffect.hasActiveEffect(attacker)) {
            return;
        }

        // Get caster info
        UUID casterId = DivineExaltationEffect.getCasterUUID(attacker);
        if (casterId == null) {
            return;
        }

        // Get piety per hit amount
        double pietyPerHit = DivineExaltationEffect.getPietyPerHit(attacker);
        if (pietyPerHit <= 0) {
            return;
        }

        // Find caster player
        ServerLevel level = (attacker.level() instanceof ServerLevel serverLevel) ? serverLevel : null;
        if (level == null) {
            return;
        }

        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
        if (caster == null) {
            return;
        }

        // Calculate current game second for cap tracking
        long currentSecond = level.getGameTime() / 20;

        // Get or create tracker for this caster
        PietyCapTracker tracker = PIETY_CAP_TRACKERS.computeIfAbsent(casterId, k -> new PietyCapTracker());

        // Set the cap based on piety per hit (pietyPerHit * 3 per second globally)
        tracker.setCap(pietyPerHit * 3.0);

        // Try to add piety (with cap enforcement)
        double pietyGained = tracker.tryAddPiety(pietyPerHit, currentSecond);

        if (pietyGained > 0) {
            // Add piety to caster
            OriginManager.modifyResource(caster, pietyGained);

            // Fire custom event for other systems to hook into
            DivineExaltationPietyEvent pietyEvent = new DivineExaltationPietyEvent(
                    caster, attacker, event.getEntity(), pietyPerHit, pietyGained
            );
            MinecraftForge.EVENT_BUS.post(pietyEvent);

            TalentsMod.LOGGER.debug("Divine Exaltation: Generated {} piety for {} from {}",
                    pietyGained, caster.getName().getString(), attacker.getName().getString());
        }
    }

    /**
     * Clean up old trackers periodically to prevent memory leaks.
     * Remove trackers for casters who haven't generated piety in the last minute.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Clean up every 5 seconds (100 ticks)
        if (event.getServer().getTickCount() % 100 != 0) {
            return;
        }

        // Use tick count as current time for cleanup comparison
        long currentTime = event.getServer().getTickCount();
        PIETY_CAP_TRACKERS.entrySet().removeIf(entry -> {
            PietyCapTracker tracker = entry.getValue();
            // Remove if no activity for 60 seconds (1200 ticks)
            return (currentTime - tracker.lastTickTime) > 1200;
        });
    }

    /**
     * Tracker for per-second piety gain cap.
     * Ensures piety generation doesn't exceed pietyPerHit * 3 per second globally across all allies.
     */
    public static class PietyCapTracker {
        private long lastSecond = -1;
        private double pietyThisSecond = 0.0;
        private double cap = 0.0;
        long lastTickTime = 0;

        /**
         * Set the piety cap for this tracker.
         *
         * @param cap Maximum piety per second (pietyPerHit * 3)
         */
        public void setCap(double cap) {
            this.cap = cap;
        }

        /**
         * Try to add piety, respecting the per-second cap.
         *
         * @param amount        Amount of piety to add
         * @param currentSecond Current game second (gameTime / 20)
         * @return Actual amount of piety added (may be less than requested due to cap)
         */
        public double tryAddPiety(double amount, long currentSecond) {
            lastTickTime = currentSecond * 20;

            // Reset if we're in a new second
            if (currentSecond != lastSecond) {
                pietyThisSecond = 0.0;
                lastSecond = currentSecond;
            }

            // Calculate remaining space in cap
            double space = cap - pietyThisSecond;
            double toAdd = Math.min(amount, Math.max(0, space));
            pietyThisSecond += toAdd;

            return toAdd;
        }

        /**
         * Get the current piety accumulated this second.
         */
        public double getPietyThisSecond() {
            return pietyThisSecond;
        }

        /**
         * Get the cap for this second.
         */
        public double getCap() {
            return cap;
        }

        /**
         * Reset the tracker (force new second).
         */
        public void reset() {
            lastSecond = -1;
            pietyThisSecond = 0.0;
        }
    }

    /**
     * Get the tracker for a specific caster (for debugging/monitoring).
     */
    public static PietyCapTracker getTracker(UUID casterId) {
        return PIETY_CAP_TRACKERS.get(casterId);
    }

    /**
     * Remove a tracker (called when caster logs out or effect expires).
     */
    public static void removeTracker(UUID casterId) {
        PIETY_CAP_TRACKERS.remove(casterId);
    }
}
