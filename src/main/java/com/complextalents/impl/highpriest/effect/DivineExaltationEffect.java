package com.complextalents.impl.highpriest.effect;

import com.complextalents.util.UUIDHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.UUID;

/**
 * Divine Exaltation Effect - Applied to allies by Tier 2 Divine Grace.
 * <p>
 * Grants increased damage and generates piety for the caster when the affected entity deals damage.
 * Stores caster UUID, skill level, and piety per hit amount in entity NBT.
 * </p>
 */
public class DivineExaltationEffect extends MobEffect {

    // NBT keys for storing effect data on the entity
    private static final String NBT_CASTER_UUID = "DivineExaltationCasterUUID";
    private static final String NBT_SKILL_LEVEL = "DivineExaltationSkillLevel";
    private static final String NBT_PIETY_PER_HIT = "DivineExaltationPietyPerHit";

    // Damage boost attribute modifier UUID (unique per entity)
    private static final UUID DAMAGE_MODIFIER_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_exaltation_damage");

    public DivineExaltationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold color
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Calculate damage boost based on skill level (amplifier + 1 = skill level)
        // Stored separately in NBT since amplifier doesn't directly give us the percent
        CompoundTag data = entity.getPersistentData();
        double damageBoostPercent = data.getDouble("DivineExaltationDamageBoostPercent");

        if (damageBoostPercent > 0) {
            AttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamageAttr != null) {
                // Remove old modifier if exists
                attackDamageAttr.removeModifier(DAMAGE_MODIFIER_UUID);

                // Add new damage boost modifier (operation 1 = add % of base)
                AttributeModifier modifier = new AttributeModifier(
                        DAMAGE_MODIFIER_UUID,
                        "Divine Exaltation Damage Boost",
                        damageBoostPercent,
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                attackDamageAttr.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Remove damage boost modifier
        AttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            attackDamageAttr.removeModifier(DAMAGE_MODIFIER_UUID);
        }

        // Clean up NBT data
        cleanupEffectData(entity);
    }

    // ========== Static Data Access Methods ==========

    /**
     * Initialize effect data on the target ally.
     *
     * @param target     The ally receiving the effect
     * @param casterUUID The High Priest who cast the effect
     * @param skillLevel The skill level (1-4) for piety calculation
     * @param pietyPerHit Amount of piety to generate per hit
     */
    public static void initializeEffectData(LivingEntity target, UUID casterUUID,
                                            int skillLevel, double pietyPerHit) {
        CompoundTag data = target.getPersistentData();
        data.putUUID(NBT_CASTER_UUID, casterUUID);
        data.putInt(NBT_SKILL_LEVEL, skillLevel);
        data.putDouble(NBT_PIETY_PER_HIT, pietyPerHit);

        // Store damage boost percent based on skill level
        // 10/15/20/25% by level
        double damageBoost = switch (skillLevel) {
            case 1 -> 0.10;
            case 2 -> 0.15;
            case 3 -> 0.20;
            default -> 0.25; // level 4+
        };
        data.putDouble("DivineExaltationDamageBoostPercent", damageBoost);
    }

    /**
     * Get the stored caster UUID from the target's NBT.
     */
    public static UUID getCasterUUID(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (data.hasUUID(NBT_CASTER_UUID)) {
            return data.getUUID(NBT_CASTER_UUID);
        }
        return null;
    }

    /**
     * Get the skill level for this effect.
     */
    public static int getSkillLevel(LivingEntity target) {
        return target.getPersistentData().getInt(NBT_SKILL_LEVEL);
    }

    /**
     * Get the piety per hit amount.
     */
    public static double getPietyPerHit(LivingEntity target) {
        return target.getPersistentData().getDouble(NBT_PIETY_PER_HIT);
    }

    /**
     * Check if the target has an active Divine Exaltation effect.
     */
    public static boolean hasActiveEffect(LivingEntity target) {
        return target.hasEffect(HighPriestEffects.DIVINE_EXALTATION.get());
    }

    /**
     * Get the damage boost percentage for the target.
     */
    public static double getDamageBoost(LivingEntity target) {
        return target.getPersistentData().getDouble("DivineExaltationDamageBoostPercent");
    }

    /**
     * Clean up effect data (called when effect expires or is removed).
     */
    public static void cleanupEffectData(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(NBT_CASTER_UUID);
        data.remove(NBT_SKILL_LEVEL);
        data.remove(NBT_PIETY_PER_HIT);
        data.remove("DivineExaltationDamageBoostPercent");

        // Remove damage boost modifier if effect was removed manually
        AttributeInstance attackDamageAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            attackDamageAttr.removeModifier(DAMAGE_MODIFIER_UUID);
        }
    }
}
