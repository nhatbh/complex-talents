package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Decrepitude effect - attack speed reduction and heal prevention (used by Decrepit Grasp reaction)
 * Reduces attack speed and converts first heal to damage
 */
public class DecrepitudeEffect extends MobEffect {
    // Generate UUID from a consistent seed based on the effect name
    private static final java.util.UUID ATTACK_SPEED_MODIFIER_UUID =
        java.util.UUID.nameUUIDFromBytes("DecrepitudeAttackSpeedReduction".getBytes());

    public DecrepitudeEffect() {
        super(MobEffectCategory.HARMFUL, 0x334455);  // Dark gray-blue for decrepitude
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        // Get current config value and apply attack speed reduction
        double attackSpeedReduction = -ElementalReactionConfig.decrepitGraspAttackSpeedReduction.get();

        var attackSpeedAttribute = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttribute != null) {
            var modifier = new AttributeModifier(
                ATTACK_SPEED_MODIFIER_UUID,
                "Decrepitude attack speed reduction",
                attackSpeedReduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            attackSpeedAttribute.addTransientModifier(modifier);
        }

        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        var attackSpeedAttribute = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttribute != null) {
            attackSpeedAttribute.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
        }
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Attack speed is handled by attribute modifier
        // Heal prevention is handled in damage/heal event handler
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
