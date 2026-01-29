package com.complextalents.targeting;

import net.minecraft.util.StringRepresentable;

/**
 * Filter for target relationships.
 * Used to restrict targeting to allies, enemies, or both.
 */
public enum TargetRelation implements StringRepresentable {
    /**
     * Target anyone (ally, enemy, or neutral)
     */
    ANY("any"),

    /**
     * Only target allies (same team, party, faction)
     */
    ALLY("ally"),

    /**
     * Only target enemies (opposing teams, hostile mobs)
     */
    ENEMY("enemy");

    private final String name;

    TargetRelation(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Check if a target with the given ally status matches this relation filter.
     *
     * @param isAlly true if the target is considered an ally
     * @return true if the target matches this relation filter
     */
    public boolean matches(boolean isAlly) {
        return switch (this) {
            case ANY -> true;
            case ALLY -> isAlly;
            case ENEMY -> !isAlly;
        };
    }
}
