package com.complextalents.passive;

import net.minecraft.resources.ResourceLocation;

/**
 * Interface for entities that can own passive stack definitions.
 * Both origins and skills can implement this.
 */
public interface PassiveOwner {

    /**
     * Get the unique identifier for this owner.
     *
     * @return The owner ID (e.g., origin ID or skill ID)
     */
    ResourceLocation getOwnerId();

    /**
     * Get the type of owner (for logging/debugging).
     *
     * @return The owner type (e.g., "origin" or "skill")
     */
    default String getOwnerType() {
        return "unknown";
    }
}
