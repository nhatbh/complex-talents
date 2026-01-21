package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fracture effect - variable damage modifier (used by Fracture reaction)
 * 25% chance to ignore damage, 25% chance to amplify damage by 1.25x
 * Effect is checked in damage event handler
 */
public class FractureEffect extends MobEffect {
    public FractureEffect() {
        super(MobEffectCategory.HARMFUL, 0x9966CC);  // Purple-gray for fractured reality
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // The variable damage modifier is applied when the entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
