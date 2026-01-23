package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Void Touched effect - marks target with void brand for damage and armor reduction
 * Used by Ender Tier 1 Super-Reaction
 * - 12.5% damage reduction (damage dealt by target)
 * - 12.5% armor reduction (damage taken by target)
 *
 * Damage/armor modifiers are handled in damage event handlers
 * This effect acts as a marker for those systems
 */
public class VoidTouchedEffect extends MobEffect {
    public static final double DAMAGE_REDUCTION = 0.125; // 12.5% reduced damage dealt
    public static final double ARMOR_REDUCTION = 0.125; // 12.5% reduced armor effectiveness

    public VoidTouchedEffect() {
        super(MobEffectCategory.HARMFUL, 0x330066);  // Purple for void brand
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // Brand particles are spawned in ElementalUnleashTalent tick handler
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
