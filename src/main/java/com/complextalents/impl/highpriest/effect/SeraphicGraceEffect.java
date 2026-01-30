package com.complextalents.impl.highpriest.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Seraphic Grace Effect - Cleanup tracker for absorption hearts from High Priest overheal.
 * <p>
 * When a High Priest with max Grace (10 stacks) overheals a target,
 * the excess healing is converted to absorption hearts via the event handler.
 * This effect serves ONLY as a duration tracker - when it expires, all absorption is removed.
 * </p>
 * <p>
 * The amplifier stores how many absorption hearts were granted for display purposes.
 * </p>
 */
public class SeraphicGraceEffect extends MobEffect {

    public SeraphicGraceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFD700);
     }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        entity.setAbsorptionAmount(0);
    }
}
