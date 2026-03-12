package com.complextalents.impl.highpriest.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side storage for High Priest Faith data.
 * Used by HighPriestRenderer for HUD display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientFaithData {

    private static double faith = 0;

    /**
     * Set faith from server sync.
     */
    public static void setFaith(double value) {
        faith = Math.max(0, value);
    }

    /**
     * Get the current faith count.
     */
    public static double getFaith() {
        return faith;
    }

    /**
     * Clear all data (on logout/world change).
     */
    public static void clear() {
        faith = 0;
    }
}
