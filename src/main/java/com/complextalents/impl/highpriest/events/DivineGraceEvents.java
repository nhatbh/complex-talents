package com.complextalents.impl.highpriest.events;

import com.complextalents.TalentsMod;
import net.minecraftforge.event.TickEvent;
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
    private static final Map<UUID, PietyCapTracker> PIETY_CAP_TRACKERS = new HashMap<>();

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
     * Ensures piety generation doesn't exceed pietyPerHit * 3 per second globally
     * across all allies.
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
