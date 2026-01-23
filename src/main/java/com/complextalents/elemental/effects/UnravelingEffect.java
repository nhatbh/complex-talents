package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unraveling effect - marks target for increased damage taken and disables healing
 * Used by Ender Super-Reactions (Tier 3 & 4)
 * - Tier 3: 25% increased damage taken, no healing
 * - Tier 4: 20% increased damage taken, no healing, 1% max HP true damage per hit
 *
 * Damage amplification and true damage are handled in damage event handlers
 * This effect acts as a marker for those systems
 */
public class UnravelingEffect extends MobEffect {
    public static final double TIER_3_DAMAGE_MULTIPLIER = 1.25; // 25% increased damage
    public static final double TIER_4_DAMAGE_MULTIPLIER = 1.20; // 20% increased damage
    public static final double TIER_4_TRUE_DAMAGE_PERCENT = 0.01; // 1% max HP per hit

    public UnravelingEffect() {
        super(MobEffectCategory.HARMFUL, 0x1A0033);  // Dark purple for void corruption
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler and healing event handler
        // Amplifier 0 = Tier 3 (25% damage boost, no healing)
        // Amplifier 1 = Tier 4 (20% damage boost, no healing, 1% true damage)
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
