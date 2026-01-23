package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ignition Fuse effect - Tier 4 Fire Super-Reaction countdown debuff
 * 8-second (160 tick) countdown that detonates for massive damage
 * Detonation is triggered when effect expires or entity dies
 */
public class IgnitionFuseEffect extends MobEffect {
    public static final int COUNTDOWN_TICKS = 160; // 8 seconds
    public static final float DETONATION_MULTIPLIER = 3.0f; // 300% damage on detonation
    public static final float DETONATION_RADIUS = 5.0f; // AoE radius in blocks

    public IgnitionFuseEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000);  // Bright red for imminent explosion
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Visual/audio countdown effects could be added here
        // Detonation is handled in FireSuperReaction when effect expires
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick every second for countdown particles/sounds
        return duration % 20 == 0;
    }
}
