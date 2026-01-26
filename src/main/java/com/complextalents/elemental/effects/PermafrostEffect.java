package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Permafrost Effect - Roots the target
 * Target cannot move from their current position (rooted).
 * Similar to Freeze but without the shatter mechanic.
 */
public class PermafrostEffect extends MobEffect {

    public PermafrostEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Prevent movement by zeroing out horizontal movement
        if (!entity.level().isClientSide) {
            entity.setDeltaMovement(0, entity.getDeltaMovement().y * 0.1, 0);
            entity.hurtMarked = true;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick to continuously prevent movement
        return true;
    }
}
