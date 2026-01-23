package com.complextalents.elemental.superreaction;

/**
 * Defines the tiers of Super-Reactions based on unique element count
 */
public enum SuperReactionTier {
    NONE(0, "None", 0f),
    TIER_1(3, "Basic", 1.0f),      // 3 unique elements
    TIER_2(4, "Enhanced", 1.5f),   // 4 unique elements
    TIER_3(5, "Superior", 2.0f),   // 5 unique elements
    TIER_4(6, "Ultimate", 3.0f);   // 6 unique elements (all)

    private final int requiredElements;
    private final String displayName;
    private final float damageMultiplier;

    SuperReactionTier(int requiredElements, String displayName, float damageMultiplier) {
        this.requiredElements = requiredElements;
        this.displayName = displayName;
        this.damageMultiplier = damageMultiplier;
    }

    public int getRequiredElements() {
        return requiredElements;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Get the tier based on unique element count
     */
    public static SuperReactionTier fromElementCount(int uniqueCount) {
        if (uniqueCount >= 6) return TIER_4;
        if (uniqueCount >= 5) return TIER_3;
        if (uniqueCount >= 4) return TIER_2;
        if (uniqueCount >= 3) return TIER_1;
        return NONE;
    }

    /**
     * Check if this tier can trigger
     */
    public boolean canTrigger() {
        return this != NONE;
    }

    /**
     * Get mastery scaling factor for this tier
     * Used in formula: Final_Effect = Base_Effect * (1 + Scaling_Factor * (Mastery - 1))
     */
    public float getScalingFactor() {
        return switch (this) {
            case TIER_1 -> 0.25f;
            case TIER_2 -> 0.50f;
            case TIER_3 -> 0.75f;
            case TIER_4 -> 1.00f;
            default -> 0f;
        };
    }
}