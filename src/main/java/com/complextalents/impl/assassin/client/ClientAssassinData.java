package com.complextalents.impl.assassin.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracking for Assassin data.
 */
public class ClientAssassinData {
    private static double stealthGauge = 100.0;
    private static double maxStealthGauge = 100.0;
    
    // Entity ID -> [startTime, expirationTime]
    private static final Map<Integer, long[]> ENTITY_COOLDOWNS = new ConcurrentHashMap<>();

    public static void setData(double gauge, double max) {
        stealthGauge = gauge;
        maxStealthGauge = max;
    }

    public static void setEntityCooldown(int entityId, long startTime, long expirationTime) {
        ENTITY_COOLDOWNS.put(entityId, new long[]{startTime, expirationTime});
    }

    public static long[] getEntityCooldown(int entityId) {
        return ENTITY_COOLDOWNS.get(entityId);
    }

    public static double getStealthGauge() {
        return stealthGauge;
    }

    public static double getMaxStealthGauge() {
        return maxStealthGauge;
    }

    public static double getGaugePercent() {
        return stealthGauge / maxStealthGauge;
    }
}
