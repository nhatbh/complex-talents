package com.complextalents.elemental;

/**
 * Elemental reactions that can occur when different elements combine
 * Based on the Complex Talents Elemental Reaction System
 */
public enum ElementalReaction {
    // Core Amplifying Reactions
    VAPORIZE(2.0f, 0, ReactionType.AMPLIFYING, "Fire + Aqua: High burst damage with ranged miss debuff"),
    MELT(2.0f, 0, ReactionType.AMPLIFYING, "Fire + Ice: High burst damage with armor reduction"),
    OVERLOADED(1.2f, 10, ReactionType.AMPLIFYING, "Fire + Lightning: AoE damage with knockback"),

    // Core Sustain Reactions
    ELECTRO_CHARGED(0.3f, 200, ReactionType.DOT, "Aqua + Lightning: DoT with crit setup"),
    FROZEN(1.0f, 90, ReactionType.CROWD_CONTROL, "Aqua + Ice: Immobilization with shatter bonus"),
    SUPERCONDUCT(0.8f, 240, ReactionType.DEBUFF, "Ice + Lightning: Physical resistance reduction"),

    // Nature Reactions
    BURNING(0.5f, 160, ReactionType.DOT, "Nature + Fire: DoT with panic effect"),
    BLOOM(1.0f, 120, ReactionType.SPAWN, "Nature + Aqua: Spawns bloom core"),
    HYPERBLOOM(1.5f, 0, ReactionType.AMPLIFYING, "Bloom Core + Lightning: Tracking projectiles"),
    BURGEON(2.0f, 80, ReactionType.AMPLIFYING, "Bloom Core + Fire: Large AoE with zone"),

    // Ender Utility Reactions
    UNSTABLE_WARD(0.0f, 160, ReactionType.UTILITY, "Ender + Element: Collectible buff shard"),
    RIFT_PULL(1.2f, 40, ReactionType.UTILITY, "Ender + Lightning: Pull target with instability"),
    SINGULARITY(0.1f, 120, ReactionType.UTILITY, "Ender + Fire: Gravity well zone"),

    // Ender Debuff Reactions
    FRACTURE(1.0f, 100, ReactionType.DEBUFF, "Ender + Ice: Variable damage modifier"),
    WITHERING_SEED(0.0f, 200, ReactionType.DEBUFF, "Ender + Nature: Damage reduction with life siphon"),
    DECREPIT_GRASP(0.8f, 160, ReactionType.DEBUFF, "Ender + Aqua: Attack speed and heal prevention");

    private final float baseMultiplier;
    private final int effectDurationTicks;
    private final ReactionType type;
    private final String description;

    ElementalReaction(float baseMultiplier, int effectDurationTicks, ReactionType type, String description) {
        this.baseMultiplier = baseMultiplier;
        this.effectDurationTicks = effectDurationTicks;
        this.type = type;
        this.description = description;
    }

    public float getBaseMultiplier() {
        return baseMultiplier;
    }

    public int getEffectDurationTicks() {
        return effectDurationTicks;
    }

    public ReactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAmplifying() {
        return type == ReactionType.AMPLIFYING;
    }

    public boolean isDot() {
        return type == ReactionType.DOT;
    }

    public boolean isCrowdControl() {
        return type == ReactionType.CROWD_CONTROL;
    }

    public boolean isDebuff() {
        return type == ReactionType.DEBUFF;
    }

    public boolean isUtility() {
        return type == ReactionType.UTILITY;
    }

    public boolean isSpawn() {
        return type == ReactionType.SPAWN;
    }

    public boolean hasStatusEffect() {
        return effectDurationTicks > 0;
    }

    public enum ReactionType {
        AMPLIFYING,     // High burst damage
        DOT,            // Damage over time
        CROWD_CONTROL,  // Movement/action restriction
        DEBUFF,         // Negative status effects
        UTILITY,        // Buffs, zones, special effects
        SPAWN           // Spawns entities
    }
}
