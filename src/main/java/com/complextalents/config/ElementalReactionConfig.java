package com.complextalents.config;

import com.complextalents.elemental.ElementalReaction;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive configuration for the Elemental Reaction System
 * All values are tunable for balance adjustments
 */
public class ElementalReactionConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ===== Core System Settings =====
    public static ForgeConfigSpec.BooleanValue enableFriendlyFireProtection;

    // ===== Core Reaction Multipliers =====
    public static final Map<ElementalReaction, ForgeConfigSpec.DoubleValue> reactionMultipliers = new HashMap<>();

    // ===== Mastery Scaling Constants =====
    public static ForgeConfigSpec.DoubleValue generalMasteryScaling;
    public static ForgeConfigSpec.DoubleValue specificMasteryScaling;

    // ===== Duration Values =====
    public static ForgeConfigSpec.IntValue stackDecayTicks;
    public static ForgeConfigSpec.IntValue maxStackCount;

    // Vaporize
    public static ForgeConfigSpec.IntValue vaporizeSteamCloudDuration;
    public static ForgeConfigSpec.DoubleValue vaporizeRangedMissChance;
    public static ForgeConfigSpec.DoubleValue vaporizeSteamCloudRadius;

    // Melt
    public static ForgeConfigSpec.IntValue meltFrostbiteDuration;
    public static ForgeConfigSpec.DoubleValue meltArmorReduction;

    // Overloaded
    public static ForgeConfigSpec.DoubleValue overloadedAoeRadius;
    public static ForgeConfigSpec.DoubleValue overloadedKnockbackStrength;

    // Electro-Charged
    public static ForgeConfigSpec.IntValue electroChargedDuration;
    public static ForgeConfigSpec.IntValue electroChargedTickRate;

    // Frozen
    public static ForgeConfigSpec.IntValue frozenMinDuration;
    public static ForgeConfigSpec.IntValue frozenMaxDuration;
    public static ForgeConfigSpec.DoubleValue frozenShatterMultiplier;

    // Superconduct
    public static ForgeConfigSpec.IntValue superconductDuration;
    public static ForgeConfigSpec.DoubleValue superconductResistanceReduction;
    public static ForgeConfigSpec.DoubleValue superconductAoeRadius;

    // Burning
    public static ForgeConfigSpec.IntValue burningDuration;
    public static ForgeConfigSpec.IntValue burningTickRate;
    public static ForgeConfigSpec.DoubleValue burningPanicChance;

    // Bloom
    public static ForgeConfigSpec.IntValue bloomCoreDuration;

    // Hyperbloom
    public static ForgeConfigSpec.IntValue hyperbloomProjectileCount;
    public static ForgeConfigSpec.DoubleValue hyperbloomTargetRadius;
    public static ForgeConfigSpec.IntValue hyperbloomVulnerableDuration;
    public static ForgeConfigSpec.DoubleValue hyperbloomVulnerableAmplification;

    // Burgeon
    public static ForgeConfigSpec.DoubleValue burgeonAoeRadius;
    public static ForgeConfigSpec.IntValue burgeonZoneDuration;
    public static ForgeConfigSpec.DoubleValue burgeonSlowPercentage;

    // Ender Reactions
    public static ForgeConfigSpec.IntValue unstableWardDuration;
    public static ForgeConfigSpec.DoubleValue unstableWardResistance;
    public static ForgeConfigSpec.DoubleValue unstableWardDamageAmp;
    public static ForgeConfigSpec.IntValue unstableWardSpellsToDetonate;

    public static ForgeConfigSpec.DoubleValue riftPullDistance;
    public static ForgeConfigSpec.IntValue riftPullInstabilityDuration;
    public static ForgeConfigSpec.DoubleValue riftPullSpeedReduction;

    public static ForgeConfigSpec.IntValue singularityDuration;
    public static ForgeConfigSpec.DoubleValue singularityRadius;
    public static ForgeConfigSpec.DoubleValue singularityPullStrength;

    public static ForgeConfigSpec.IntValue fractureDuration;
    public static ForgeConfigSpec.DoubleValue fractureIgnoreChance;
    public static ForgeConfigSpec.DoubleValue fractureAmplifyChance;
    public static ForgeConfigSpec.DoubleValue fractureAmplifyMultiplier;

    public static ForgeConfigSpec.IntValue witheringSeedDuration;
    public static ForgeConfigSpec.DoubleValue witheringSeedDamageReduction;
    public static ForgeConfigSpec.DoubleValue witheringSeedLifeSteal;

    public static ForgeConfigSpec.IntValue decrepitGraspDuration;
    public static ForgeConfigSpec.DoubleValue decrepitGraspAttackSpeedReduction;

    // ===== General Settings =====
    public static ForgeConfigSpec.BooleanValue enableElementalSystem;
    public static ForgeConfigSpec.BooleanValue enableParticleEffects;
    public static ForgeConfigSpec.BooleanValue enableSoundEffects;
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;

    static {
        BUILDER.comment("====================================")
               .comment("Elemental Reaction System Configuration")
               .comment("====================================");

        // Core Reaction Multipliers
        BUILDER.comment("Core Reaction Damage Multipliers").push("multipliers");

        reactionMultipliers.put(ElementalReaction.VAPORIZE,
            BUILDER.comment("Vaporize (Fire + Aqua) base multiplier")
                   .defineInRange("vaporize", 2.0, 0.1, 10.0));
        reactionMultipliers.put(ElementalReaction.MELT,
            BUILDER.comment("Melt (Fire + Ice) base multiplier")
                   .defineInRange("melt", 2.0, 0.1, 10.0));
        reactionMultipliers.put(ElementalReaction.OVERLOADED,
            BUILDER.comment("Overloaded (Fire + Lightning) base multiplier")
                   .defineInRange("overloaded", 1.2, 0.1, 10.0));
        reactionMultipliers.put(ElementalReaction.ELECTRO_CHARGED,
            BUILDER.comment("Electro-Charged (Aqua + Lightning) damage per tick")
                   .defineInRange("electroCharged", 0.3, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.FROZEN,
            BUILDER.comment("Frozen (Aqua + Ice) base multiplier")
                   .defineInRange("frozen", 1.0, 0.0, 5.0));
        reactionMultipliers.put(ElementalReaction.SUPERCONDUCT,
            BUILDER.comment("Superconduct (Ice + Lightning) base multiplier")
                   .defineInRange("superconduct", 0.8, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.BURNING,
            BUILDER.comment("Burning (Nature + Fire) damage per tick")
                   .defineInRange("burning", 0.5, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.BLOOM,
            BUILDER.comment("Bloom (Nature + Aqua) base multiplier")
                   .defineInRange("bloom", 1.0, 0.0, 5.0));
        reactionMultipliers.put(ElementalReaction.HYPERBLOOM,
            BUILDER.comment("Hyperbloom (Bloom + Lightning) per projectile")
                   .defineInRange("hyperbloom", 1.5, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.BURGEON,
            BUILDER.comment("Burgeon (Bloom + Fire) AoE multiplier")
                   .defineInRange("burgeon", 2.0, 0.1, 10.0));

        // Ender reactions
        reactionMultipliers.put(ElementalReaction.RIFT_PULL,
            BUILDER.comment("Rift Pull (Ender + Lightning) damage multiplier")
                   .defineInRange("riftPull", 1.2, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.SINGULARITY,
            BUILDER.comment("Singularity (Ender + Fire) DoT per second")
                   .defineInRange("singularity", 0.1, 0.0, 2.0));
        reactionMultipliers.put(ElementalReaction.FRACTURE,
            BUILDER.comment("Fracture (Ender + Ice) base multiplier")
                   .defineInRange("fracture", 1.0, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.DECREPIT_GRASP,
            BUILDER.comment("Decrepit Grasp (Ender + Aqua) damage multiplier")
                   .defineInRange("decrepitGrasp", 0.8, 0.1, 5.0));

        BUILDER.pop();

        // Mastery Scaling
        BUILDER.comment("Mastery Attribute Scaling").push("mastery");

        generalMasteryScaling = BUILDER
            .comment("Scaling constant for General Elemental Mastery")
            .comment("Formula: bonus = mastery / (mastery + constant)")
            .defineInRange("generalScaling", 100.0, 10.0, 1000.0);

        specificMasteryScaling = BUILDER
            .comment("Scaling constant for Specific Mastery (Fire, Aqua, etc.)")
            .comment("Lower values = more aggressive scaling")
            .defineInRange("specificScaling", 50.0, 10.0, 500.0);

        BUILDER.pop();

        // Stack Settings
        BUILDER.comment("Elemental Stack Settings").push("stacks");

        stackDecayTicks = BUILDER
            .comment("Ticks before elemental stacks expire (20 ticks = 1 second)")
            .defineInRange("decayTicks", 300, 20, 6000);

        maxStackCount = BUILDER
            .comment("Maximum stacks per element on an entity")
            .defineInRange("maxStacks", 2, 1, 10);

        BUILDER.pop();

        // Reaction-Specific Settings
        BUILDER.comment("Vaporize Reaction Settings").push("vaporize");
        vaporizeSteamCloudDuration = BUILDER.defineInRange("steamCloudDuration", 60, 1, 600);
        vaporizeRangedMissChance = BUILDER.defineInRange("rangedMissChance", 0.25, 0.0, 1.0);
        vaporizeSteamCloudRadius = BUILDER.defineInRange("steamCloudRadius", 3.0, 0.5, 10.0);
        BUILDER.pop();

        BUILDER.comment("Melt Reaction Settings").push("melt");
        meltFrostbiteDuration = BUILDER.defineInRange("frostbiteDuration", 120, 1, 600);
        meltArmorReduction = BUILDER.defineInRange("armorReduction", 0.15, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Overloaded Reaction Settings").push("overloaded");
        overloadedAoeRadius = BUILDER.defineInRange("aoeRadius", 4.0, 1.0, 10.0);
        overloadedKnockbackStrength = BUILDER
            .comment("Knockback strength applied to targets (increased from 1.5 to 3.0 after removing Stagger)")
            .defineInRange("knockbackStrength", 3.0, 0.1, 10.0);
        BUILDER.pop();

        BUILDER.comment("Electro-Charged Reaction Settings").push("electroCharged");
        electroChargedDuration = BUILDER.defineInRange("duration", 200, 20, 1200);
        electroChargedTickRate = BUILDER.defineInRange("tickRate", 20, 1, 100);
        BUILDER.pop();

        BUILDER.comment("Frozen Reaction Settings").push("frozen");
        frozenMinDuration = BUILDER.defineInRange("minDuration", 30, 1, 200);
        frozenMaxDuration = BUILDER.defineInRange("maxDuration", 90, 1, 600);
        frozenShatterMultiplier = BUILDER.defineInRange("shatterMultiplier", 1.5, 1.0, 5.0);
        BUILDER.pop();

        BUILDER.comment("Superconduct Reaction Settings").push("superconduct");
        superconductDuration = BUILDER.defineInRange("duration", 240, 20, 1200);
        superconductResistanceReduction = BUILDER.defineInRange("resistanceReduction", 0.40, 0.0, 1.0);
        superconductAoeRadius = BUILDER.defineInRange("aoeRadius", 3.0, 1.0, 10.0);
        BUILDER.pop();

        BUILDER.comment("Burning Reaction Settings").push("burning");
        burningDuration = BUILDER.defineInRange("duration", 160, 20, 1200);
        burningTickRate = BUILDER.defineInRange("tickRate", 20, 1, 100);
        burningPanicChance = BUILDER.defineInRange("panicChance", 0.30, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Bloom Reaction Settings").push("bloom");
        bloomCoreDuration = BUILDER.defineInRange("coreDuration", 120, 20, 600);
        BUILDER.pop();

        BUILDER.comment("Hyperbloom Reaction Settings").push("hyperbloom");
        hyperbloomProjectileCount = BUILDER.defineInRange("projectileCount", 4, 1, 10);
        hyperbloomTargetRadius = BUILDER.defineInRange("targetRadius", 8.0, 1.0, 32.0);
        hyperbloomVulnerableDuration = BUILDER.defineInRange("vulnerableDuration", 100, 20, 600);
        hyperbloomVulnerableAmplification = BUILDER.defineInRange("vulnerableAmplification", 0.20, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Burgeon Reaction Settings").push("burgeon");
        burgeonAoeRadius = BUILDER.defineInRange("aoeRadius", 6.0, 1.0, 15.0);
        burgeonZoneDuration = BUILDER.defineInRange("zoneDuration", 80, 20, 600);
        burgeonSlowPercentage = BUILDER.defineInRange("slowPercentage", 0.30, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Unstable Ward Reaction Settings").push("unstableWard");
        unstableWardDuration = BUILDER.defineInRange("duration", 160, 20, 600);
        unstableWardResistance = BUILDER.defineInRange("resistance", 0.60, 0.0, 1.0);
        unstableWardDamageAmp = BUILDER.defineInRange("damageAmplification", 0.20, 0.0, 2.0);
        unstableWardSpellsToDetonate = BUILDER.defineInRange("spellsToDetonate", 3, 1, 10);
        BUILDER.pop();

        BUILDER.comment("Rift Pull Reaction Settings").push("riftPull");
        riftPullDistance = BUILDER.defineInRange("pullDistance", 8.0, 1.0, 32.0);
        riftPullInstabilityDuration = BUILDER.defineInRange("instabilityDuration", 40, 10, 200);
        riftPullSpeedReduction = BUILDER.defineInRange("speedReduction", 0.50, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Singularity Reaction Settings").push("singularity");
        singularityDuration = BUILDER.defineInRange("duration", 120, 20, 600);
        singularityRadius = BUILDER.defineInRange("radius", 5.0, 1.0, 15.0);
        singularityPullStrength = BUILDER.defineInRange("pullStrength", 0.3, 0.1, 2.0);
        BUILDER.pop();

        BUILDER.comment("Fracture Reaction Settings").push("fracture");
        fractureDuration = BUILDER.defineInRange("duration", 100, 20, 600);
        fractureIgnoreChance = BUILDER.defineInRange("ignoreChance", 0.25, 0.0, 1.0);
        fractureAmplifyChance = BUILDER.defineInRange("amplifyChance", 0.25, 0.0, 1.0);
        fractureAmplifyMultiplier = BUILDER.defineInRange("amplifyMultiplier", 1.25, 1.0, 3.0);
        BUILDER.pop();

        BUILDER.comment("Withering Seed Reaction Settings").push("witheringSeed");
        witheringSeedDuration = BUILDER.defineInRange("duration", 200, 20, 1200);
        witheringSeedDamageReduction = BUILDER.defineInRange("damageReduction", 0.15, 0.0, 1.0);
        witheringSeedLifeSteal = BUILDER.defineInRange("lifeSteal", 0.5, 0.0, 2.0);
        BUILDER.pop();

        BUILDER.comment("Decrepit Grasp Reaction Settings").push("decrepitGrasp");
        decrepitGraspDuration = BUILDER.defineInRange("duration", 160, 20, 1200);
        decrepitGraspAttackSpeedReduction = BUILDER.defineInRange("attackSpeedReduction", 0.30, 0.0, 1.0);
        BUILDER.pop();

        // General Settings
        BUILDER.comment("General System Settings").push("general");

        enableElementalSystem = BUILDER
            .comment("Enable/disable the entire elemental reaction system")
            .define("enableSystem", true);

        enableFriendlyFireProtection = BUILDER
            .comment("Prevent reactions from harming teammates (uses Minecraft team system)")
            .define("enableFriendlyFireProtection", true);

        enableParticleEffects = BUILDER
            .comment("Show particle effects for reactions and stacks")
            .define("enableParticles", true);

        enableSoundEffects = BUILDER
            .comment("Play sound effects for reactions")
            .define("enableSounds", true);

        enableDebugLogging = BUILDER
            .comment("Enable debug logging for reaction triggers")
            .define("debugLogging", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static double getReactionMultiplier(ElementalReaction reaction) {
        ForgeConfigSpec.DoubleValue value = reactionMultipliers.get(reaction);
        return value != null ? value.get() : reaction.getBaseMultiplier();
    }
}
