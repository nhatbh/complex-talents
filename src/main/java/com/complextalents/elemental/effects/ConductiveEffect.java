package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Conductive effect - marks entity for guaranteed crits (used by Electro-Charged reaction)
 * Next attack against this entity will be a critical hit
 */
public class ConductiveEffect extends MobEffect {
    public ConductiveEffect() {
        super(MobEffectCategory.HARMFUL, 0xAA00FF);  // Purple color for electric
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // The actual crit bonus is applied when the entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
