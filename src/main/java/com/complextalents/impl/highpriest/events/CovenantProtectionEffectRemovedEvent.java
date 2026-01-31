package com.complextalents.impl.highpriest.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * Fired when the Covenant Protection effect is removed from a target.
 * This handles all removal reasons: expired, out of range, no piety, manually toggled off.
 */
public class CovenantProtectionEffectRemovedEvent extends Event {

    private final LivingEntity target;
    private final UUID casterId;
    private final int skillLevel;
    private final RemovalReason reason;

    public CovenantProtectionEffectRemovedEvent(LivingEntity target, UUID casterId,
                                                int skillLevel, RemovalReason reason) {
        this.target = target;
        this.casterId = casterId;
        this.skillLevel = skillLevel;
        this.reason = reason;
    }

    public LivingEntity getTarget() { return target; }
    public UUID getCasterId() { return casterId; }
    public int getSkillLevel() { return skillLevel; }
    public RemovalReason getReason() { return reason; }

    public enum RemovalReason {
        EXPIRED,         // Duration ran out naturally
        OUT_OF_RANGE,    // Caster moved out of range
        NO_PIETY,        // Caster ran out of Piety
        MANUALLY_TOGGLED_OFF, // Caster manually deactivated
        CASTER_GONE,     // Caster died or logged out
        UNKNOWN          // Default fallback
    }
}
