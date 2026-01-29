package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * After-effects event fired after skill execution completes.
 *
 * <p><b>Event Phase:</b> Fifth and final in the pipeline (after SkillExecuteEvent)</p>
 * <p><b>Cancelable:</b> No - this event is informational only</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Start cooldowns after successful execution</li>
 *   <li>Consume resources (mana, energy, etc.)</li>
 *   <li>Trigger passive skill effects (e.g., "casting reduces cooldown of X")</li>
 *   <li>Track statistics / achievements</li>
 *   <li>Log analytics data</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onSkillPostExecute(SkillPostExecuteEvent event) {
 *     if (event.wasSuccessful()) {
 *         // Start cooldown
 *         CooldownManager.startCooldown(event.getPlayer(), event.getSkillId(), 5.0);
 *
 *         // Consume mana
 *         ManaData.get(event.getPlayer()).consume(50);
 *     }
 * }
 * }</pre>
 */
public class SkillPostExecuteEvent extends SkillEvent {

    private final ResolvedTargetData targetData;
    private final boolean successful;

    /**
     * Create a new post-execute event.
     *
     * @param player The player who cast the skill
     * @param skillId The skill that was cast
     * @param targetData The resolved target data
     * @param successful Whether the skill execution was successful (not canceled)
     */
    public SkillPostExecuteEvent(ServerPlayer player, ResourceLocation skillId,
                                  ResolvedTargetData targetData, boolean successful) {
        super(player, skillId);
        this.targetData = targetData;
        this.successful = successful;
    }

    /**
     * @return The resolved target data
     */
    public ResolvedTargetData getTargetData() {
        return targetData;
    }

    /**
     * @return true if the skill executed successfully (was not canceled)
     */
    public boolean wasSuccessful() {
        return successful;
    }
}
