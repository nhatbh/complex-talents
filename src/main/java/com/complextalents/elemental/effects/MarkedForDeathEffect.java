package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MarkedForDeathEffect extends MobEffect {

    private static final java.util.UUID DAMAGE_MODIFIER_UUID = java.util.UUID.fromString("e7f4c6b8-3d29-4a1f-8e93-2f5b8c9d1a3e");
    private static final java.util.UUID ARMOR_MODIFIER_UUID = java.util.UUID.fromString("f8c5d7a2-1b34-4e2d-9c87-3a6f5b8e2d1c");

    public MarkedForDeathEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive, damage amplification is handled via attribute modifier
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No periodic ticks needed
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Note: In Minecraft, there's no direct "damage taken" modifier
        // We'll need to handle this through events or damage calculation hooks
        // For now, we'll reduce armor toughness to simulate vulnerability
        var toughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                DAMAGE_MODIFIER_UUID,
                "Marked for death armor reduction",
                -0.5, // Reduce armor toughness by 50%
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            toughnessInstance.addTransientModifier(modifier);
        }

        // Also reduce armor effectiveness
        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Marked for death armor reduction",
                -0.3, // Reduce armor by 30%
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            armorInstance.addTransientModifier(modifier);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        var toughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            toughnessInstance.removeModifier(DAMAGE_MODIFIER_UUID);
        }

        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);
        }
    }
}