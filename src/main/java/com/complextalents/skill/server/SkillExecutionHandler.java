package com.complextalents.skill.server;

import com.complextalents.origin.OriginManager;
import com.complextalents.origin.ResourceType;
import com.complextalents.skill.BuiltSkill;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.skill.event.SkillExecuteEvent;
import com.complextalents.skill.form.SkillFormManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central handler for executing skills through the event pipeline.
 * Stage 4: Executes skill active handlers.
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class SkillExecutionHandler {

    /**
     * Stage 4: Execute skill effects.
     * This is the PRIMARY integration point for all skills.
     */
    @SubscribeEvent
    public static void onSkillExecute(SkillExecuteEvent event) {
        ResourceLocation skillId = event.getSkillId();
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);

        if (skill == null) {
            // Unknown skill - log and cancel
            event.setCanceled(true);
            return;
        }

        // Only execute if this skill has an active component
        if (!skill.getNature().hasActive()) {
            // Passive-only skills shouldn't be cast
            event.setCanceled(true);
            return;
        }

        ServerPlayer player = event.getPlayer();

        // Get skill level for scaling
        int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(skillId))
                .orElse(1);

        // Validate resource cost (check against origin resource)
        double cost = skill.getResourceCost(skillLevel);
        ResourceLocation costResourceType = skill.getResourceType();
        if (cost > 0 && costResourceType != null) {
            // Get player's active origin resource type
            ResourceType playerResource = OriginManager.getResourceType(player);
            if (playerResource != null && playerResource.getId().equals(costResourceType)) {
                // Skill costs origin resource - check player has enough
                double currentResource = OriginManager.getResource(player);
                if (currentResource < cost) {
                    player.sendSystemMessage(Component.literal("\u00A7cNot enough " + playerResource.getName() + "!"));
                    event.setCanceled(true);
                    return;
                }
                // Consume resource after successful execution (handled after validation)
            }
            // TODO: Later integrate with Iron's Spellbooks mana for non-origin resources
        }

        // Check for form enhancement - if this skill is in slots 0-2 and a form is active
        int slotIndex = findSlotForSkill(player, skillId);
        if (slotIndex >= 0 && slotIndex <= 2) {
            ResourceLocation enhancedSkillId = SkillFormManager.getEnhancedSkillId(player, slotIndex);
            if (enhancedSkillId != null) {
                // Execute the enhanced skill instead
                Skill enhancedSkill = SkillRegistry.getInstance().getSkill(enhancedSkillId);
                if (enhancedSkill != null) {
                    // Cancel the event to prevent other listeners from executing the original skill
                    event.setCanceled(true);

                    // Get the active form skill ID to use its level for stat scaling
                    ResourceLocation formSkillId = player.getCapability(SkillDataProvider.SKILL_DATA)
                            .map(data -> data.getActiveForm())
                            .orElse(null);

                    Skill.ExecutionContext context;
                    if (formSkillId != null) {
                        // Use form skill's level for stat resolution (enhanced skills scale with form power)
                        context = createExecutionContextForSkillWithFormLevel(
                                event, enhancedSkillId, formSkillId);
                    } else {
                        // Fallback: use enhanced skill's own level
                        context = createExecutionContextForSkill(event, enhancedSkillId);
                    }

                    // Check validation before executing enhanced skill
                    if (!enhancedSkill.canCast(context)) {
                        return;
                    }

                    enhancedSkill.executeActive(context);

                    // Consume resource after successful execution
                    consumeOriginResource(player, enhancedSkill);

                    return;  // Skip normal execution
                }
            }
        }

        // Normal execution path
        if (skill instanceof BuiltSkill builtSkill) {
            if (builtSkill.hasActiveHandler()) {
                // Create execution context
                Skill.ExecutionContext context = createExecutionContextForSkill(event, skillId);

                // Check validation before executing
                if (!builtSkill.canCast(context)) {
                    event.setCanceled(true);
                    return;
                }

                // Execute the skill
                if (builtSkill.getNature() == com.complextalents.skill.SkillNature.CHARGE) {
                    // For charge skills, this event is the RELEASE phase
                    builtSkill.executeRelease(context, context.channelTime());
                } else {
                    // Normal active execution
                    builtSkill.executeActive(context);
                }

                // Consume resource after successful execution
                consumeOriginResource(player, builtSkill);
            } else {
                // Skill has no handler registered
                event.setCanceled(true);
            }
        }
        // For custom skill implementations, they should have their own event listeners
        // This allows for more complex skill behaviors
    }

    /**
     * Triggered when a CHARGE skill starts its charging phase.
     * This triggers the activeHandler of the skill.
     *
     * @param player  The player starting the charge
     * @param skillId The skill ID
     */
    public static void handleChargeStart(ServerPlayer player, ResourceLocation skillId) {
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill instanceof BuiltSkill builtSkill && builtSkill.getNature() == com.complextalents.skill.SkillNature.CHARGE) {
            // Check for enhanced skill first
            int slotIndex = findSlotForSkill(player, skillId);
            Skill actualSkill = builtSkill;
            ResourceLocation actualSkillId = skillId;

            if (slotIndex >= 0 && slotIndex <= 2) {
                ResourceLocation enhancedId = SkillFormManager.getEnhancedSkillId(player, slotIndex);
                if (enhancedId != null) {
                    Skill enhanced = SkillRegistry.getInstance().getSkill(enhancedId);
                    if (enhanced != null) {
                        actualSkill = (BuiltSkill) enhanced;
                        actualSkillId = enhancedId;
                    }
                }
            }

            // Create context (with 0 channel time as it just started)
            Skill.ExecutionContext context = createExecutionContextForSkill(player, actualSkillId, 0.0);

            // Execute the START phase (using activeHandler)
            actualSkill.executeActive(context);
        }
    }

    /**
     * Create an execution context for a specific skill ID.
     * This allows executing a different skill than the one in the event.
     */
    private static Skill.ExecutionContext createExecutionContextForSkill(SkillExecuteEvent event, ResourceLocation skillId) {
        ServerPlayer player = event.getPlayer();
        ResolvedTargetData targetData = event.getTargetData();
        double channelTime = event.getChannelTime();

        // Get player's skill level from capability for the specified skill
        int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(skillId))
                .orElse(1);

        Skill.ServerPlayerWrapper playerWrapper = new Skill.ServerPlayerWrapper(player);
        Skill.ResolvedTargetWrapper targetWrapper = new Skill.ResolvedTargetWrapper(targetData);

        return new Skill.ExecutionContext(playerWrapper, targetWrapper, skillId, skillLevel, channelTime);
    }

    /**
     * Create an execution context for a specific skill ID and player.
     */
    private static Skill.ExecutionContext createExecutionContextForSkill(ServerPlayer player, ResourceLocation skillId, double channelTime) {
        // Get player's skill level from capability for the specified skill
        int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(skillId))
                .orElse(1);

        Skill.ServerPlayerWrapper playerWrapper = new Skill.ServerPlayerWrapper(player);
        // During start-charge, we might not have target data yet, use dummy
        Skill.ResolvedTargetWrapper targetWrapper = new Skill.ResolvedTargetWrapper(
                new ResolvedTargetData(player, player, player.position(), player.getLookAngle(), true, true, 0.0)
        );

        return new Skill.ExecutionContext(playerWrapper, targetWrapper, skillId, skillLevel, channelTime);
    }

    /**
     * Create an execution context for an enhanced skill using the form skill's level for stat scaling.
     * <p>
     * When a form is active, enhanced skills should scale with the form skill's level,
     * not their own individual level. This allows the form's power to determine the
     * enhanced skills' effectiveness.
     *
     * @param event        The skill execute event
     * @param enhancedSkillId The enhanced skill ID (which skill to execute)
     * @param formSkillId    The form skill ID (whose level to use for stat scaling)
     * @return An execution context with the enhanced skill ID but form skill's level
     */
    private static Skill.ExecutionContext createExecutionContextForSkillWithFormLevel(
            SkillExecuteEvent event, ResourceLocation enhancedSkillId, ResourceLocation formSkillId) {
        ServerPlayer player = event.getPlayer();
        ResolvedTargetData targetData = event.getTargetData();
        double channelTime = event.getChannelTime();

        // Get player's form skill level (not the enhanced skill's level)
        // This allows enhanced skills to scale with the form's power
        int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(formSkillId))
                .orElse(1);

        Skill.ServerPlayerWrapper playerWrapper = new Skill.ServerPlayerWrapper(player);
        Skill.ResolvedTargetWrapper targetWrapper = new Skill.ResolvedTargetWrapper(targetData);

        // Pass enhanced skill ID but form skill's level for stat resolution
        return new Skill.ExecutionContext(playerWrapper, targetWrapper, enhancedSkillId, skillLevel, channelTime);
    }

    /**
     * Find which slot a skill is being cast from.
     * This is needed to determine if the skill should be enhanced by an active form.
     *
     * @param player  The player casting the skill
     * @param skillId The skill ID to find
     * @return The slot index (0-3), or -1 if not found in any slot
     */
    private static int findSlotForSkill(ServerPlayer player, ResourceLocation skillId) {
        return player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> {
                    for (int i = 0; i < IPlayerSkillData.SLOT_COUNT; i++) {
                        if (skillId.equals(data.getSkillInSlot(i))) {
                            return i;
                        }
                    }
                    return -1;
                })
                .orElse(-1);
    }

    /**
     * Consume origin resource after successful skill execution.
     *
     * @param player The player casting the skill
     * @param skill  The skill being cast
     */
    private static void consumeOriginResource(ServerPlayer player, Skill skill) {
        ResourceLocation skillId = skill.getId();
        // Get skill level for scaling
        int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(skillId))
                .orElse(1);

        double cost = skill.getResourceCost(skillLevel);
        ResourceLocation costResourceType = skill.getResourceType();
        if (cost > 0 && costResourceType != null) {
            ResourceType playerResource = OriginManager.getResourceType(player);
            if (playerResource != null && playerResource.getId().equals(costResourceType)) {
                OriginManager.consumeResource(player, cost);
            }
        }
    }
}
