package com.complextalents.targeting;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

/**
 * Defines what kinds of targets are allowed/resolved by the targeting system.
 * Each type represents a different targeting mode that skills can use.
 */
public enum TargetType implements StringRepresentable {
    /**
     * Target a specific entity (player, mob, etc.)
     */
    ENTITY("entity"),

    /**
     * Target a specific position in the world (block or air)
     */
    POSITION("position"),

    /**
     * Target a direction (look vector) without a specific endpoint
     */
    DIRECTION("direction");

    private final String name;

    TargetType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Check if this target type is valid for the given target data.
     *
     * @param hasEntity Whether an entity is present
     * @param hasPosition Whether a position is present
     * @return true if this target type can represent the given data
     */
    public boolean isValidFor(boolean hasEntity, boolean hasPosition) {
        return switch (this) {
            case ENTITY -> hasEntity;
            case POSITION -> hasPosition;
            case DIRECTION -> true; // Direction is always valid (uses aim direction)
        };
    }
}
