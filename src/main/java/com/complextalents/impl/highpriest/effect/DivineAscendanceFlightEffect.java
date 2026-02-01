package com.complextalents.impl.highpriest.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Divine Ascendance Flight Effect.
 * <p>
 * Marker effect applied only to the Priest (caster) when Divine Ascendance is active.
 * The flight limiter in DivineAscendanceFlightHandler checks for this effect to enable
 * and limit flight for the caster.
 * </p>
 */
public class DivineAscendanceFlightEffect extends MobEffect {

    public DivineAscendanceFlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold color
    }
}
