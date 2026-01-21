package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Brittle effect - marks frozen entities for shatter bonus (used by Frozen reaction)
 * When damaged while frozen, takes bonus damage
 */
public class BrittleEffect extends MobEffect {
    public static final double SHATTER_MULTIPLIER = 1.5;

    public BrittleEffect() {
        super(MobEffectCategory.HARMFUL, 0xCCFFFF);  // Very light blue/white for ice
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // Shatter bonus is applied when the entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
