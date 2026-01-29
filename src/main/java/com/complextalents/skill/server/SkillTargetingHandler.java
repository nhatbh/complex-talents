package com.complextalents.skill.server;

import com.complextalents.skill.*;
import com.complextalents.skill.event.*;
import com.complextalents.targeting.TargetType;
import com.complextalents.targeting.TargetingSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Server-side handler for skill casting.
 *
 * <p>Processes skill casting entirely via events:</p>
 * <pre>
 * SkillCastPacket
 *   ↓
 * SkillCastRequestEvent (cooldowns, resources, permissions)
 *   ↓
 * TargetResolutionEvent (normalizes targeting data)
 *   ↓
 * SkillPreExecuteEvent (final validation gate)
 *   ↓
 * SkillExecuteEvent (core execution)
 *   ↓
 * SkillPostExecuteEvent (after-effects)
 * </pre>
 */
public class SkillTargetingHandler {

    /**
     * Handle a skill cast packet from the client.
     *
     * @param player The player who cast the skill
     * @param skillId The ID of the skill being cast
     * @param slotIndex The hotbar slot index (0-3)
     * @param snapshot The targeting snapshot from client resolution
     * @param channelTime The channel time in seconds (0 for instant skills)
     */
    public static void handleSkillCast(ServerPlayer player, ResourceLocation skillId,
                                        int slotIndex, TargetingSnapshot snapshot, double channelTime) {

        player.sendSystemMessage(Component.literal("§7[DEBUG] §f=== SKILL CAST START ==="));
        player.sendSystemMessage(Component.literal("§7[DEBUG] §eSkill: " + skillId + " | Channel: " + String.format("%.3f", channelTime) + "s"));

        // Get skill nature and targeting type (placeholder - integrate with your skill registry)
        SkillNature nature = getSkillNature(skillId);
        TargetType targetingType = getTargetingType(skillId);

        player.sendSystemMessage(Component.literal("§7[DEBUG] §9Nature: " + nature + " | TargetingType: " + targetingType));

        // Passives shouldn't send cast packets
        if (nature == SkillNature.PASSIVE) {
            player.sendSystemMessage(Component.literal("§c[DEBUG] Passive skill - ignoring cast attempt"));
            return;
        }

        // Stage 1: Cast Request Event
        // Used for: cooldowns, resource checks, silence/stun, permissions
        player.sendSystemMessage(Component.literal("§7[DEBUG] §6Stage 1: SkillCastRequestEvent"));
        SkillCastRequestEvent requestEvent = new SkillCastRequestEvent(player, skillId, slotIndex);
        MinecraftForge.EVENT_BUS.post(requestEvent);

        if (requestEvent.isCanceled()) {
            // Cast failed at request stage (cooldown, no resources, etc.)
            player.sendSystemMessage(Component.literal("§c[DEBUG] Cast CANCELED at request stage: " + requestEvent.getFailureReason()));
            sendCastFailure(player, skillId, requestEvent.getFailureReason());
            return;
        }
        player.sendSystemMessage(Component.literal("§7[DEBUG] §aRequest stage PASSED"));

        // Stage 2: Target Resolution Event
        // Normalizes targeting data into guaranteed-valid form
        player.sendSystemMessage(Component.literal("§7[DEBUG] §6Stage 2: TargetResolutionEvent"));
        TargetResolutionEvent resolutionEvent = new TargetResolutionEvent(
                player,
                skillId,
                targetingType,
                snapshot
        );
        MinecraftForge.EVENT_BUS.post(resolutionEvent);

        // Get resolved target data
        ResolvedTargetData targetData = resolutionEvent.getResolvedTarget();

        player.sendSystemMessage(Component.literal("§7[DEBUG] §aResolved Target: " +
                (targetData.isSelfTarget() ? "SELF" : (targetData.hasEntity() ? targetData.getTargetEntity().getName().getString() : "NONE")) +
                " | Ally: " + targetData.isAlly() +
                " | Dist: " + String.format("%.1f", targetData.getDistance())));

        // Validate: ENTITY-targeted skills must have an entity target
        if (targetingType == TargetType.ENTITY && !targetData.hasEntity()) {
            player.sendSystemMessage(Component.literal("§c[DEBUG] Cast CANCELED: No entity target for ENTITY-targeted skill"));
            sendCastFailure(player, skillId, "No target");
            return;
        }

        // Stage 3: Pre-Execute Event
        // Final validation gate: ally protection, area restrictions, conditional failures
        player.sendSystemMessage(Component.literal("§7[DEBUG] §6Stage 3: SkillPreExecuteEvent"));
        SkillPreExecuteEvent preEvent = new SkillPreExecuteEvent(
                player,
                skillId,
                targetData
        );
        MinecraftForge.EVENT_BUS.post(preEvent);

        if (preEvent.isCanceled()) {
            // Cast failed at pre-execution stage
            player.sendSystemMessage(Component.literal("§c[DEBUG] Cast CANCELED at pre-execute stage: " + preEvent.getFailureReason()));
            sendCastFailure(player, skillId, preEvent.getFailureReason());
            return;
        }
        player.sendSystemMessage(Component.literal("§7[DEBUG] §aPre-execute stage PASSED"));

        // Stage 4: Execute Event
        // Core execution - skills listen for this event
        player.sendSystemMessage(Component.literal("§7[DEBUG] §6Stage 4: SkillExecuteEvent"));
        SkillExecuteEvent executeEvent = new SkillExecuteEvent(
                player,
                skillId,
                targetData,
                channelTime
        );
        MinecraftForge.EVENT_BUS.post(executeEvent);

        if (executeEvent.isCanceled()) {
            player.sendSystemMessage(Component.literal("§c[DEBUG] Execution was CANCELED"));
        } else {
            player.sendSystemMessage(Component.literal("§7[DEBUG] §aExecution completed"));
        }

        // Stage 5: Post-Execute Event
        // After-effects: cooldowns, resource consumption, passive triggers
        player.sendSystemMessage(Component.literal("§7[DEBUG] §6Stage 5: SkillPostExecuteEvent"));
        SkillPostExecuteEvent postEvent = new SkillPostExecuteEvent(
                player,
                skillId,
                targetData,
                !executeEvent.isCanceled()
        );
        MinecraftForge.EVENT_BUS.post(postEvent);

        player.sendSystemMessage(Component.literal("§7[DEBUG] §f=== SKILL CAST END ==="));
    }

    /**
     * Get the skill nature for a given skill ID.
     */
    private static SkillNature getSkillNature(ResourceLocation skillId) {
        return SkillRegistry.getInstance().getSkillNature(skillId);
    }

    /**
     * Get the targeting type for a given skill ID.
     */
    private static TargetType getTargetingType(ResourceLocation skillId) {
        return SkillRegistry.getInstance().getTargetingType(skillId);
    }

    /**
     * Send a cast failure notification to the client.
     * TODO: Implement failure feedback packet.
     */
    private static void sendCastFailure(ServerPlayer player, ResourceLocation skillId, String reason) {
        // For now, just log to chat
        player.sendSystemMessage(Component.literal("§cCast failed: " + reason));
    }
}
