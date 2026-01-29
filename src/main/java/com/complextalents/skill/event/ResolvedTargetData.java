package com.complextalents.skill.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Unified target data provided to all skills during execution.
 *
 * <p>This class guarantees:</p>
 * <ul>
 *   <li>targetEntity MAY be null if client decided not to fall back to self</li>
 *   <li>targetPosition is ALWAYS present</li>
 *   <li>aimDirection is ALWAYS present</li>
 *   <li>isSelfTarget clearly indicates if caster is the target</li>
 *   <li>isAlly indicates target relationship</li>
 * </ul>
 *
 * <p><b>Note:</b> The client decides whether to fall back to self based on skill
 * configuration. Skills that require a valid target should check {@link #hasEntity()}
 * before accessing the target entity.</p>
 */
public class ResolvedTargetData {

    private final ServerPlayer caster;
    private final Entity targetEntity;
    private final Vec3 targetPosition;
    private final Vec3 aimDirection;
    private final boolean isSelfTarget;
    private final boolean isAlly;
    private final double distance;

    /**
     * Create resolved target data with all fields.
     */
    public ResolvedTargetData(
            ServerPlayer caster,
            Entity targetEntity,
            Vec3 targetPosition,
            Vec3 aimDirection,
            boolean isSelfTarget,
            boolean isAlly,
            double distance
    ) {
        this.caster = caster;
        this.targetEntity = targetEntity;
        this.targetPosition = targetPosition;
        this.aimDirection = aimDirection;
        this.isSelfTarget = isSelfTarget;
        this.isAlly = isAlly;
        this.distance = distance;
    }

    /**
     * @return The player casting the skill
     */
    public ServerPlayer getCaster() {
        return caster;
    }

    /**
     * @return The target entity (may be null if fallbackToSelf is disabled)
     */
    public Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * @return true if a valid target entity exists (not null)
     */
    public boolean hasEntity() {
        return targetEntity != null;
    }

    /**
     * @return The target position (ALWAYS present)
     */
    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    /**
     * @return The aim direction (ALWAYS present, normalized)
     */
    public Vec3 getAimDirection() {
        return aimDirection;
    }

    /**
     * @return true if the caster is targeting themselves
     */
    public boolean isSelfTarget() {
        return isSelfTarget;
    }

    /**
     * @return true if the target is considered an ally
     */
    public boolean isAlly() {
        return isAlly;
    }

    /**
     * @return The distance from caster to target
     */
    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "ResolvedTargetData{" +
                "caster=" + caster.getGameProfile().getName() +
                ", targetEntity=" + (targetEntity != null ? targetEntity.getType().getDescriptionId() : "null") +
                ", targetPosition=" + targetPosition +
                ", isSelfTarget=" + isSelfTarget +
                ", isAlly=" + isAlly +
                ", distance=" + distance +
                '}';
    }
}
