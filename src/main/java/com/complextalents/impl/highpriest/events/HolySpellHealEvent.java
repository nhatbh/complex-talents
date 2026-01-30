package com.complextalents.impl.highpriest.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an entity is healed by a holy spell from Iron's Spellbooks.
 * <p>
 * This event is specifically for holy spell healing detection and allows
 * origin-specific mechanics to respond to holy healing (e.g., Piety generation).
 * </p>
 *
 * <p>This event is {@linkplain net.minecraftforge.eventbus.api.Event.HasResult has a result}</p>
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>If this event is canceled, the holy heal event will not be processed by origin handlers.</p>
 */
public class HolySpellHealEvent extends Event {

    private final LivingEntity target;
    private final LivingEntity caster;
    private final float healAmount;
    private final Object schoolType; // SchoolType from Iron's Spellbooks (stored as Object for compile-time safety)

    /**
     * Creates a new HolySpellHealEvent.
     *
     * @param target     The entity that was healed
     * @param caster     The entity that cast the holy heal
     * @param healAmount The amount of healing provided
     * @param schoolType The school type of the healing spell (can be null)
     */
    public HolySpellHealEvent(LivingEntity target, LivingEntity caster, float healAmount, Object schoolType) {
        this.target = target;
        this.caster = caster;
        this.healAmount = healAmount;
        this.schoolType = schoolType;
    }

    /**
     * Gets the target entity that was healed.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the caster entity that provided the healing.
     *
     * @return The caster entity
     */
    public LivingEntity getCaster() {
        return caster;
    }

    /**
     * Gets the amount of healing provided.
     *
     * @return The heal amount
     */
    public float getHealAmount() {
        return healAmount;
    }

    /**
     * Gets the school type of the healing spell.
     * <p>
     * This returns an Object to avoid compile-time dependencies on Iron's Spellbooks.
     * If Iron's Spellbooks is loaded, you can cast this to
     * {@code io.redspace.ironsspellbooks.api.spells.SchoolType}.
     * </p>
     *
     * @return The school type object, or null if not available
     */
    public Object getSchoolType() {
        return schoolType;
    }
}
