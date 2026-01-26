package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * Main service interface for the elemental reaction system.
 * Provides a high-level API for triggering and managing reactions.
 */
public interface IReactionService {

    /**
     * Triggers an elemental reaction on a target.
     *
     * @param target The target entity
     * @param reaction The reaction type
     * @param triggeringElement The element that triggered the reaction
     * @param existingElement The element already on the target
     * @param attacker The player causing the reaction
     * @param triggeringSpellDamage The damage of the triggering spell
     */
    void triggerReaction(LivingEntity target, ElementalReaction reaction,
                        ElementType triggeringElement, ElementType existingElement,
                        ServerPlayer attacker, float triggeringSpellDamage);

    /**
     * Triggers a reaction using a context object.
     *
     * @param context The reaction context
     */
    void triggerReaction(ReactionContext context);

    /**
     * Checks if a reaction can occur between two elements.
     *
     * @param element1 The first element
     * @param element2 The second element
     * @return The reaction that would occur, or empty if no reaction
     */
    Optional<ElementalReaction> getReaction(ElementType element1, ElementType element2);

    /**
     * Applies an element to a target and checks for reactions.
     *
     * @param target The target entity
     * @param element The element to apply
     * @param source The source of the element
     * @param damage The spell damage
     * @return true if a reaction was triggered
     */
    boolean applyElementAndCheckReaction(LivingEntity target, ElementType element,
                                        LivingEntity source, float damage);

    /**
     * Registers a custom reaction strategy.
     *
     * @param reaction The reaction type
     * @param strategy The strategy implementation
     */
    void registerReactionStrategy(ElementalReaction reaction, IReactionStrategy strategy);

    /**
     * Gets all registered reaction types.
     *
     * @return Collection of registered reactions
     */
    Collection<ElementalReaction> getRegisteredReactions();

    /**
     * Gets the strategy for a specific reaction.
     *
     * @param reaction The reaction type
     * @return The strategy, or null if not registered
     */
    @Nullable
    IReactionStrategy getStrategy(ElementalReaction reaction);

    /**
     * Checks if the elemental system is enabled.
     *
     * @return true if the system is enabled
     */
    boolean isSystemEnabled();

    /**
     * Checks if friendly fire protection is enabled.
     *
     * @return true if friendly fire protection is enabled
     */
    boolean isFriendlyFireProtectionEnabled();

    /**
     * Checks if two entities are teammates.
     *
     * @param entity1 The first entity
     * @param entity2 The second entity
     * @return true if they are teammates
     */
    boolean areTeammates(LivingEntity entity1, LivingEntity entity2);

    /**
     * Gets the stack manager.
     *
     * @return The elemental stack manager
     */
    IElementalStackManager getStackManager();

    /**
     * Gets the damage calculator.
     *
     * @return The damage calculator
     */
    IDamageCalculator getDamageCalculator();

    /**
     * Gets the effect applicator.
     *
     * @return The effect applicator
     */
    IEffectApplicator getEffectApplicator();

    /**
     * Gets the particle manager.
     *
     * @return The particle manager
     */
    IParticleManager getParticleManager();

    /**
     * Reloads the reaction registry and configurations.
     * Useful for runtime configuration changes.
     */
    void reload();

    /**
     * Shuts down the service and cleans up resources.
     */
    void shutdown();
}