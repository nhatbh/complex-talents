package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Conflagration Burn effect - Tier 1 Fire Super-Reaction DOT
 * Applies burning damage over 2.5 seconds (50 ticks)
 * Damage is applied periodically via DamageOverTimeManager
 */
public class ConflagrationBurnEffect extends MobEffect {
    public static final float BASE_MULTIPLIER = 0.4f;
    public static final int DURATION_TICKS = 50; // 2.5 seconds

    public ConflagrationBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600);  // Orange-red for burning
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // DOT damage is handled by DamageOverTimeManager
        // This effect acts as a marker for the DOT system
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // DOT ticks are managed by DamageOverTimeManager
        return false;
    }
}
