package com.complextalents.elemental.superreaction;

import com.complextalents.elemental.ElementType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * Base interface for all Super-Reactions
 * Each element has its own implementation with unique effects
 */
public interface SuperReaction {

    /**
     * Execute the Super-Reaction
     *
     * @param caster The player triggering the reaction
     * @param target The target entity
     * @param tier The tier of the reaction (based on unique element count)
     * @param elements All unique elements present on the target
     * @param baseDamage The calculated base damage for this reaction
     */
    void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                 Set<ElementType> elements, float baseDamage);

    /**
     * Get the name of this Super-Reaction
     */
    String getName();

    /**
     * Get the description of what this reaction does at a specific tier
     */
    String getDescription(SuperReactionTier tier);
}