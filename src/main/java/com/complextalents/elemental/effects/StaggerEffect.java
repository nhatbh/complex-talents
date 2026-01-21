package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stagger effect - interrupts actions (used by Overloaded reaction)
 * Prevents the entity from performing actions briefly
 */
public class StaggerEffect extends MobEffect {
    public StaggerEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFAA00);  // Orange color
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Interrupt current action by clearing AI goals temporarily
        // This is a simplified implementation - full implementation would need custom AI handling
        if (!entity.level().isClientSide) {
            // Cancel any current item use
            if (entity.isUsingItem()) {
                entity.stopUsingItem();
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick every game tick while active
        return true;
    }
}
