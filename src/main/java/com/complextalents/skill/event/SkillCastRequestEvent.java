package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired when a player attempts to cast a skill, before any processing.
 *
 * <p><b>Event Phase:</b> First in the pipeline</p>
 * <p><b>Cancelable:</b> Yes - prevents skill execution</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Check cooldowns and cancel if not ready</li>
 *   <li>Check resource costs (mana, energy, etc.) and cancel if insufficient</li>
 *   <li>Check for silence/stun effects and cancel if prevented</li>
 *   <li>Check player permissions and cancel if not allowed</li>
 *   <li>Set custom failure reason for client feedback</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onSkillCastRequest(SkillCastRequestEvent event) {
 *     if (!CooldownManager.isReady(event.getPlayer(), event.getSkillId())) {
 *         event.setCanceled(true);
 *         event.setFailureReason("Cooldown not ready");
 *     }
 * }
 * }</pre>
 */
@Cancelable
public class SkillCastRequestEvent extends SkillEvent {

    private final int slotIndex;
    private String failureReason = "Skill cast failed";

    /**
     * Create a new skill cast request event.
     *
     * @param player The player attempting to cast
     * @param skillId The skill being cast
     * @param slotIndex The hotbar slot index (0-3)
     */
    public SkillCastRequestEvent(ServerPlayer player, ResourceLocation skillId, int slotIndex) {
        super(player, skillId);
        this.slotIndex = slotIndex;
    }

    /**
     * @return The hotbar slot index (0-3 for combat mode)
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * @return The reason for failure if this event is canceled
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Set the failure reason for client feedback.
     *
     * @param reason The failure message
     */
    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    @Override
    public String toString() {
        return "SkillCastRequestEvent{" +
                "player=" + player.getGameProfile().getName() +
                ", skillId=" + skillId +
                ", slotIndex=" + slotIndex +
                '}';
    }
}
