package com.complextalents.elemental;

/**
 * Elemental reactions that can occur when different elements combine.
 * The actual implementation logic is handled by IReactionStrategy implementations.
 */
public enum ElementalReaction {
    // Core Amplifying Reactions
    VAPORIZE,
    MELT,
    OVERLOADED,
    BURNING,
    VOIDFIRE,

    // Ice Reactions
    FREEZE,        // Ice + Aqua - Encases in ice, physical hits deal 2.5x damage
    SUPERCONDUCT,  // Ice + Lightning - Armor corrosion, reduces armor by 50%
    PERMAFROST,    // Ice + Nature - Roots target, prevents movement
    FRACTURE,      // Ice + Ender - Shatter defenses, sets armor to 0 for 3 hits

    // Aqua Reactions
    ELECTRO_CHARGED, // Aqua + Lightning - Chain lightning that zaps 3 nearby enemies
}