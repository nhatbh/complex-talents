package com.complextalents.elemental.effects;

import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Spatial Instability effect - reduces movement speed (used by Rift Pull reaction)
 * Applies a movement speed debuff
 */
public class SpatialInstabilityEffect extends MobEffect {
    // Generate UUID from a consistent seed based on the effect name
    private static final java.util.UUID MOVEMENT_SPEED_MODIFIER_UUID =
        java.util.UUID.nameUUIDFromBytes("SpatialInstabilitySpeedReduction".getBytes());

    public SpatialInstabilityEffect() {
        super(MobEffectCategory.HARMFUL, 0x663399);  // Purple-ish color for ender/rift theme
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        // Get current config value and apply movement speed reduction
        double speedReduction = -ElementalReactionConfig.riftPullSpeedReduction.get();

        var movementSpeedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeedAttribute != null) {
            var modifier = new AttributeModifier(
                MOVEMENT_SPEED_MODIFIER_UUID,
                "Spatial Instability speed reduction",
                speedReduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeedAttribute.addTransientModifier(modifier);
        }

        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier) {
        var movementSpeedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeedAttribute != null) {
            movementSpeedAttribute.removeModifier(MOVEMENT_SPEED_MODIFIER_UUID);
        }
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Movement speed reduction is handled by attribute modifier
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // No periodic ticking needed
        return false;
    }
}
