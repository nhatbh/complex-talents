package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Interface for calculating damage from elemental reactions.
 * Separates damage calculation logic from reaction execution.
 */
public interface IDamageCalculator {

    /**
     * Calculates the base damage for a reaction.
     *
     * @param reaction The reaction type
     * @param element The triggering element
     * @param triggeringDamage The damage of the triggering spell
     * @param attacker The player causing the reaction
     * @return The calculated base damage
     */
    float calculateBaseDamage(ElementalReaction reaction, ElementType element,
                             float triggeringDamage, ServerPlayer attacker);

    /**
     * Calculates damage with full context.
     *
     * @param context The reaction context
     * @return The calculated damage
     */
    float calculateDamage(ReactionContext context);

    /**
     * Gets the mastery multiplier for a player and element.
     *
     * @param player The player
     * @param element The element type
     * @return The mastery multiplier
     */
    float getMasteryMultiplier(ServerPlayer player, ElementType element);

    /**
     * Gets the general elemental mastery value for a player.
     *
     * @param player The player
     * @return The general mastery value
     */
    double getGeneralMastery(ServerPlayer player);

    /**
     * Gets the specific mastery value for an element.
     *
     * @param player The player
     * @param element The element type
     * @return The specific mastery value
     */
    double getSpecificMastery(ServerPlayer player, ElementType element);

    /**
     * Applies damage modifiers based on target conditions.
     *
     * @param baseDamage The base damage
     * @param target The target entity
     * @param reaction The reaction type
     * @return The modified damage
     */
    float applyTargetModifiers(float baseDamage, LivingEntity target, ElementalReaction reaction);

    /**
     * Checks if damage should be amplified (crit, special conditions, etc).
     *
     * @param context The reaction context
     * @return The amplification multiplier (1.0 for no amplification)
     */
    float getAmplificationMultiplier(ReactionContext context);

    /**
     * Gets the configuration multiplier for a reaction type.
     *
     * @param reaction The reaction type
     * @return The configured multiplier
     */
    double getConfiguredMultiplier(ElementalReaction reaction);
}