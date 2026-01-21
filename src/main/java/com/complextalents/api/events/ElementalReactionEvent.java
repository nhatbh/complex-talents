package com.complextalents.api.events;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementType;
import com.complextalents.TalentsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an elemental reaction occurs.
 * This event is cancellable. If cancelled, the reaction's damage and effects will not be applied.
 */
public class ElementalReactionEvent extends Event {
    private final LivingEntity target;
    private final ServerPlayer attacker;
    private final ElementalReaction reaction;
    private float damage;
    private final ElementType element1;
    private final ElementType element2;

    public ElementalReactionEvent(LivingEntity target, ServerPlayer attacker, ElementalReaction reaction, float damage, ElementType element1, ElementType element2) {
        this.target = target;
        this.attacker = attacker;
        this.reaction = reaction;
        this.damage = damage;
        this.element1 = element1;
        this.element2 = element2;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ServerPlayer getAttacker() {
        return attacker;
    }

    public ElementalReaction getReaction() {
        return reaction;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0, damage);
        TalentsMod.LOGGER.debug("Reaction damage modified to {}", damage);
    }

    public ElementType getElement1() {
        return element1;
    }

    public ElementType getElement2() {
        return element2;
    }
}
