package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Scorched Earth effect - Tier 3 Fire Super-Reaction zone DOT
 * Creates a ground zone that deals DOT over 3.75 seconds (75 ticks)
 * Zone is created by the super-reaction, this effect marks entities inside
 */
public class ScorchedEarthEffect extends MobEffect {
    public static final float BASE_MULTIPLIER = 0.3f;
    public static final int DURATION_TICKS = 75; // 3.75 seconds

    public ScorchedEarthEffect() {
        super(MobEffectCategory.HARMFUL, 0xCC3300);  // Dark red for scorched ground
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // DOT damage is handled by DamageOverTimeManager
        // Zone management is handled by FireSuperReaction
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // DOT ticks are managed by DamageOverTimeManager
        return false;
    }
}
