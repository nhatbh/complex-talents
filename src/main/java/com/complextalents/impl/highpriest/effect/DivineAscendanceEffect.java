package com.complextalents.impl.highpriest.effect;

import com.complextalents.TalentsMod;
import com.complextalents.util.UUIDHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Divine Ascendance Effect - AoE raid buff from High Priest ultimate.
 * <p>
 * Grants massive combat stat bonuses to all allies within 50 blocks of the Priest.
 * Buff amount scales with the skill level (stored as amplifier).
 * </p>
 * <p>
 * <strong>Scaling by Level:</strong>
 * <ul>
 *   <li>Level 1: +30% to all stats</li>
 *   <li>Level 2: +45% to all stats</li>
 *   <li>Level 3: +60% to all stats</li>
 *   <li>Level 4: +90% to all stats</li>
 * </ul>
 * </p>
 */
public class DivineAscendanceEffect extends MobEffect {

    // NBT key for storing the caster UUID
    private static final String NBT_CASTER_UUID = "DivineAscendanceCasterUUID";

    // Unique modifier UUIDs for each attribute (prevents conflicts)
    private static final UUID ATTACK_DAMAGE_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_attack_damage");
    private static final UUID ATTACK_SPEED_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_attack_speed");
    private static final UUID MANA_REGEN_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_mana_regen");
    private static final UUID CAST_TIME_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_cast_time");
    private static final UUID COOLDOWN_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_cooldown");
    private static final UUID BULLET_DAMAGE_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest_effects", "divine_ascendance_bullet_damage");

    public DivineAscendanceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold color
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Calculate buff percentage based on skill level (amplifier + 1)
        double buffPercent = getBuffPercent(amplifier);

        // Apply Attack Damage buff
        applyModifier(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID, buffPercent, "Divine Ascendance Damage");

        // Apply Attack Speed buff
        applyModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID, buffPercent, "Divine Ascendance Speed");

        // Apply Iron's Spellbooks attributes if available
        applyIronAttribute(entity, "irons_spellbooks:mana_regen", MANA_REGEN_UUID, buffPercent, "Divine Ascendance Mana Regen");
        applyIronAttribute(entity, "irons_spellbooks:cast_time_reduction", CAST_TIME_UUID, buffPercent, "Divine Ascendance Cast Speed");
        applyIronAttribute(entity, "irons_spellbooks:cooldown_reduction", COOLDOWN_UUID, buffPercent, "Divine Ascendance Cooldown");

        // Apply TAA bullet damage if available
        applyIronAttribute(entity, "taa:bullet_gundamage", BULLET_DAMAGE_UUID, buffPercent, "Divine Ascendance Bullet Damage");
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Remove all modifiers
        removeModifier(entity, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID);
        removeModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID);

        removeIronModifier(entity, "irons_spellbooks:mana_regen", MANA_REGEN_UUID);
        removeIronModifier(entity, "irons_spellbooks:cast_time_reduction", CAST_TIME_UUID);
        removeIronModifier(entity, "irons_spellbooks:cooldown_reduction", COOLDOWN_UUID);
        removeIronModifier(entity, "taa:bullet_gundamage", BULLET_DAMAGE_UUID);

        // Clean up NBT data
        entity.getPersistentData().remove(NBT_CASTER_UUID);
    }

    /**
     * Get the buff percentage as a decimal based on skill level.
     *
     * @param amplifier The effect amplifier (skill level - 1)
     * @return Buff percentage (0.30, 0.45, 0.60, or 0.90)
     */
    public static double getBuffPercent(int amplifier) {
        return switch (amplifier) {
            case 0 -> 0.30;  // Level 1: +30%
            case 1 -> 0.45;  // Level 2: +45%
            case 2 -> 0.60;  // Level 3: +60%
            default -> 0.90; // Level 4+: +90%
        };
    }

    /**
     * Apply an attribute modifier to a vanilla attribute.
     */
    private void applyModifier(LivingEntity entity, Attribute attribute, UUID uuid, double amount, String name) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
            AttributeModifier modifier = new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
            instance.addTransientModifier(modifier);
        }
    }

    /**
     * Apply an attribute modifier to a modded attribute via ResourceLocation.
     */
    private void applyIronAttribute(LivingEntity entity, String attrId, UUID uuid, double amount, String name) {
        ResourceLocation attrLoc = ResourceLocation.fromNamespaceAndPath(
                attrId.split(":")[0],
                attrId.split(":")[1]
        );
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrLoc);
        if (attribute != null) {
            applyModifier(entity, attribute, uuid, amount, name);
        } else {
            TalentsMod.LOGGER.debug("Attribute not found: {} (may be from an unloaded mod)", attrId);
        }
    }

    /**
     * Remove an attribute modifier from a vanilla attribute.
     */
    private void removeModifier(LivingEntity entity, Attribute attribute, UUID uuid) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }

    /**
     * Remove an attribute modifier from a modded attribute via ResourceLocation.
     */
    private void removeIronModifier(LivingEntity entity, String attrId, UUID uuid) {
        ResourceLocation attrLoc = ResourceLocation.fromNamespaceAndPath(
                attrId.split(":")[0],
                attrId.split(":")[1]
        );
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrLoc);
        if (attribute != null) {
            removeModifier(entity, attribute, uuid);
        }
    }

    // ========== Static Data Access Methods ==========

    /**
     * Store the caster UUID on the target entity.
     */
    public static void setCasterUUID(LivingEntity target, java.util.UUID casterUUID) {
        target.getPersistentData().putUUID(NBT_CASTER_UUID, casterUUID);
    }

    /**
     * Get the stored caster UUID from the target.
     */
    public static java.util.UUID getCasterUUID(LivingEntity target) {
        if (target.getPersistentData().hasUUID(NBT_CASTER_UUID)) {
            return target.getPersistentData().getUUID(NBT_CASTER_UUID);
        }
        return null;
    }
}
