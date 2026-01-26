package com.complextalents.elemental.events;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.api.ReactionContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when an elemental reaction is triggered.
 * This event is fired on the Forge Event Bus after a reaction is calculated but before it executes.
 *
 * <p>This event allows other mods or systems to:
 * <ul>
 *   <li>Modify reaction damage or effects</li>
 *   <li>Add additional effects when reactions trigger</li>
 *   <li>Prevent specific reactions from executing</li>
 *   <li>Track reaction statistics or achievements</li>
 * </ul></p>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 * <p>If this event is canceled, the reaction will not execute.</p>
 */
public class ElementalReactionTriggeredEvent extends Event {

    private final LivingEntity target;
    private final ServerPlayer attacker;
    private final ElementalReaction reaction;
    private final ElementType triggeringElement;
    private final ElementType existingElement;
    private float damage;
    private final float elementalMastery;
    private final float damageMultiplier;

    /**
     * Creates a new ElementalReactionTriggeredEvent.
     *
     * @param target The target entity of the reaction
     * @param attacker The player who triggered the reaction
     * @param reaction The type of reaction triggered
     * @param triggeringElement The element that triggered the reaction
     * @param existingElement The element already on the target
     * @param damage The calculated reaction damage (modifiable)
     * @param elementalMastery The elemental mastery of the attacker
     * @param damageMultiplier The damage multiplier from talents/skills
     */
    public ElementalReactionTriggeredEvent(LivingEntity target, ServerPlayer attacker,
                                          ElementalReaction reaction, ElementType triggeringElement,
                                          ElementType existingElement, float damage,
                                          float elementalMastery, float damageMultiplier) {
        this.target = target;
        this.attacker = attacker;
        this.reaction = reaction;
        this.triggeringElement = triggeringElement;
        this.existingElement = existingElement;
        this.damage = damage;
        this.elementalMastery = elementalMastery;
        this.damageMultiplier = damageMultiplier;
    }

    /**
     * Gets the target entity of the reaction.
     *
     * @return The target entity
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the player who triggered the reaction.
     *
     * @return The attacking player
     */
    public ServerPlayer getAttacker() {
        return attacker;
    }

    /**
     * Gets the type of reaction triggered.
     *
     * @return The reaction type
     */
    public ElementalReaction getReaction() {
        return reaction;
    }

    /**
     * Gets the element that triggered the reaction.
     *
     * @return The triggering element type
     */
    public ElementType getTriggeringElement() {
        return triggeringElement;
    }

    /**
     * Gets the element that was already on the target.
     *
     * @return The existing element type
     */
    public ElementType getExistingElement() {
        return existingElement;
    }

    /**
     * Gets the calculated reaction damage.
     *
     * @return The reaction damage
     */
    public float getDamage() {
        return damage;
    }

    /**
     * Sets the reaction damage.
     * This allows event handlers to modify the damage before it is applied.
     *
     * @param damage The new damage value (must be non-negative)
     */
    public void setDamage(float damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        this.damage = damage;
    }

    /**
     * Gets the elemental mastery of the attacker.
     *
     * @return The elemental mastery value
     */
    public float getElementalMastery() {
        return elementalMastery;
    }

    /**
     * Gets the damage multiplier from talents/skills.
     *
     * @return The damage multiplier
     */
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Creates a ReactionContext from this event's data.
     * Useful for systems that need full context access.
     *
     * @param level The server level
     * @return A ReactionContext containing this event's data
     */
    public ReactionContext toContext(net.minecraft.server.level.ServerLevel level) {
        return ReactionContext.builder()
            .target(target)
            .attacker(attacker)
            .reaction(reaction)
            .triggeringElement(triggeringElement)
            .existingElement(existingElement)
            .damageMultiplier(damageMultiplier)
            .elementalMastery(elementalMastery)
            .level(level)
            .build();
    }
}
