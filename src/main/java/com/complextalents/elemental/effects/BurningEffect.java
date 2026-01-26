package com.complextalents.elemental.effects;

import com.complextalents.elemental.ElementFX;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Burning Effect - Damage over Time from Burning Reaction
 * Applies periodic fire damage with visual particle effects.
 *
 * Damage is calculated based on the effect amplifier.
 * Amplifier 0 = 0.5 damage/tick, Amplifier 1 = 1.0 damage/tick, etc.
 */
public class BurningEffect extends MobEffect {

    /**
     * Base damage per tick at amplifier 0.
     * 0.5 damage = 0.25 hearts per tick
     */
    public static final float BASE_DAMAGE_PER_TICK = 0.5f;

    public BurningEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Only apply damage on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Calculate damage: base * (amplifier + 1)
        // Amplifier 0 = 0.5 damage, Amplifier 1 = 1.0 damage, Amplifier 2 = 1.5 damage, etc.
        float damage = BASE_DAMAGE_PER_TICK * (amplifier + 1);

        // Apply fire damage
        entity.hurt(entity.level().damageSources().onFire(), damage);

        // Spawn particle effects for visual feedback
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ElementFX.spawnEmbers(serverLevel, entity.position(), 3);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply damage every second (20 ticks)
        // This makes the damage more readable and less spammy than every tick
        return duration % 20 == 0;
    }
}
