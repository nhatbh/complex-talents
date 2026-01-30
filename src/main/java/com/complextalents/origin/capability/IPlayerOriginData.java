package com.complextalents.origin.capability;

import com.complextalents.origin.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

/**
 * Capability interface for player origin data.
 * Mirrors IPlayerSkillData pattern.
 */
public interface IPlayerOriginData extends INBTSerializable<net.minecraft.nbt.CompoundTag> {

    /**
     * Get the player's active origin ID.
     *
     * @return The origin ID, or null if no origin is set
     */
    @Nullable
    ResourceLocation getActiveOrigin();

    /**
     * Set the player's active origin.
     *
     * @param originId The origin ID, or null to clear
     */
    void setActiveOrigin(@Nullable ResourceLocation originId);

    /**
     * Get the player's origin level.
     *
     * @return The origin level (1-maxLevel)
     */
    int getOriginLevel();

    /**
     * Set the player's origin level.
     *
     * @param level The new level (will be clamped to valid range)
     */
    void setOriginLevel(int level);

    /**
     * Get the resource type for the player's active origin.
     *
     * @return The resource type, or null if no origin or the origin has no resource
     */
    @Nullable
    ResourceType getResourceType();

    /**
     * Get the player's current resource value.
     *
     * @return The current resource value
     */
    double getResource();

    /**
     * Set the player's resource value.
     * The value is clamped to the resource type's min/max range.
     *
     * @param value The new value
     */
    void setResource(double value);

    /**
     * Modify the player's resource value.
     * The final value is clamped to the resource type's min/max range.
     * Triggers a sync if the value changes.
     *
     * @param delta The amount to add (can be negative)
     */
    void modifyResource(double delta);

    /**
     * Sync the origin data to the client.
     */
    void sync();

    /**
     * Clear all origin data (remove origin, reset resource).
     */
    void clear();

    /**
     * Called each server tick for passive effects.
     */
    void tick();
}
