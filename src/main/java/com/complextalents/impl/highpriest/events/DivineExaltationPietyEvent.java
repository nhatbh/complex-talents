package com.complextalents.impl.highpriest.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event fired when piety is generated through Divine Exaltation effect.
 * <p>
 * This event allows other systems to modify or cancel piety generation.
 * It is fired when an ally with Divine Exaltation deals damage.
 * </p>
 */
@Cancelable
public class DivineExaltationPietyEvent extends Event {

    private final ServerPlayer caster;
    private final LivingEntity dealer;
    private final LivingEntity target;
    private final double basePiety;
    private double finalPiety;

    /**
     * Create a new Divine Exaltation piety event.
     *
     * @param caster     The High Priest who receives the piety
     * @param dealer     The entity with Divine Exaltation who dealt damage
     * @param target     The entity that was damaged
     * @param basePiety  The base amount of piety to generate
     * @param finalPiety The actual amount of piety generated (after cap)
     */
    public DivineExaltationPietyEvent(ServerPlayer caster, LivingEntity dealer,
                                     LivingEntity target, double basePiety, double finalPiety) {
        this.caster = caster;
        this.dealer = dealer;
        this.target = target;
        this.basePiety = basePiety;
        this.finalPiety = finalPiety;
    }

    /**
     * Get the caster who receives the piety.
     */
    public ServerPlayer getCaster() {
        return caster;
    }

    /**
     * Get the entity with Divine Exaltation who dealt damage.
     */
    public LivingEntity getDealer() {
        return dealer;
    }

    /**
     * Get the entity that was damaged.
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Get the base amount of piety before caps/modifications.
     */
    public double getBasePiety() {
        return basePiety;
    }

    /**
     * Get the final amount of piety that will be added.
     * This can be modified by event handlers.
     */
    public double getFinalPiety() {
        return finalPiety;
    }

    /**
     * Set the final amount of piety to add.
     * Use this to modify the piety gain (e.g., bonuses, multipliers).
     */
    public void setFinalPiety(double finalPiety) {
        this.finalPiety = finalPiety;
    }
}
