package com.complextalents.impl.assassin.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Ambush Effect - Applied after a successful stealth backstab.
 * Handles multiplicative damage scaling via event handlers.
 */
public class AmbushEffect extends MobEffect {

    public AmbushEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x333333); // Dark grey color
    }
}
