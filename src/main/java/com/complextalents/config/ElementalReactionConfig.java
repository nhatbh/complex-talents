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
    public static ForgeConfigSpec.EnumValue<ParticleQuality> particleQuality;
    public static ForgeConfigSpec.BooleanValue enableSoundEffects;
    public static ForgeConfigSpec.BooleanValue enableDebugLogging;

    public enum ParticleQuality {
        LOW(0.25),
        MEDIUM(0.5),
        HIGH(1.0),
        ULTRA(1.5);

        private final double multiplier;

        ParticleQuality(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public int scale(int baseCount) {
            return (int) Math.max(1, baseCount * multiplier);
        }

        public double scale(double baseValue) {
            return baseValue * multiplier;
        }
    }

    // ===== Mastery Scaling Constants =====
    public static ForgeConfigSpec.DoubleValue generalMasteryScaling;
    public static ForgeConfigSpec.DoubleValue specificMasteryScaling;

    // ===== Stack Settings =====
    public static ForgeConfigSpec.IntValue stackDecayTicks;
    /**
     * @deprecated Stacking has been removed. Only one stack per element is allowed.
     * This config option is kept for backwards compatibility but has no effect.
     */
    @Deprecated
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

        particleQuality = BUILDER
            .comment("Particle quality setting - affects particle count and visual density")
            .comment("LOW: 25% particles (best performance)")
            .comment("MEDIUM: 50% particles (balanced)")
            .comment("HIGH: 100% particles (default, recommended)")
            .comment("ULTRA: 150% particles (maximum visual fidelity)")
            .defineEnum("particleQuality", ParticleQuality.HIGH);

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
            .comment("[DEPRECATED - Stacking removed] Maximum stacks per element on an entity")
            .comment("This option has no effect. Only one stack per element is allowed.")
            .defineInRange("maxStacks", 1, 1, 10);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}