package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for managing elemental stacks on entities.
 * This abstraction allows for different implementations and easier testing.
 */
public interface IElementalStackManager {

    /**
     * Applies an elemental stack to a target entity.
     *
     * @param target The entity to apply the element to
     * @param element The element type to apply
     * @param source The entity causing the elemental application
     */
    void applyElementStack(LivingEntity target, ElementType element, LivingEntity source);

    /**
     * Gets all elemental stacks on an entity.
     *
     * @param entityId The entity's UUID
     * @return Map of element types to stack counts
     */
    Map<ElementType, Integer> getEntityStacks(UUID entityId);

    /**
     * Gets all elemental stacks on an entity.
     *
     * @param entity The entity
     * @return Map of element types to stack counts
     */
    Map<ElementType, Integer> getEntityStacks(LivingEntity entity);

    /**
     * Checks if an entity has any elemental stacks.
     *
     * @param entityId The entity's UUID
     * @return true if entity has stacks
     */
    boolean hasAnyStack(UUID entityId);

    /**
     * Checks if an entity has any elemental stacks.
     *
     * @param entity The entity
     * @return true if entity has stacks
     */
    boolean hasAnyStack(LivingEntity entity);

    /**
     * Gets the element type with the highest stack count on an entity.
     *
     * @param entityId The entity's UUID
     * @return The element with highest stacks, or null if no stacks
     */
    ElementType getHighestStack(UUID entityId);

    /**
     * Sets the stack count for a specific element on an entity.
     *
     * @param target The target entity
     * @param element The element type
     * @param count The stack count (0 or less removes the stack)
     */
    void setStacks(LivingEntity target, ElementType element, int count);

    /**
     * Clears all elemental stacks from an entity.
     *
     * @param entityId The entity's UUID
     */
    void clearEntityStacks(UUID entityId);

    /**
     * Gets all unique elements currently on an entity.
     *
     * @param entity The entity
     * @return Set of unique element types
     */
    Set<ElementType> getUniqueElements(LivingEntity entity);

    /**
     * Checks if an entity has a specific element.
     *
     * @param entity The entity
     * @param element The element to check for
     * @return true if entity has the element
     */
    boolean hasElement(LivingEntity entity, ElementType element);

    /**
     * Gets the stack count for a specific element on an entity.
     *
     * @param entity The entity
     * @param element The element type
     * @return The stack count, or 0 if not present
     */
    int getStackCount(LivingEntity entity, ElementType element);
}