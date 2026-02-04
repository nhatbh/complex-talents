package com.complextalents.network.yygm;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side storage for YYGM Equilibrium data.
 * Used by YinYangRenderer for HUD display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientEquilibriumData {

    private static int equilibrium = 0;
    private static long lastUpdateTime = 0;

    // Decay time in milliseconds (10 seconds)
    private static final long DECAY_TIME_MS = 10000L;

    /**
     * Set equilibrium from server sync (hitTime parameter ignored for client-side timer).
     */
    public static void setEquilibrium(int value, long hitTime) {
        equilibrium = Math.max(0, Math.min(8, value));
        // Use current system time as the update time for accurate countdown
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Set equilibrium from server sync.
     */
    public static void setEquilibrium(int value) {
        setEquilibrium(value, 0);
    }

    /**
     * Get the current equilibrium stacks.
     */
    public static int getEquilibrium() {
        return equilibrium;
    }

    /**
     * Get the timestamp of last update.
     */
    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Get the seconds remaining until equilibrium decays.
     * Returns 0 if no equilibrium or time has expired.
     */
    public static float getSecondsRemaining() {
        if (equilibrium == 0 || lastUpdateTime == 0) {
            return 0f;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;
        float remaining = DECAY_TIME_MS - elapsedTime;

        return Math.max(0f, remaining / 1000f);
    }

    /**
     * Clear equilibrium data (on logout/world change).
     */
    public static void clear() {
        equilibrium = 0;
        lastUpdateTime = 0;
    }
}
