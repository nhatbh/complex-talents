package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementalReaction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Interface for applying effects from elemental reactions.
 * Centralizes effect application logic and makes it testable.
 */
public interface IEffectApplicator {

    /**
     * Applies a potion effect to an entity.
     *
     * @param entity The entity to apply the effect to
     * @param effect The effect to apply
     * @param duration Duration in ticks
     * @param amplifier Effect amplifier (0-based)
     */
    void applyEffect(LivingEntity entity, MobEffect effect, int duration, int amplifier);

    /**
     * Applies a potion effect instance to an entity.
     *
     * @param entity The entity to apply the effect to
     * @param effectInstance The effect instance to apply
     */
    void applyEffect(LivingEntity entity, MobEffectInstance effectInstance);

    /**
     * Removes a potion effect from an entity.
     *
     * @param entity The entity to remove the effect from
     * @param effect The effect to remove
     */
    void removeEffect(LivingEntity entity, MobEffect effect);

    /**
     * Checks if an entity has a specific effect.
     *
     * @param entity The entity to check
     * @param effect The effect to check for
     * @return true if entity has the effect
     */
    boolean hasEffect(LivingEntity entity, MobEffect effect);

    /**
     * Applies knockback to an entity.
     *
     * @param entity The entity to knock back
     * @param strength Knockback strength
     * @param xRatio X component of knockback direction
     * @param zRatio Z component of knockback direction
     */
    void applyKnockback(LivingEntity entity, double strength, double xRatio, double zRatio);

    /**
     * Applies a pull effect to an entity.
     *
     * @param entity The entity to pull
     * @param pullCenter The point to pull towards
     * @param strength Pull strength
     */
    void applyPull(LivingEntity entity, Vec3 pullCenter, double strength);

    /**
     * Sets an entity on fire.
     *
     * @param entity The entity to ignite
     * @param duration Duration in seconds
     */
    void setOnFire(LivingEntity entity, int duration);

    /**
     * Freezes an entity (applies slowness and immobilization).
     *
     * @param entity The entity to freeze
     * @param duration Duration in ticks
     * @param strength Freeze strength (affects slowness level)
     */
    void freeze(LivingEntity entity, int duration, int strength);

    /**
     * Applies damage over time to an entity.
     *
     * @param entity The entity to damage
     * @param source The damage source entity
     * @param reaction The reaction causing the DoT
     * @param totalDamage Total damage to deal
     * @param duration Duration in ticks
     * @param tickRate How often to apply damage (in ticks)
     */
    void applyDamageOverTime(LivingEntity entity, LivingEntity source,
                            ElementalReaction reaction, float totalDamage,
                            int duration, int tickRate);

    /**
     * Applies area of effect damage around a point.
     *
     * @param center The center point
     * @param radius The effect radius
     * @param damage The damage to apply
     * @param source The damage source entity
     * @param excludeTarget Entity to exclude from damage (usually the center entity)
     */
    void applyAreaDamage(Vec3 center, double radius, float damage,
                        LivingEntity source, LivingEntity excludeTarget);

    /**
     * Clears all elemental effects from an entity.
     *
     * @param entity The entity to clear effects from
     */
    void clearElementalEffects(LivingEntity entity);

    /**
     * Applies a custom resistance modifier to an entity.
     *
     * @param entity The entity
     * @param resistanceReduction The resistance reduction amount
     * @param duration Duration in ticks
     */
    void applyResistanceModifier(LivingEntity entity, float resistanceReduction, int duration);
}