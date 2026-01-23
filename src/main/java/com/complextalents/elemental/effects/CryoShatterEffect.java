package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cryo-Shatter effect - converts incoming damage to bonus Poise damage (Tier 4 Ice)
 * While active, all damage taken is converted to 125% Poise damage
 * Note: Poise system integration required for full effect
 */
public class CryoShatterEffect extends MobEffect {
    public static final double POISE_DAMAGE_MULTIPLIER = 1.25;

    public CryoShatterEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FFFF);  // Cyan for cryo effect
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // Damage conversion happens when entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
