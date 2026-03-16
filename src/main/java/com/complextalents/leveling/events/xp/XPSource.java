package com.complextalents.leveling.events.xp;

/**
 * Enum representing different sources of XP awards.
 * Provides type-safe tracking of XP sources throughout the system.
 */
public enum XPSource {
    PRIMARY_COMBAT("Primary Combat"),
    ASSASSIN_AMBUSH("Assassin Ambush"),
    ASSASSIN_GHOST("Assassin Ghost"),
    DARKMAGE_SOUL_HOARDER("Dark Mage Soul Hoarder"),
    DARKMAGE_EDGE("Dark Mage Edge of Death"),
    ELEMENTAL_MASTER("Elemental Master"),
    WARRIOR_MOMENTUM("Warrior Momentum"),
    WARRIOR_PARRY("Warrior Parry"),
    HIGHPRIEST_SAVIOR("High Priest Savior"),
    HIGHPRIEST_CROWD_CONTROL("High Priest Crowd Control");

    private final String displayName;

    XPSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
