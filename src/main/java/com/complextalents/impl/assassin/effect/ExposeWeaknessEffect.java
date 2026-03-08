package com.complextalents.impl.assassin.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Expose Weakness Effect - Marks an enemy to receive amplified team damage.
 */
public class ExposeWeaknessEffect extends MobEffect {

    public ExposeWeaknessEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4444); // Red color
    }
}
