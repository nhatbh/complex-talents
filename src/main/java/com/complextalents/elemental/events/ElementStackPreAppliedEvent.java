package com.complextalents.elemental.events;

import com.complextalents.elemental.ElementType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired BEFORE a new element stack is applied to an entity.
 * This event is fired on the Forge Event Bus.
 *
 * <p>This event allows systems to cancel element stack applications before they occur,
 * such as preventing Nature Cores from being targets for elemental reactions.</p>
 *
 * <p>This event differs from {@link ElementStackAppliedEvent} in timing:</p>
 * <ul>
 *   <li>ElementStackPreAppliedEvent: Fires BEFORE application, can cancel</li>
 *   <li>ElementStackAppliedEvent: Fires AFTER application, for reactions/effects</li>
 * </ul>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>If this event is canceled, the element stack will not be applied and no reactions will trigger.</p>
 */
@Cancelable
public class ElementStackPreAppliedEvent extends Event {

    private final LivingEntity target;
    private final LivingEntity source;
    private final ElementType element;
    private int stackCount;

    /**
     * Creates a new ElementStackPreAppliedEvent.
     *
     * @param target The entity receiving the element stack
     * @param source The entity causing the element application (may be null for environmental sources)
     * @param element The element type being applied
     * @param stackCount The number of stacks being applied
     */
    public ElementStackPreAppliedEvent(LivingEntity target, LivingEntity source, ElementType element, int stackCount) {
        super();
        this.target = target;
        this.source = source;
        this.element = element;
        this.stackCount = stackCount;
    }

    /**
     * Gets the target entity receiving the element stack.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the source entity causing the element application.
     *
     * @return The source entity, or null if the application is from an environmental source
     */
    public LivingEntity getSource() {
        return source;
    }

    /**
     * Gets the element type being applied.
     *
     * @return The element type
     */
    public ElementType getElement() {
        return element;
    }

    /**
     * Gets the number of stacks being applied.
     *
     * @return The stack count
     */
    public int getStackCount() {
        return stackCount;
    }

    /**
     * Sets the number of stacks to apply.
     * This allows event handlers to modify the stack count before application.
     *
     * @param stackCount The new stack count (must be positive)
     */
    public void setStackCount(int stackCount) {
        if (stackCount < 0) {
            throw new IllegalArgumentException("Stack count cannot be negative");
        }
        this.stackCount = stackCount;
    }
}
