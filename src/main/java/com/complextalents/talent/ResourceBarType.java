package com.complextalents.talent;

/**
 * Defines different types of resource bars with their characteristics
 */
public enum ResourceBarType {
    /**
     * Mana - Regenerates over time, used for casting spells
     */
    MANA(0x4488FF, true, 1.0f),

    /**
     * Rage - Builds through combat, decays when out of combat
     */
    RAGE(0xFF4444, false, -0.5f),

    /**
     * Energy - Fast regeneration, used for quick abilities
     */
    ENERGY(0xFFDD44, true, 3.0f),

    /**
     * Heat - Builds when using fire abilities, decays over time
     */
    HEAT(0xFF8800, false, -1.0f),

    /**
     * Chi - Balanced resource, moderate regeneration
     */
    CHI(0x88FF88, true, 0.5f),

    /**
     * Corruption - Builds when using dark powers, dangerous at high levels
     */
    CORRUPTION(0x8800FF, false, -0.2f),

    /**
     * Combo Points - Builds through attacks, spent on finishers, no natural regen
     */
    COMBO(0xFFAA00, false, 0.0f),

    /**
     * Charges - Builds over time, can store multiple charges
     */
    CHARGES(0x00FFFF, true, 0.1f),

    /**
     * Focus - Generated from elemental reactions, decays after inactivity
     * Used by Elemental Mage for powerful abilities
     */
    FOCUS(0x6699FF, false, 0.0f),

    /**
     * Custom - Fully customizable resource type
     */
    CUSTOM(0xFFFFFF, false, 0.0f);

    private final int defaultColor;
    private final boolean regenerates;
    private final float defaultRegenRate; // Per second

    ResourceBarType(int defaultColor, boolean regenerates, float defaultRegenRate) {
        this.defaultColor = defaultColor;
        this.regenerates = regenerates;
        this.defaultRegenRate = defaultRegenRate;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public boolean regenerates() {
        return regenerates;
    }

    /**
     * Get the default regeneration rate in resource units per second
     * Positive = regeneration, Negative = decay
     */
    public float getDefaultRegenRate() {
        return defaultRegenRate;
    }
}
