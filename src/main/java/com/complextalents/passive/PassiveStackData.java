package com.complextalents.passive;

/**
 * Runtime data for a single passive stack on a player.
 * Tracks the current stack count.
 * <p>
 * Note: Tick tracking is NOT stored here - the owner's event handler
 * manages its own timing logic using game time.
 * </p>
 */
public class PassiveStackData {

    private final String stackTypeId;
    private int currentStacks;

    /**
     * Create new passive stack data.
     *
     * @param stackTypeId The stack type identifier
     */
    public PassiveStackData(String stackTypeId) {
        this.stackTypeId = stackTypeId;
        this.currentStacks = 0;
    }

    /**
     * Create new passive stack data with initial value.
     *
     * @param stackTypeId The stack type identifier
     * @param initialStacks Initial stack count
     */
    public PassiveStackData(String stackTypeId, int initialStacks) {
        this.stackTypeId = stackTypeId;
        this.currentStacks = Math.max(0, initialStacks);
    }

    /**
     * Get the current stack count.
     *
     * @return Current stacks
     */
    public int getStacks() {
        return currentStacks;
    }

    /**
     * Set the stack count.
     * Clamped to 0 or greater (max clamping happens at capability level).
     *
     * @param stacks New stack count
     */
    public void setStacks(int stacks) {
        this.currentStacks = Math.max(0, stacks);
    }

    /**
     * Add stacks to the current count.
     *
     * @param amount Amount to add (can be negative)
     * @return The new stack count
     */
    public int addStacks(int amount) {
        this.currentStacks = Math.max(0, currentStacks + amount);
        return currentStacks;
    }

    /**
     * Remove stacks from the current count.
     *
     * @param amount Amount to remove
     * @return The new stack count
     */
    public int removeStacks(int amount) {
        this.currentStacks = Math.max(0, currentStacks - amount);
        return currentStacks;
    }

    /**
     * Reset stacks to zero.
     */
    public void reset() {
        this.currentStacks = 0;
    }

    /**
     * Get the stack type identifier.
     *
     * @return The stack type ID
     */
    public String getStackTypeId() {
        return stackTypeId;
    }
}
