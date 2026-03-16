package com.complextalents.leveling.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side storage for leveling data.
 */
@OnlyIn(Dist.CLIENT)
public class ClientLevelingData {
    private static int level = 1;
    private static double currentXP = 0;
    private static double xpForNext = 100;
    private static double chunkFatigue = 1.0;

    public static int getLevel() {
        return level;
    }

    public static void setLevel(int level) {
        ClientLevelingData.level = level;
    }

    public static double getCurrentXP() {
        return currentXP;
    }

    public static void setCurrentXP(double currentXP) {
        ClientLevelingData.currentXP = currentXP;
    }

    public static double getXpForNext() {
        return xpForNext;
    }

    public static void setXpForNext(double xpForNext) {
        ClientLevelingData.xpForNext = xpForNext;
    }

    public static double getChunkFatigue() {
        return chunkFatigue;
    }

    public static void setChunkFatigue(double chunkFatigue) {
        ClientLevelingData.chunkFatigue = chunkFatigue;
    }
}
