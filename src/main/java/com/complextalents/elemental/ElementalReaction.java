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
    SPRING,          // Aqua + Ender - Spawns buff potion on pickup

    // Nature Reactions
    BLOOM,           // Aqua + Nature - Spawns Nature Core that explodes with Fire/Lightning

    // Lightning Reactions
    FLUX,            // Lightning + Ender - Spawns Black Hole singularity that pulls entities and implodes
    OVERGROWTH,      // Lightning + Nature - Afflicts Unstable Bio-energy, target explodes on death
}