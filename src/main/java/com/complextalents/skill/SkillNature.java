package com.complextalents.skill;

import net.minecraft.util.StringRepresentable;

/**
 * Defines how a skill behaves and when it activates.
 */
public enum SkillNature implements StringRepresentable {
    /**
     * Always active, event-driven skills.
     * Never send packets, never require targeting.
     * React to combat events, player tick, skill executions, etc.
     */
    PASSIVE("passive"),

    /**
     * Castable skills activated by player input.
     * Send targeting snapshot to server for execution.
     */
    ACTIVE("active"),

    /**
     * Skills with both passive effects and active castability.
     * Passive effects run server-side as event listeners.
     * Active casting uses client targeting and server execution.
     */
    BOTH("both"),

    TOGGLE("toggle"),

    /**
     * Skill that has a start-charge phase and an end-release phase.
     * Pressing starts charging, releasing triggers the final effect.
     */
    CHARGE("charge");

    private final String name;

    SkillNature(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * @return true if this skill has passive components
     */
    public boolean hasPassive() {
        return this == PASSIVE || this == BOTH;
    }

    /**
     * @return true if this skill can be actively cast
     */
    public boolean hasActive() {
        return this == ACTIVE || this == BOTH || this == TOGGLE || this == CHARGE;
    }

    /**
     * @return true if this skill is toggleable
     */
    public boolean isToggle() {
        return this == TOGGLE;
    }
}
