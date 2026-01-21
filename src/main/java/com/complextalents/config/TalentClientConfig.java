package com.complextalents.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TalentClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // UI settings
    public static ForgeConfigSpec.BooleanValue showTalentOverlay;
    public static ForgeConfigSpec.BooleanValue showReactionEffects;
    public static ForgeConfigSpec.EnumValue<OverlayPosition> overlayPosition;
    public static ForgeConfigSpec.IntValue overlayScale;
    public static ForgeConfigSpec.BooleanValue showCooldowns;
    public static ForgeConfigSpec.BooleanValue showStacksOnEntities;

    // Key bindings
    public static ForgeConfigSpec.BooleanValue enableKeyBindings;

    static {
        BUILDER.comment("UI Settings").push("ui");
        showTalentOverlay = BUILDER.comment("Show talent HUD overlay")
                                  .define("showTalentOverlay", true);
        showReactionEffects = BUILDER.comment("Show visual effects for elemental reactions")
                                 .define("showReactionEffects", true);
        overlayPosition = BUILDER.comment("Position of talent overlay on screen")
                             .defineEnum("overlayPosition", OverlayPosition.TOP_LEFT);
        overlayScale = BUILDER.comment("Scale of talent overlay (100 = normal size)")
                           .defineInRange("overlayScale", 100, 50, 200);
        showCooldowns = BUILDER.comment("Show cooldown timers on talent icons")
                           .define("showCooldowns", true);
        showStacksOnEntities = BUILDER.comment("Show elemental stack indicators on entities")
                                 .define("showStacksOnEntities", true);
        BUILDER.pop();

        BUILDER.comment("Key Bindings").push("keybindings");
        enableKeyBindings = BUILDER.comment("Enable custom key bindings for activating talents")
                               .define("enableKeyBindings", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public enum OverlayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
