package com.complextalents.elemental.events;

import com.complextalents.elemental.ElementType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an elemental stack is removed from an entity.
 * This event is fired on the Forge Event Bus.
 *
 * <p>This event allows other mods or systems to react to stack removal,
 * such as removing effects, triggering callbacks, or tracking removals.</p>
 *
 * <p>Stacks can be removed due to:</p>
 * <ul>
 *   <li>Expiration (time-based decay)</li>
 *   <li>Entity death</li>
 *   <li>Manual removal</li>
 *   <li>Reaction consumption</li>
 * </ul>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>However, canceling this event will not restore the stack as it has already been removed.</p>
 */
public class ElementalStackRemovedEvent extends Event {

    private final LivingEntity target;
    private final ElementType element;
    private final RemovalReason reason;

    /**
     * The reason why the stack was removed.
     */
    public enum RemovalReason {
        /** Stack expired due to time-based decay */
        EXPIRED,
        /** Entity died */
        ENTITY_DEATH,
        /** Stack was consumed by a reaction */
        REACTION_CONSUMED,
        /** Stack was manually removed */
        MANUAL,
        /** Stack was replaced by a new stack of the same element */
        REFRESHED
    }

    /**
     * Creates a new ElementalStackRemovedEvent.
     *
     * @param target The entity that had the stack removed
     * @param element The element type that was removed
     * @param reason The reason for removal
     */
    public ElementalStackRemovedEvent(LivingEntity target, ElementType element, RemovalReason reason) {
        this.target = target;
        this.element = element;
        this.reason = reason;
    }

    /**
     * Gets the target entity that had the stack removed.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the element type that was removed.
     *
     * @return The element type
     */
    public ElementType getElement() {
        return element;
    }

    /**
     * Gets the reason why the stack was removed.
     *
     * @return The removal reason
     */
    public RemovalReason getReason() {
        return reason;
    }
}
