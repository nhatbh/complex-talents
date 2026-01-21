package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vulnerable effect - amplifies damage taken (used by Hyperbloom reaction)
 * Increases incoming damage by configured percentage
 */
public class VulnerableEffect extends MobEffect {
    public VulnerableEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FF88);  // Green-cyan color
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is checked in damage event handler
        // Damage amplification is applied when the entity takes damage
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }

    /**
     * Gets the damage amplification multiplier for this effect
     */
    public static double getDamageAmplifier() {
        return 1.0 + ElementalReactionConfig.hyperbloomVulnerableAmplification.get();
    }
}
