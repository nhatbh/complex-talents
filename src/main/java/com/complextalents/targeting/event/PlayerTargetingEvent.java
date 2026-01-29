package com.complextalents.targeting.event;

import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

import java.util.EnumSet;
import java.util.Set;

/**
 * Server-side event fired after successful targeting validation.
 *
 * <p>This is the primary integration point for skills to respond to
 * player targeting. Skills should listen to this event and execute
 * their effects when triggered.</p>
 *
 * <p><b>Event Flow:</b></p>
 * <ol>
 *   <li>Client sends {@code SkillUsePacket} with targeting snapshot</li>
 *   <li>Server validates (skill exists, cooldown ready, resources available)</li>
 *   <li>Server fires {@code PlayerTargetingEvent}</li>
 *   <li>Skills consume the event and apply effects</li>
 * </ol>
 *
 * <p>Skills receive the targeting data ready-to-use without needing to
 * know about raycasts, networking, or client-side calculations.</p>
 *
 * <p>This event is not cancelable by default. Individual skills can choose
 * to ignore it, but the event will still be fired to other subscribers.</p>
 */
public class PlayerTargetingEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation skillId;
    private final Vec3 aimDirection;
    private final Vec3 targetPosition;
    private final Entity targetEntity;
    private final boolean isAlly;
    private final Set<TargetType> resolvedTypes;

    /**
     * Create a new player targeting event.
     *
     * @param player The player who activated the skill
     * @param skillId The ID of the skill being activated
     * @param aimDirection The player's aim direction (normalized)
     * @param targetPosition The resolved target position
     * @param targetEntity The targeted entity (may be null)
     * @param isAlly Whether the target is considered an ally
     * @param resolvedTypes The set of resolved target types
     */
    public PlayerTargetingEvent(
            ServerPlayer player,
            ResourceLocation skillId,
            Vec3 aimDirection,
            Vec3 targetPosition,
            Entity targetEntity,
            boolean isAlly,
            EnumSet<TargetType> resolvedTypes
    ) {
        this.player = player;
        this.skillId = skillId;
        this.aimDirection = aimDirection;
        this.targetPosition = targetPosition;
        this.targetEntity = targetEntity;
        this.isAlly = isAlly;
        this.resolvedTypes = EnumSet.copyOf(resolvedTypes);
    }

    /**
     * @return The player who activated the skill
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * @return The ID of the skill being activated
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }

    /**
     * @return The player's aim direction (normalized vector)
     */
    public Vec3 getAimDirection() {
        return aimDirection;
    }

    /**
     * @return The resolved target position
     */
    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    /**
     * @return The targeted entity, or null if no entity is targeted
     */
    public Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * @return Whether a target entity is present
     */
    public boolean hasTargetEntity() {
        return targetEntity != null;
    }

    /**
     * @return Whether the target (if any) is considered an ally
     */
    public boolean isAlly() {
        return isAlly;
    }

    /**
     * @return The set of resolved target types
     */
    public Set<TargetType> getResolvedTypes() {
        return EnumSet.copyOf(resolvedTypes);
    }

    /**
     * Check if a specific target type was resolved.
     *
     * @param type The target type to check
     * @return true if this type was resolved
     */
    public boolean hasType(TargetType type) {
        return resolvedTypes.contains(type);
    }

    /**
     * Check if this event has at least one of the specified target types.
     *
     * @param types The target types to check for
     * @return true if at least one of the specified types is present
     */
    public boolean hasAnyOf(TargetType... types) {
        for (TargetType type : types) {
            if (resolvedTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this event has all of the specified target types.
     *
     * @param types The target types to check for
     * @return true if all specified types are present
     */
    public boolean hasAllOf(TargetType... types) {
        for (TargetType type : types) {
            if (!resolvedTypes.contains(type)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "PlayerTargetingEvent{" +
                "player=" + player.getGameProfile().getName() +
                ", skillId=" + skillId +
                ", targetPosition=" + targetPosition +
                ", targetEntity=" + (targetEntity != null ? targetEntity.getType().getDescriptionId() : "none") +
                ", isAlly=" + isAlly +
                ", resolvedTypes=" + resolvedTypes +
                '}';
    }
}
