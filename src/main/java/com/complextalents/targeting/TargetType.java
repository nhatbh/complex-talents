package com.complextalents.targeting;

import net.minecraft.util.StringRepresentable;

/**
 * Defines what kinds of targets are allowed/resolved by the targeting system.
 * Each type represents a different targeting mode that skills can use.
 *
 * <p>This is the single source of truth for targeting types used by both
 * the targeting system and the skill system.</p>
 */
public enum TargetType implements StringRepresentable {
    /**
     * No targeting required.
     * Skills execute on the caster or without any target.
     * Example: Self-buffs, movement skills, AoE around caster.
     */
    NONE("none"),

    /**
     * Target a specific entity (player, mob, etc.)
     * Example: Single-target spells, heals, debuffs.
     */
    ENTITY("entity"),

    /**
     * Target a specific position in the world (block or air)
     * Example: Ground-targeted AoE, teleport, placeable objects.
     */
    POSITION("position"),

    /**
     * Target a direction (look vector) without a specific endpoint.
     * Provides direction vector from player's look.
     * Example: Projectile skills, dash skills, cone attacks.
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
            case NONE -> true;
            case ENTITY -> hasEntity;
            case POSITION -> hasPosition;
            case DIRECTION -> true; // Direction is always valid (uses aim direction)
        };
    }

    /**
     * @return true if this targeting type requires raycasting
     */
    public boolean requiresRaycast() {
        return this != NONE;
    }

    /**
     * @return true if this targeting type can target entities
     */
    public boolean canTargetEntity() {
        return this == ENTITY;
    }

    /**
     * @return true if this targeting type provides position data
     */
    public boolean providesPosition() {
        return this == POSITION || this == DIRECTION || this == NONE;
    }
}
