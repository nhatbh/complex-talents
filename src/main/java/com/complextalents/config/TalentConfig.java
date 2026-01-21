package com.complextalents.config;

import com.complextalents.elemental.ElementalReaction;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;

public class TalentConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Elemental reaction multipliers
    public static final Map<ElementalReaction, ForgeConfigSpec.DoubleValue> reactionMultipliers = new HashMap<>();

    // Talent settings
    public static ForgeConfigSpec.BooleanValue enableTalents;
    public static ForgeConfigSpec.IntValue maxTalentsPerPlayer;
    public static ForgeConfigSpec.BooleanValue enableElementalTalents;
    public static ForgeConfigSpec.BooleanValue enableElementalSystem;

    // Stack decay settings
    public static ForgeConfigSpec.IntValue stackDecayTicks;
    public static ForgeConfigSpec.IntValue maxStackCount;

    // Reaction damage settings
    public static ForgeConfigSpec.DoubleValue baseReactionDamage;
    public static ForgeConfigSpec.DoubleValue elementalMasteryBonusPerLevel;

    static {
        BUILDER.comment("Elemental Reaction Settings").push("reactions");

        reactionMultipliers.put(ElementalReaction.VAPORIZE,
            BUILDER.comment("Pyro + Hydro reaction multiplier")
                  .defineInRange("vaporizeMultiplier", 1.5, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.MELT,
            BUILDER.comment("Pyro + Cryo reaction multiplier")
                  .defineInRange("meltMultiplier", 2.0, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.FROZEN,
            BUILDER.comment("Cryo + Hydro reaction multiplier")
                  .defineInRange("frozenMultiplier", 1.0, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.OVERLOADED,
            BUILDER.comment("Pyro + Electro reaction multiplier")
                  .defineInRange("overloadedMultiplier", 1.2, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.ELECTRO_CHARGED,
            BUILDER.comment("Hydro + Electro reaction multiplier")
                  .defineInRange("electroChargedMultiplier", 1.0, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.SUPERCONDUCT,
            BUILDER.comment("Cryo + Electro reaction multiplier")
                  .defineInRange("superconductMultiplier", 1.0, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.BURNING,
            BUILDER.comment("Dendro + Pyro reaction multiplier")
                  .defineInRange("burningMultiplier", 1.2, 0.1, 5.0));
        reactionMultipliers.put(ElementalReaction.BLOOM,
            BUILDER.comment("Nature + Aqua reaction multiplier")
                  .defineInRange("bloomMultiplier", 1.0, 0.1, 5.0));

        BUILDER.pop();

        BUILDER.comment("Talent System Settings").push("talents");
        enableTalents = BUILDER.comment("Enable the entire talent system")
                             .define("enableTalents", true);
        maxTalentsPerPlayer = BUILDER.comment("Maximum number of talents a player can have")
                                 .defineInRange("maxTalentsPerPlayer", 20, 1, 100);
        enableElementalTalents = BUILDER.comment("Enable elemental mastery talents (Elemental Mastery, Fire Mastery, etc.)")
                                     .define("enableElementalTalents", true);
        BUILDER.pop();

        BUILDER.comment("Elemental System Settings").push("elemental");
        enableElementalSystem = BUILDER.comment("Enable elemental stack and reaction system")
                                   .define("enableElementalSystem", true);
        stackDecayTicks = BUILDER.comment("How many ticks it takes for an elemental stack to decay naturally (20 ticks = 1 second, default 200 = 10 seconds)")
                              .defineInRange("stackDecayTicks", 200, 20, 3600);
        maxStackCount = BUILDER.comment("Maximum number of stacks of a single element on an entity")
                            .defineInRange("maxStackCount", 2, 1, 10);
        baseReactionDamage = BUILDER.comment("Base damage multiplier for elemental reactions (as percentage of original damage)")
                               .defineInRange("baseReactionDamage", 0.5, 0.0, 2.0);
        elementalMasteryBonusPerLevel = BUILDER.comment("Bonus reaction damage per level of Elemental Mastery talent (as percentage, e.g., 0.1 = 10%)")
                                         .defineInRange("elementalMasteryBonusPerLevel", 0.1, 0.0, 0.5);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void onLoad(final net.minecraftforge.fml.event.config.ModConfigEvent event) {
        // Config values are automatically loaded by Forge
    }

    public static double getReactionMultiplier(ElementalReaction reaction) {
        ForgeConfigSpec.DoubleValue value = reactionMultipliers.get(reaction);
        return value != null ? value.get() : 1.0;
    }
}
