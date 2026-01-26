package com.complextalents.elemental;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a single elemental stack applied to an entity.
 * Only one stack per element type is allowed - stacking logic has been removed.
 */
public class ElementStack {
    private final ElementType element;
    private final long appliedTick;
    private final LivingEntity entity;
    @Nullable
    private final UUID sourceId;

    public ElementStack(ElementType element, LivingEntity entity, @Nullable LivingEntity source) {
        this.element = element;
        this.entity = entity;
        this.sourceId = source != null ? source.getUUID() : null;
        this.appliedTick = entity.level().getGameTime();
    }

    public ElementType getElement() {
        return element;
    }

    /**
     * Gets the tick when this stack was applied.
     *
     * @return The applied tick
     */
    public long getAppliedTick() {
        return appliedTick;
    }

    /**
     * @deprecated Use getAppliedTick() instead. Stacking logic has been removed.
     */
    @Deprecated
    public long getLastAppliedTick() {
        return appliedTick;
    }

    /**
     * @deprecated Stacking logic has been removed. Always returns 1.
     */
    @Deprecated
    public int getStackCount() {
        return 1;
    }

    @Nullable
    public UUID getSourceId() {
        return sourceId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * @deprecated Stacking logic has been removed. Use refresh() instead.
     */
    @Deprecated
    public void addStack() {
        // No-op - stacking removed
    }

    /**
     * @deprecated Stacking logic has been removed.
     */
    @Deprecated
    public void removeStack() {
        // No-op - stacking removed
    }

    /**
     * @deprecated Stacking logic has been removed.
     */
    @Deprecated
    public void setStackCount(int count) {
        // No-op - stacking removed
    }

    /**
     * Refreshes the stack's applied tick to current time.
     * This is used when reapplying the same element.
     */
    public void refresh() {
        // Note: Since ElementStack is immutable, this would require creating a new stack
        // This method is kept for API compatibility but doesn't modify this instance
    }

    /**
     * Checks if this stack has expired based on the decay ticks.
     *
     * @param decayTicks The number of ticks before a stack expires
     * @return true if the stack has expired
     */
    public boolean isExpired(long decayTicks) {
        long currentTick = entity.level().getGameTime();
        return (currentTick - appliedTick) > decayTicks;
    }

    /**
     * Gets the time remaining before this stack expires.
     *
     * @param decayTicks The number of ticks before a stack expires
     * @return The number of ticks remaining
     */
    public long getTimeUntilExpiry(long decayTicks) {
        long currentTick = entity.level().getGameTime();
        long elapsed = currentTick - appliedTick;
        return Math.max(0, decayTicks - elapsed);
    }
}
