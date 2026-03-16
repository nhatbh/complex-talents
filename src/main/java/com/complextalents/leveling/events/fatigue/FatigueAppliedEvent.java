package com.complextalents.leveling.events.fatigue;

import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when fatigue degradation is applied to a chunk.
 * This event is NOT cancelable - the degradation has already been applied.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>Sync fatigue data to clients</li>
 *   <li>Display notifications</li>
 *   <li>Track statistics</li>
 * </ul>
 *
 * <p>This event is immutable - all fields are final and read-only.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 *
 * @see FatigueRecoveredEvent
 */
public class FatigueAppliedEvent extends Event {
    private final ChunkPos chunkPos;
    private final double oldMultiplier;
    private final double newMultiplier;
    private final double xpAmount; // Amount of XP that caused degradation

    /**
     * Creates a new FatigueAppliedEvent.
     *
     * @param chunkPos The chunk where fatigue was applied
     * @param oldMultiplier The fatigue multiplier before degradation
     * @param newMultiplier The fatigue multiplier after degradation
     * @param xpAmount The XP amount that caused the degradation
     */
    public FatigueAppliedEvent(ChunkPos chunkPos, double oldMultiplier, double newMultiplier, double xpAmount) {
        this.chunkPos = chunkPos;
        this.oldMultiplier = oldMultiplier;
        this.newMultiplier = newMultiplier;
        this.xpAmount = xpAmount;
    }

    // Immutable getters
    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    /**
     * Gets the fatigue multiplier before degradation was applied.
     *
     * @return The old multiplier (0.0 to 1.0)
     */
    public double getOldMultiplier() {
        return oldMultiplier;
    }

    /**
     * Gets the fatigue multiplier after degradation was applied.
     *
     * @return The new multiplier (0.0 to 1.0)
     */
    public double getNewMultiplier() {
        return newMultiplier;
    }

    /**
     * Calculates the change in multiplier.
     * Will be negative (degradation) or zero.
     *
     * @return The change in multiplier
     */
    public double getMultiplierChange() {
        return newMultiplier - oldMultiplier;
    }

    /**
     * Gets the amount of XP that caused this fatigue degradation.
     *
     * @return The XP amount
     */
    public double getXPAmount() {
        return xpAmount;
    }

    @Override
    public String toString() {
        return String.format("FatigueAppliedEvent{chunk=%d,%d, multiplier=%.3f->%.3f, xp=%.1f}",
                chunkPos.x, chunkPos.z, oldMultiplier, newMultiplier, xpAmount);
    }
}
