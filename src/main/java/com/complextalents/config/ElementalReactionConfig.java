package com.complextalents.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for the Elemental Reaction System
 * Simplified for the strategy-based reaction system
 */
public class ElementalReactionConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ===== Core System Settings =====
    public static ForgeConfigSpec.BooleanValue enableElementalSystem;
    public static ForgeConfigSpec.BooleanValue enableFriendlyFireProtection;
    public static ForgeConfigSpec.BooleanValue enableParticleEffects;
    public static ForgeConfigSpec.BooleanValue enableSoundEffects;
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;

    // ===== Mastery Scaling Constants =====
    public static ForgeConfigSpec.DoubleValue generalMasteryScaling;
    public static ForgeConfigSpec.DoubleValue specificMasteryScaling;

    // ===== Stack Settings =====
    public static ForgeConfigSpec.IntValue stackDecayTicks;
    public static ForgeConfigSpec.IntValue maxStackCount;

    static {
        BUILDER.comment("====================================")
               .comment("Elemental Reaction System Configuration")
               .comment("====================================");

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

        SPEC = BUILDER.build();
    }
}