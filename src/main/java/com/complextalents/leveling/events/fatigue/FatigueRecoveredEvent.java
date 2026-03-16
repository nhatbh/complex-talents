package com.complextalents.leveling.events.fatigue;

import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when chunk fatigue recovers (tick-based).
 * This event is NOT cancelable - the recovery has already been applied.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>Sync fatigue data to clients</li>
 *   <li>Track statistics</li>
 * </ul>
 *
 * <p>This event is immutable - all fields are final and read-only.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 *
 * @see FatigueAppliedEvent
 */
public class FatigueRecoveredEvent extends Event {
    private final ChunkPos chunkPos;
    private final double oldMultiplier;
    private final double newMultiplier;

    /**
     * Creates a new FatigueRecoveredEvent.
     *
     * @param chunkPos The chunk where recovery was applied
     * @param oldMultiplier The fatigue multiplier before recovery
     * @param newMultiplier The fatigue multiplier after recovery
     */
    public FatigueRecoveredEvent(ChunkPos chunkPos, double oldMultiplier, double newMultiplier) {
        this.chunkPos = chunkPos;
        this.oldMultiplier = oldMultiplier;
        this.newMultiplier = newMultiplier;
    }

    // Immutable getters
    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    /**
     * Gets the fatigue multiplier before recovery was applied.
     *
     * @return The old multiplier (0.0 to 1.0)
     */
    public double getOldMultiplier() {
        return oldMultiplier;
    }

    /**
     * Gets the fatigue multiplier after recovery was applied.
     *
     * @return The new multiplier (0.0 to 1.0)
     */
    public double getNewMultiplier() {
        return newMultiplier;
    }

    /**
     * Calculates the change in multiplier.
     * Will be positive (recovery) or zero.
     *
     * @return The change in multiplier
     */
    public double getMultiplierChange() {
        return newMultiplier - oldMultiplier;
    }

    @Override
    public String toString() {
        return String.format("FatigueRecoveredEvent{chunk=%d,%d, multiplier=%.3f->%.3f}",
                chunkPos.x, chunkPos.z, oldMultiplier, newMultiplier);
    }
}
