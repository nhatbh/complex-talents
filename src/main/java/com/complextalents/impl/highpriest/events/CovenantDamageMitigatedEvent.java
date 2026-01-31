package com.complextalents.impl.highpriest.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * Fired when damage is mitigated by Covenant of Protection.
 * This event deducts Piety from the caster based on mitigated damage.
 * <p>
 * The event can be cancelled or the mitigated amount can be modified
 * by other event handlers if needed.
 */
public class CovenantDamageMitigatedEvent extends Event {

    private final LivingEntity target;
    private final UUID casterId;
    private float mitigatedDamage;
    private final int skillLevel;

    public CovenantDamageMitigatedEvent(LivingEntity target, UUID casterId,
                                       float mitigatedDamage, int skillLevel) {
        this.target = target;
        this.casterId = casterId;
        this.mitigatedDamage = mitigatedDamage;
        this.skillLevel = skillLevel;
    }

    public LivingEntity getTarget() { return target; }
    public UUID getCasterId() { return casterId; }
    public float getMitigatedDamage() { return mitigatedDamage; }
    public int getSkillLevel() { return skillLevel; }

    /**
     * Set the mitigated damage amount.
     * This can be used to modify how much damage is actually mitigated
     * (e.g., when Piety is insufficient for full mitigation).
     */
    public void setMitigatedDamage(float amount) {
        this.mitigatedDamage = amount;
    }
}
