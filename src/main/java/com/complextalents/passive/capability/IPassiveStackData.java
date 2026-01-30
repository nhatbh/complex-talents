package com.complextalents.passive.capability;

import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

/**
 * Capability interface for player passive stack data.
 * Stores passive stack counts that can be used by both origins and skills.
 */
public interface IPassiveStackData extends INBTSerializable<net.minecraft.nbt.CompoundTag> {

    /**
     * Get all passive stacks for the player.
     *
     * @return Map of stack type name -> current stack count
     */
    Map<String, Integer> getPassiveStacks();

    /**
     * Get the current stack count for a specific passive stack type.
     *
     * @param stackTypeName The stack type name (e.g., "grace")
     * @return The current stack count, or 0 if not found
     */
    int getPassiveStackCount(String stackTypeName);

    /**
     * Set the stack count for a specific passive stack type.
     * The value is clamped to the stack type's max stacks.
     * Triggers a sync if the value changes.
     *
     * @param stackTypeName The stack type name
     * @param count The new stack count
     */
    void setPassiveStacks(String stackTypeName, int count);

    /**
     * Modify the stack count for a specific passive stack type.
     * The final value is clamped to the stack type's max stacks.
     * Triggers a sync if the value changes.
     *
     * @param stackTypeName The stack type name
     * @param delta The amount to add (can be negative)
     */
    void modifyPassiveStacks(String stackTypeName, int delta);

    /**
     * Reset all passive stacks to zero.
     * Triggers a sync.
     */
    void resetPassiveStacks();

    /**
     * Sync the passive stack data to the client.
     */
    void sync();
}
