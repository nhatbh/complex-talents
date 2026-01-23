package com.complextalents.talent;

/**
 * Defines the 5 specific talent slot types for players.
 * Each player has one slot of each type.
 */
public enum TalentSlotType {
    /**
     * Definition talent - Defines the core playstyle
     */
    DEFINITION("Definition", "Defines your core playstyle"),

    /**
     * Harmony talent - Enhances the playstyle mechanic
     */
    HARMONY("Harmony", "Enhances your playstyle mechanics"),

    /**
     * Crescendo talent - Activates powerful effects from core mechanics
     */
    CRESCENDO("Crescendo", "Activates powerful effects from core mechanics"),

    /**
     * Resonance talent - Uses the mechanic defensively
     */
    RESONANCE("Resonance", "Uses your mechanics defensively"),

    /**
     * Finale talent - A powerful ability that brings the most out of core mechanics
     */
    FINALE("Finale", "A powerful ability maximizing your core mechanics");

    private final String displayName;
    private final String description;

    TalentSlotType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
