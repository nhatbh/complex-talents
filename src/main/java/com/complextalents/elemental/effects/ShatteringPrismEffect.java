package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shattering Prism effect - marks target for explosive shatter on next hit (Tier 2 Ice)
 * When damaged, triggers 150% AoE damage in 4-block radius
 * Effect is consumed when triggered
 */
public class ShatteringPrismEffect extends MobEffect {
    public static final double SHATTER_DAMAGE_MULTIPLIER = 1.5;
    public static final float SHATTER_RADIUS = 4.0f;

    public ShatteringPrismEffect() {
        super(MobEffectCategory.HARMFUL, 0x99CCFF);  // Light blue for crystal effect
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // Shatter explosion triggers when entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
