package com.complextalents.impl.highpriest.effect;

import com.complextalents.impl.highpriest.events.CovenantDamageMitigatedEvent;
import com.complextalents.impl.highpriest.events.CovenantProtectionEffectRemovedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * Covenant of Protection Effect - Placed on the protected ally.
 * Stores caster UUID, range, damage reduction rate, and piety drain rate.
 * Uses entity NBT to persist effect data.
 */
public class CovenantProtectionEffect extends MobEffect {

    // NBT keys for storing effect data on the entity
    private static final String NBT_CASTER_UUID = "CovenantCasterUUID";
    private static final String NBT_RANGE = "CovenantRange";
    private static final String NBT_DAMAGE_REDUCTION = "CovenantDamageReduction";
    private static final String NBT_PIETY_DRAIN_RATE = "CovenantPietyDrainRate";
    private static final String NBT_SKILL_LEVEL = "CovenantSkillLevel";

    public CovenantProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold color
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up NBT data and fire removal event
        CompoundTag data = entity.getPersistentData();

        // Get data before cleanup for the event
        UUID casterId = getStoredCasterUUID(data);
        int skillLevel = data.getInt(NBT_SKILL_LEVEL);

        // Determine removal reason - default to expired (duration ran out)
        // Other reasons are determined by the code that removes the effect
        CovenantProtectionEffectRemovedEvent.RemovalReason reason = CovenantProtectionEffectRemovedEvent.RemovalReason.EXPIRED;

        // Fire removal event
        MinecraftForge.EVENT_BUS.post(new CovenantProtectionEffectRemovedEvent(
                entity, casterId, skillLevel, reason
        ));

        // Clean up all NBT data
        data.remove(NBT_CASTER_UUID);
        data.remove(NBT_RANGE);
        data.remove(NBT_DAMAGE_REDUCTION);
        data.remove(NBT_PIETY_DRAIN_RATE);
        data.remove(NBT_SKILL_LEVEL);
    }

    // ========== Static Data Access Methods ==========

    /**
     * Initialize effect data on the protected target.
     */
    public static void initializeEffectData(LivingEntity target, UUID casterUUID,
                                            double range, double damageReduction,
                                            double pietyDrainRate, int skillLevel) {
        CompoundTag data = target.getPersistentData();
        data.putUUID(NBT_CASTER_UUID, casterUUID);
        data.putDouble(NBT_RANGE, range);
        data.putDouble(NBT_DAMAGE_REDUCTION, damageReduction);
        data.putDouble(NBT_PIETY_DRAIN_RATE, pietyDrainRate);
        data.putInt(NBT_SKILL_LEVEL, skillLevel);
    }

    /**
     * Get the stored caster UUID from the target's NBT.
     */
    public static UUID getStoredCasterUUID(LivingEntity target) {
        return getStoredCasterUUID(target.getPersistentData());
    }

    private static UUID getStoredCasterUUID(CompoundTag data) {
        if (data.hasUUID(NBT_CASTER_UUID)) {
            return data.getUUID(NBT_CASTER_UUID);
        }
        return null;
    }

    /**
     * Get the damage reduction rate (0.0 - 1.0).
     */
    public static double getDamageReduction(LivingEntity target) {
        return target.getPersistentData().getDouble(NBT_DAMAGE_REDUCTION);
    }

    /**
     * Get the piety drain rate (piety divisor per damage).
     */
    public static double getPietyDrainRate(LivingEntity target) {
        return target.getPersistentData().getDouble(NBT_PIETY_DRAIN_RATE);
    }

    /**
     * Get the range for this covenant.
     */
    public static double getRange(LivingEntity target) {
        return target.getPersistentData().getDouble(NBT_RANGE);
    }

    /**
     * Get the skill level for this covenant.
     */
    public static int getSkillLevel(LivingEntity target) {
        return target.getPersistentData().getInt(NBT_SKILL_LEVEL);
    }

    /**
     * Check if the target has an active covenant effect.
     */
    public static boolean hasActiveCovenant(LivingEntity target) {
        return target.hasEffect(HighPriestEffects.COVENANT_PROTECTION.get());
    }

    /**
     * Apply damage mitigation.
     * Returns the mitigated damage amount.
     */
    public static float applyMitigation(LivingEntity target, float originalDamage) {
        double reductionRate = getDamageReduction(target);
        float mitigatedDamage = originalDamage * (float) reductionRate;

        // Fire the mitigation event so piety can be deducted
        UUID casterId = getStoredCasterUUID(target);
        int skillLevel = getSkillLevel(target);

        MinecraftForge.EVENT_BUS.post(new CovenantDamageMitigatedEvent(
                target, casterId, mitigatedDamage, skillLevel
        ));

        return mitigatedDamage;
    }

    /**
     * Clean up covenant data (called when manually removing effect).
     */
    public static void cleanupCovenantData(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(NBT_CASTER_UUID);
        data.remove(NBT_RANGE);
        data.remove(NBT_DAMAGE_REDUCTION);
        data.remove(NBT_PIETY_DRAIN_RATE);
        data.remove(NBT_SKILL_LEVEL);
    }
}
