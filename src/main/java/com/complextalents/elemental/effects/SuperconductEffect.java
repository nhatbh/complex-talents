package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Superconduct Effect - Armor corrosion
 * Reduces target's armor by 50% using attribute modifiers.
 */
public class SuperconductEffect extends MobEffect {

    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("a8b3c4d5-6e7f-4a8b-9c0d-1e2f3a4b5c6d");

    public SuperconductEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive, armor reduction is handled via attribute modifier
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No periodic ticks needed
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Apply -50% armor modifier
        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            // Remove existing modifier if present
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);

            AttributeModifier armorShred = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Superconduct armor reduction",
                -0.5, // -50% armor
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            armorInstance.addTransientModifier(armorShred);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);
        }
    }
}
