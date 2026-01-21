package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frostbite effect - reduces target's armor (used by Melt reaction)
 * Applies a percentage-based armor reduction via addAttributeModifiers
 */
public class FrostbiteEffect extends MobEffect {
    // Generate UUID from a consistent seed based on the effect name
    private static final java.util.UUID ARMOR_MODIFIER_UUID =
        java.util.UUID.nameUUIDFromBytes("FrostbiteArmorReduction".getBytes());

    public FrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x88CCFF);  // Light blue color
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        // Get current config value and apply armor reduction
        double armorReduction = -ElementalReactionConfig.meltArmorReduction.get();

        var armorAttribute = entity.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            var modifier = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Frostbite armor reduction",
                armorReduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            armorAttribute.addTransientModifier(modifier);
        }

        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        // Clean up modifier when effect is removed
        var armorAttribute = entity.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            armorAttribute.removeModifier(ARMOR_MODIFIER_UUID);
        }
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // No periodic tick effects
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
