package com.complextalents.leveling.service;

import com.complextalents.leveling.events.fatigue.FatigueAppliedEvent;
import com.complextalents.leveling.events.fatigue.FatigueRecoveredEvent;
import com.complextalents.leveling.fatigue.ChunkFatigueData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;

import java.util.Objects;

/**
 * Service layer for fatigue operations.
 * Provides a clean abstraction over fatigue data storage and event firing.
 *
 * <p>Fatigue is a per-chunk multiplier that reduces XP rewards after many kills
 * in the same area. This encourages exploration and prevents farming in one spot.</p>
 *
 * <p>Singleton pattern with lazy initialization and double-checked locking.</p>
 *
 * <p>Thread-safe.</p>
 */
public class FatigueService {
    private static volatile FatigueService INSTANCE;

    private FatigueService() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of FatigueService.
     * Uses double-checked locking for thread-safety and performance.
     *
     * @return The FatigueService singleton
     */
    public static FatigueService getInstance() {
        if (INSTANCE == null) {
            synchronized (FatigueService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FatigueService();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Gets the current fatigue multiplier for a chunk.
     * The multiplier ranges from 0.0 (completely fatigued) to 1.0 (no fatigue).
     *
     * @param level The server level
     * @param chunkPos The chunk position
     * @return The fatigue multiplier (0.0 to 1.0)
     */
    public double getMultiplier(ServerLevel level, ChunkPos chunkPos) {
        Objects.requireNonNull(level, "ServerLevel cannot be null");
        Objects.requireNonNull(chunkPos, "ChunkPos cannot be null");

        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        return fatigueData.getMultiplier(chunkPos);
    }

    /**
     * Applies fatigue degradation to a chunk due to XP awards.
     * Fires FatigueAppliedEvent.
     *
     * <p>Degradation is based on the amount of XP awarded.
     * More XP = more degradation.</p>
     *
     * @param level The server level
     * @param chunkPos The chunk position
     * @param xpAmount The amount of XP that caused degradation
     */
    public void applyDegradation(ServerLevel level, ChunkPos chunkPos, double xpAmount) {
        Objects.requireNonNull(level, "ServerLevel cannot be null");
        Objects.requireNonNull(chunkPos, "ChunkPos cannot be null");

        if (xpAmount <= 0) return;

        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        double oldMultiplier = fatigueData.getMultiplier(chunkPos);

        fatigueData.applyDegradation(chunkPos, xpAmount);

        double newMultiplier = fatigueData.getMultiplier(chunkPos);

        // Only fire event if multiplier actually changed
        if (Math.abs(newMultiplier - oldMultiplier) > 0.0001) {
            FatigueAppliedEvent event = new FatigueAppliedEvent(chunkPos, oldMultiplier, newMultiplier, xpAmount);
            MinecraftForge.EVENT_BUS.post(event);
        }
    }

    /**
     * Ticks fatigue recovery for all chunks in a level.
     * Called by FatigueHandler from the level tick event.
     * Recovery happens naturally over time, encouraging players to return
     * to areas after they've rested.
     *
     * @param level The server level
     */
    public void tickRecovery(ServerLevel level) {
        Objects.requireNonNull(level, "ServerLevel cannot be null");
        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        fatigueData.tick(level);
    }
}
