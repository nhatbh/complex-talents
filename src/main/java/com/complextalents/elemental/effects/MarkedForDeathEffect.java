package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import com.complextalents.util.UUIDHelper;
import com.complextalents.elemental.strategies.reactions.VoidfireReaction;
import com.complextalents.util.TeamHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Marked for Death Effect - Accumulates damage taken and deals bonus damage on expiration
 * While active, the target takes increased damage (reduced armor).
 * When the effect expires, deals 20% of accumulated damage, scaled by amplifier.
 */
public class MarkedForDeathEffect extends MobEffect {

    private static final UUID DAMAGE_MODIFIER_UUID = UUIDHelper.generateAttributeModifierUUID("elemental_effects", "marked_for_death_toughness");
    private static final UUID ARMOR_MODIFIER_UUID = UUIDHelper.generateAttributeModifierUUID("elemental_effects", "marked_for_death_armor");

    private static final String NBT_ACCUMULATED_DAMAGE = "MarkedForDeathAccumulatedDamage";
    private static final String NBT_HAS_TRIGGERED = "MarkedForDeathTriggered";

    /**
     * Percentage of accumulated damage to deal on expiration (20%)
     */
    private static final float DAMAGE_PERCENTAGE = 0.2f;

    /**
     * Base damage cap at amplifier 0 (in hearts)
     * Amplifier 0: 5 hearts cap
     * Amplifier 1: 10 hearts cap
     * Amplifier 2: 15 hearts cap
     * etc.
     */
    private static final float BASE_DAMAGE_CAP_HEARTS = 5.0f;

    public MarkedForDeathEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive, damage tracking is handled via event
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No periodic ticks needed
    }

    /**
     * Records damage taken by the marked entity.
     * Called from damage event handler.
     *
     * @param entity The marked entity
     * @param damage The damage amount taken
     */
    public static void recordDamage(LivingEntity entity, float damage) {
        CompoundTag data = entity.getPersistentData();
        float accumulated = data.getFloat(NBT_ACCUMULATED_DAMAGE);
        float newAccumulated = accumulated + damage;
        data.putFloat(NBT_ACCUMULATED_DAMAGE, newAccumulated);

        // Log damage accumulation for debugging
        if (!entity.level().isClientSide) {
            TalentsMod.LOGGER.info("Marked for Death: {} accumulated damage: {} (added {})", entity.getName().getString(), String.format("%.2f", newAccumulated), String.format("%.2f", damage));
        }
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Initialize accumulated damage when effect is applied
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_ACCUMULATED_DAMAGE)) {
            data.putFloat(NBT_ACCUMULATED_DAMAGE, 0.0f);
        }
        data.putBoolean(NBT_HAS_TRIGGERED, false);

        // Reduce armor toughness to simulate vulnerability
        var toughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            toughnessInstance.removeModifier(DAMAGE_MODIFIER_UUID);
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
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);
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

        // Remove attribute modifiers
        var toughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            toughnessInstance.removeModifier(DAMAGE_MODIFIER_UUID);
        }

        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);
        }

        // Trigger the death mark damage if effect expired naturally (not removed by other means)
        CompoundTag data = entity.getPersistentData();
        boolean hasTriggered = data.getBoolean(NBT_HAS_TRIGGERED);

        if (!hasTriggered && !entity.level().isClientSide) {
            // Get the attacker username from NBT
            String attackerUsername = VoidfireReaction.getAttackerUsername(entity);
            ServerPlayer attacker = null;

            // Retrieve the attacker from the level if username exists and server is available
            if (attackerUsername != null && entity.getServer() != null) {
                attacker = entity.getServer().getPlayerList().getPlayerByName(attackerUsername);
            }

            // Skip if the entity is an ally of the attacker
            if (attacker != null && TeamHelper.isAlly(attacker, entity)) {
                return;
            }

            // Get accumulated damage
            float accumulatedDamage = data.getFloat(NBT_ACCUMULATED_DAMAGE);

            if (accumulatedDamage > 0) {
                // Calculate final damage: 20% of accumulated damage
                float finalDamage = accumulatedDamage * DAMAGE_PERCENTAGE;

                // Calculate damage cap based on amplifier
                float damageCap = BASE_DAMAGE_CAP_HEARTS * 2.0f * (amplifier + 1); // Convert hearts to half-hearts

                // Cap the damage
                float uncappedDamage = finalDamage;
                finalDamage = Math.min(finalDamage, damageCap);
                boolean wasCapped = uncappedDamage > damageCap;

                // Log expiration damage for debugging
                TalentsMod.LOGGER.info("Marked for Death expired on {}: Accumulated: {}%, Dealt: {}/{} hearts (Amplifier: {}){}",
                    entity.getName().getString(),
                    String.format("%.2f", accumulatedDamage),
                    String.format("%.1f", finalDamage / 2.0f),
                    String.format("%.1f", damageCap / 2.0f),
                    amplifier,
                    wasCapped ? " [CAPPED]" : ""
                );

                // Apply the damage if entity is still alive with player attribution
                if (entity.isAlive()) {
                    DamageSource damageSource;
                    if (attacker != null) {
                        damageSource = entity.level().damageSources().playerAttack(attacker);
                    } else {
                        damageSource = entity.level().damageSources().magic();
                    }
                    entity.hurt(damageSource, finalDamage);
                }

                // Mark as triggered to prevent double-triggering
                data.putBoolean(NBT_HAS_TRIGGERED, true);
            }
        }

        // Clean up NBT data
        data.remove(NBT_ACCUMULATED_DAMAGE);
        data.remove(NBT_HAS_TRIGGERED);
        VoidfireReaction.clearAttackerUsername(entity);
    }

    /**
     * Gets the currently accumulated damage for an entity.
     *
     * @param entity The entity
     * @return The accumulated damage amount
     */
    public static float getAccumulatedDamage(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.getFloat(NBT_ACCUMULATED_DAMAGE);
    }

    /**
     * Calculates the damage that will be dealt when the effect expires.
     *
     * @param entity The entity
     * @param amplifier The effect amplifier
     * @return The damage that will be dealt
     */
    public static float calculateExpirationDamage(LivingEntity entity, int amplifier) {
        float accumulated = getAccumulatedDamage(entity);
        float damage = accumulated * DAMAGE_PERCENTAGE;
        float damageCap = BASE_DAMAGE_CAP_HEARTS * 2.0f * (amplifier + 1);
        return Math.min(damage, damageCap);
    }
}
