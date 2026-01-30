package com.complextalents.origin.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an entity takes holy spell damage from Iron's Spellbooks.
 * <p>
 * This event is specifically for holy spell detection and is separate from the
 * elemental damage system, allowing origin-specific mechanics to respond to holy damage.
 * </p>
 *
 * <p>This event is {@linkplain net.minecraftforge.eventbus.api.Event.HasResult has a result}</p>
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>If this event is canceled, the holy damage event will not be processed by origin handlers.</p>
 */
public class HolySpellDamageEvent extends Event {

    private final LivingEntity target;
    private final LivingEntity caster;
    private final float damage;
    private final Object spell; // AbstractSpell from Iron's Spellbooks (stored as Object for compile-time safety)

    /**
     * Creates a new HolySpellDamageEvent.
     *
     * @param target The entity that took holy damage
     * @param caster The entity that cast the holy spell
     * @param damage The amount of holy damage dealt
     * @param spell  The spell that caused the damage (can be null)
     */
    public HolySpellDamageEvent(LivingEntity target, LivingEntity caster, float damage, Object spell) {
        this.target = target;
        this.caster = caster;
        this.damage = damage;
        this.spell = spell;
    }

    /**
     * Gets the target entity that took holy damage.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the caster entity that caused the holy damage.
     *
     * @return The caster entity
     */
    public LivingEntity getCaster() {
        return caster;
    }

    /**
     * Gets the amount of holy damage dealt.
     *
     * @return The damage amount
     */
    public float getDamage() {
        return damage;
    }

    /**
     * Gets the spell that caused the holy damage.
     * <p>
     * This returns an Object to avoid compile-time dependencies on Iron's Spellbooks.
     * If Iron's Spellbooks is loaded, you can cast this to
     * {@code io.redspace.ironsspellbooks.api.spells.AbstractSpell}.
     * </p>
     *
     * @return The spell object, or null if not available
     */
    public Object getSpell() {
        return spell;
    }
}
