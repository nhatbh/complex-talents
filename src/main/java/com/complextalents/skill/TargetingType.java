package com.complextalents.skill;

import net.minecraft.util.StringRepresentable;

/**
 * Defines what kind of targeting an active skill requires.
 * This determines what data is included in the targeting snapshot
 * and how the server resolves targets.
 */
public enum TargetingType implements StringRepresentable {
    /**
     * No targeting required.
     * Skills execute on the caster or without any target.
     * Example: Self-buffs, movement skills, AoE around caster.
     */
    NONE("none"),

    /**
     * Uses aim direction only.
     * Provides direction vector from player's look.
     * Target position is at max range in that direction.
     * Example: Projectile skills, dash skills, cone attacks.
     */
    DIRECTION("direction"),

    /**
     * Uses a specific world position.
     * Provides exact block/entity hit position.
     * Example: Ground-targeted AoE, teleport, placeable objects.
     */
    POSITION("position"),

    /**
     * Targets an entity.
     * If an entity is hit, provides that entity.
     * Falls back to caster if no valid entity target.
     * Example: Single-target spells, heals, debuffs.
     */
    ENTITY("entity");

    private final String name;

    TargetingType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
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
        return this == POSITION || this == DIRECTION;
    }
}
