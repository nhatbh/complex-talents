package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Withering effect - damage reduction and life siphon (used by Withering Seed reaction)
 * Reduces outgoing damage and causes attackers to siphon life
 * Effect is checked in damage event handler
 */
public class WitheringEffect extends MobEffect {
    public WitheringEffect() {
        super(MobEffectCategory.HARMFUL, 0x556622);  // Sickly green-brown
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Damage reduction is handled in damage event handler
        // Life siphon is handled when this entity deals damage to others
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }

    /**
     * Gets the damage reduction multiplier
     */
    public static double getDamageReduction() {
        if (ElementalReactionConfig.witheringSeedDamageReduction == null) {
            return 0.15; // Default fallback
        }
        return ElementalReactionConfig.witheringSeedDamageReduction.get();
    }

    /**
     * Gets the life steal percentage
     */
    public static double getLifeSteal() {
        if (ElementalReactionConfig.witheringSeedLifeSteal == null) {
            return 0.5; // Default fallback
        }
        return ElementalReactionConfig.witheringSeedLifeSteal.get();
    }
}
