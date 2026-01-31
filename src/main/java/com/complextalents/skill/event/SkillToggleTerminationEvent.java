package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a toggle skill needs to be terminated early.
 * <p>
 * This event is fired by skill logic when a toggle needs to end prematurely
 * (e.g., running out of resources, going out of range, etc.).
 * The toggle system will handle:
 * <ul>
 *   <li>Removing the toggle state</li>
 *   <li>Calling the skill's toggle-off handler</li>
 *   <li>Starting the cooldown</li>
 * </ul>
 *
 * <p><b>Event Phase:</b> Dynamic - fired when skill logic requests termination</p>
 * <p><b>Cancelable:</b> No</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Skill runs out of sustaining resources</li>
 *   <li>Skill conditions are no longer met (range, line of sight, etc.)</li>
 *   <li>Skill is interrupted by external factors</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * // In skill logic - when Piety runs out
 * if (currentPiety <= 0) {
 *     MinecraftForge.EVENT_BUS.post(
 *         new SkillToggleTerminationEvent(player, skillId, TerminationReason.INSUFFICIENT_RESOURCE)
 *     );
 * }
 * }</pre>
 */
public class SkillToggleTerminationEvent extends SkillEvent {

    private final TerminationReason reason;

    /**
     * Create a new toggle termination event.
     *
     * @param player The player who owns the toggle skill
     * @param skillId The skill being terminated
     * @param reason The reason for termination
     */
    public SkillToggleTerminationEvent(ServerPlayer player, ResourceLocation skillId, TerminationReason reason) {
        super(player, skillId);
        this.reason = reason;
    }

    /**
     * Create a new toggle termination event with UNKNOWN reason.
     *
     * @param player The player who owns the toggle skill
     * @param skillId The skill being terminated
     */
    public SkillToggleTerminationEvent(ServerPlayer player, ResourceLocation skillId) {
        this(player, skillId, TerminationReason.UNKNOWN);
    }

    /**
     * @return The reason why the toggle was terminated
     */
    public TerminationReason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "SkillToggleTerminationEvent{" +
                "player=" + player.getGameProfile().getName() +
                ", skillId=" + skillId +
                ", reason=" + reason +
                '}';
    }

    /**
     * Reasons why a toggle skill can be terminated early.
     */
    public enum TerminationReason {
        /**
         * Termination reason is unknown or not specified.
         */
        UNKNOWN,

        /**
         * Player ran out of the required resource (mana, energy, etc.).
         */
        INSUFFICIENT_RESOURCE,

        /**
         * Player moved out of range of the target or effect area.
         */
        OUT_OF_RANGE,

        /**
         * Line of sight was broken.
         */
        LINE_OF_SIGHT_BROKEN,

        /**
         * The target of the toggle skill died or was removed.
         */
        TARGET_DIED,

        /**
         * The caster died.
         */
        CASTER_DIED,

        /**
         * The toggle was interrupted (stun, silence, etc.).
         */
        INTERRUPTED,

        /**
         * Custom skill-specific reason.
         */
        CUSTOM
    }
}
