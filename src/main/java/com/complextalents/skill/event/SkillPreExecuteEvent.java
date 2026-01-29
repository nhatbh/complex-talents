package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Final validation gate before skill execution.
 *
 * <p><b>Event Phase:</b> Third in the pipeline (after TargetResolutionEvent)</p>
 * <p><b>Cancelable:</b> Yes - prevents skill execution</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Check ally protection and cancel if targeting ally with hostile skill</li>
 *   <li>Check area restrictions (e.g., no skills in safe zones)</li>
 *   <li>Check conditional requirements (e.g., stealth, combo points)</li>
 *   <li>Cancel skill based on resolved target data</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onSkillPreExecute(SkillPreExecuteEvent event) {
 *     ResolvedTargetData target = event.getTargetData();
 *     if (!target.isAlly() && event.getSkillId().equals("mod:heal")) {
 *         event.setCanceled(true);
 *         event.setFailureReason("Cannot heal enemies");
 *     }
 * }
 * }</pre>
 */
@Cancelable
public class SkillPreExecuteEvent extends SkillEvent {

    private final ResolvedTargetData targetData;
    private String failureReason = "Skill execution prevented";

    /**
     * Create a new pre-execute event.
     *
     * @param player The player casting the skill
     * @param skillId The skill being cast
     * @param targetData The resolved target data
     */
    public SkillPreExecuteEvent(ServerPlayer player, ResourceLocation skillId,
                                 ResolvedTargetData targetData) {
        super(player, skillId);
        this.targetData = targetData;
    }

    /**
     * @return The resolved target data (guaranteed valid)
     */
    public ResolvedTargetData getTargetData() {
        return targetData;
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
}
