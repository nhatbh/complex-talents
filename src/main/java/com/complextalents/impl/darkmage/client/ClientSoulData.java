package com.complextalents.impl.darkmage.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side storage for Dark Mage Soul data.
 * Used by DarkMageRenderer for HUD display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSoulData {

    private static int souls = 0;
    private static boolean bloodPactActive = false;
    private static long phylacteryCooldownTicks = 0;
    private static long phylacteryTotalCooldownTicks = 0;
    private static long cooldownSyncTime = 0; // Client time when cooldown was synced

    /**
     * Set souls from server sync.
     */
    public static void setSouls(int value) {
        souls = Math.max(0, value);
    }

    /**
     * Get the current soul count.
     */
    public static int getSouls() {
        return souls;
    }

    /**
     * Set Blood Pact active state from server sync.
     */
    public static void setBloodPactActive(boolean active) {
        bloodPactActive = active;
    }

    /**
     * Check if Blood Pact is active.
     */
    public static boolean isBloodPactActive() {
        return bloodPactActive;
    }

    /**
     * Set Phylactery cooldown from server sync.
     */
    public static void setPhylacteryCooldown(long remainingTicks, long totalTicks) {
        phylacteryCooldownTicks = remainingTicks;
        phylacteryTotalCooldownTicks = totalTicks;
        cooldownSyncTime = System.currentTimeMillis();
    }

    /**
     * Get remaining Phylactery cooldown in ticks (client-side interpolated).
     */
    public static long getPhylacteryCooldownTicks() {
        if (phylacteryCooldownTicks <= 0) {
            return 0;
        }
        // Interpolate based on time passed since sync (50ms per tick)
        long elapsedMs = System.currentTimeMillis() - cooldownSyncTime;
        long elapsedTicks = elapsedMs / 50;
        return Math.max(0, phylacteryCooldownTicks - elapsedTicks);
    }

    /**
     * Get total Phylactery cooldown in ticks (for ratio calculation).
     */
    public static long getPhylacteryTotalCooldownTicks() {
        return phylacteryTotalCooldownTicks;
    }

    /**
     * Check if Phylactery is on cooldown.
     */
    public static boolean isPhylacteryOnCooldown() {
        return getPhylacteryCooldownTicks() > 0;
    }

    /**
     * Get Phylactery cooldown ratio (0.0 to 1.0, where 1.0 = full cooldown remaining).
     */
    public static float getPhylacteryCooldownRatio() {
        if (phylacteryTotalCooldownTicks <= 0) {
            return 0f;
        }
        return (float) getPhylacteryCooldownTicks() / phylacteryTotalCooldownTicks;
    }

    /**
     * Get remaining Phylactery cooldown in seconds.
     */
    public static float getPhylacteryCooldownSeconds() {
        return getPhylacteryCooldownTicks() / 20f;
    }

    /**
     * Clear all data (on logout/world change).
     */
    public static void clear() {
        souls = 0;
        bloodPactActive = false;
        phylacteryCooldownTicks = 0;
        phylacteryTotalCooldownTicks = 0;
        cooldownSyncTime = 0;
    }
}
