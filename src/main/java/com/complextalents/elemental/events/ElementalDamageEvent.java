package com.complextalents.elemental.events;

import com.complextalents.elemental.ElementType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an entity takes elemental damage from any source.
 * This is the entry point event for the elemental reaction system.
 *
 * <p>This event should be fired from all magic damage sources (vanilla and modded)
 * to initiate the elemental reaction chain.</p>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>If this event is canceled, no elemental stacks will be applied.</p>
 */
public class ElementalDamageEvent extends Event {

    private final LivingEntity target;
    private final LivingEntity source;
    private final ElementType element;
    private final float damage;

    /**
     * Creates a new ElementalDamageEvent.
     *
     * @param target The entity that took elemental damage
     * @param source The entity that caused the damage (may be null for environmental sources)
     * @param element The element type of the damage
     * @param damage The amount of elemental damage dealt
     */
    public ElementalDamageEvent(LivingEntity target, LivingEntity source, ElementType element, float damage) {
        this.target = target;
        this.source = source;
        this.element = element;
        this.damage = damage;
    }

    /**
     * Gets the target entity that took elemental damage.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the source entity that caused the damage.
     *
     * @return The source entity, or null if the damage is from an environmental source
     */
    public LivingEntity getSource() {
        return source;
    }

    /**
     * Gets the element type of the damage.
     *
     * @return The element type
     */
    public ElementType getElement() {
        return element;
    }

    /**
     * Gets the amount of elemental damage dealt.
     *
     * @return The damage amount
     */
    public float getDamage() {
        return damage;
    }
}
