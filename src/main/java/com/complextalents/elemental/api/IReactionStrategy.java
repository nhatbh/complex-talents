package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementalReaction;

/**
 * Strategy interface for elemental reactions.
 * Each reaction type should have its own implementation of this interface.
 * This allows for easy extensibility and reduces coupling in the system.
 */
public interface IReactionStrategy {

    /**
     * Executes the reaction effect on the target.
     * This method should handle all aspects of the reaction including:
     * - Damage application
     * - Effect application
     * - Visual effects
     * - Special mechanics
     *
     * @param context The reaction context containing all necessary data
     */
    void execute(ReactionContext context);

    /**
     * Calculates the damage that this reaction will deal.
     * This is separated from execution to allow for damage preview
     * and modification by other systems.
     *
     * @param context The reaction context
     * @return The calculated damage amount
     */
    float calculateDamage(ReactionContext context);

    /**
     * Checks if this reaction can trigger in the given context.
     * This allows for conditional reactions based on game state,
     * entity conditions, or other factors.
     *
     * @param context The reaction context
     * @return true if the reaction can trigger, false otherwise
     */
    boolean canTrigger(ReactionContext context);

    /**
     * Gets the reaction type this strategy handles.
     *
     * @return The ElementalReaction enum value
     */
    ElementalReaction getReactionType();

    /**
     * Gets the priority of this reaction for ordering purposes.
     * Higher priority reactions execute first when multiple reactions
     * could trigger simultaneously.
     *
     * @return Priority value (higher = earlier execution)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Checks if this reaction consumes the elemental stacks.
     * Some reactions might preserve stacks for chain reactions.
     *
     * @return true if stacks should be consumed, false otherwise
     */
    default boolean consumesStacks() {
        return true;
    }
}