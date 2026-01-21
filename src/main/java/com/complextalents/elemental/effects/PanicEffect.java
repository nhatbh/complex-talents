package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Panic effect - causes mobs to flee randomly (used by Burning reaction)
 * 30% chance per second to trigger flee behavior
 */
public class PanicEffect extends MobEffect {
    public PanicEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4400);  // Red-orange for burning panic
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob && !entity.level().isClientSide) {
            double panicChance = ElementalReactionConfig.burningPanicChance.get();

            // 30% chance per second = (chance / 20) per tick
            if (entity.level().random.nextDouble() < (panicChance / 20.0)) {
                // Apply velocity in random direction to simulate panic
                double angle = entity.level().random.nextDouble() * Math.PI * 2;
                double speed = 0.3;
                entity.setDeltaMovement(
                    entity.getDeltaMovement().add(
                        Math.cos(angle) * speed,
                        0.1,
                        Math.sin(angle) * speed
                    )
                );
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick every game tick
        return true;
    }
}
