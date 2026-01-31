package com.complextalents.skill.server;

import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.SkillCastRequestEvent;
import com.complextalents.skill.event.SkillPostExecuteEvent;
import com.complextalents.skill.event.SkillToggleTerminationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for managing skill cooldowns and resource costs through the event pipeline.
 * Stage 1: Checks cooldowns and resources, cancels if not available
 * Stage 5: Applies cooldowns and consumes resources after successful execution
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class SkillCooldownHandler {

    /**
     * Stage 1: Check cooldowns and resources during cast request.
     * High priority to run before other handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSkillCastRequest(SkillCastRequestEvent event) {
        ServerPlayer player = event.getPlayer();
        ResourceLocation skillId = event.getSkillId();

        var skillDataOpt = player.getCapability(SkillDataProvider.SKILL_DATA);
        if (!skillDataOpt.isPresent()) {
            event.setCanceled(true);
            event.setFailureReason("Skill data not available");
            return;
        }

        var skillData = skillDataOpt.resolve().get();

        // Get skill from registry first
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            event.setCanceled(true);
            event.setFailureReason("Unknown skill");
            return;
        }

        // Handle toggle skills: if already active, deactivate and cancel (successful toggle-off)
        // This MUST come before cooldown check so players can toggle off even while on cooldown
        if (skill.isToggleable() && skillData.isToggleActive(skillId)) {
            event.setCanceled(true);
            skillData.setToggleActive(skillId, false);

            // Call toggle-off handler if present
            if (skill.hasToggleOffHandler()) {
                skill.executeToggleOff(player);
            }

            // Don't set failure reason - this is a successful toggle-off
            player.sendSystemMessage(Component.literal("\u00A77Toggle skill deactivated"));
            return;
        }

        // Check active cooldown
        if (skillData.isOnCooldown(skillId)) {
            event.setCanceled(true);
            double remaining = skillData.getCooldown(skillId);
            event.setFailureReason(String.format("Cooldown: %.1fs", remaining));
            return;
        }

        // Check resource cost
        if (skill.getResourceCost() > 0) {
            if (!hasEnoughResource(player, skill)) {
                event.setCanceled(true);
                String resourceName = skill.getResourceType() != null
                        ? skill.getResourceType().getPath()
                        : "resource";
                event.setFailureReason("Not enough " + resourceName);
                return;
            }
        }
    }

    /**
     * Stage 5: Apply cooldowns and consume resources after successful execution.
     * Low priority to run after other handlers.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSkillPostExecute(SkillPostExecuteEvent event) {
        if (!event.wasSuccessful()) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        ResourceLocation skillId = event.getSkillId();

        var skillDataOpt = player.getCapability(SkillDataProvider.SKILL_DATA);
        if (!skillDataOpt.isPresent()) {
            return;
        }

        var skillData = skillDataOpt.resolve().get();

        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            return;
        }

        // For toggle skills: activate toggle and apply cooldown
        // Cooldown starts when toggle is turned on
        if (skill.isToggleable()) {
            skillData.setToggleActive(skillId, true);
            if (skill.getActiveCooldown() > 0) {
                skillData.setCooldown(skillId, skill.getActiveCooldown());
            }
            player.sendSystemMessage(Component.literal("\u00A7aToggle skill activated"));
            // Consume initial resource cost for toggle-on
            if (skill.getResourceCost() > 0) {
                consumeResource(player, skill, skill.getResourceCost());
            }
            return;
        }

        // For non-toggle skills: apply cooldown
        if (skill.getActiveCooldown() > 0) {
            skillData.setCooldown(skillId, skill.getActiveCooldown());
        }

        // Consume resources
        if (skill.getResourceCost() > 0) {
            consumeResource(player, skill, skill.getResourceCost());
        }
    }

    /**
     * Handle toggle termination events.
     * Fired when skill logic requests early termination (e.g., out of resources, range, etc.)
     * Only clears toggle - cooldown already started when toggle was activated.
     */
    @SubscribeEvent
    public static void onToggleTermination(SkillToggleTerminationEvent event) {
        ServerPlayer player = event.getPlayer();
        ResourceLocation skillId = event.getSkillId();

        var skillDataOpt = player.getCapability(SkillDataProvider.SKILL_DATA);
        if (!skillDataOpt.isPresent()) {
            return;
        }

        var skillData = skillDataOpt.resolve().get();

        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            return;
        }

        // Only proceed if toggle is currently active
        if (!skillData.isToggleActive(skillId)) {
            return;
        }

        // Clear the toggle state
        skillData.setToggleActive(skillId, false);

        // Call toggle-off handler if present
        if (skill.hasToggleOffHandler()) {
            skill.executeToggleOff(player);
        }

        // Optional: send message based on termination reason
        String message = getTerminationMessage(event.getReason());
        if (message != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    /**
     * Get a message for the termination reason.
     */
    private static String getTerminationMessage(SkillToggleTerminationEvent.TerminationReason reason) {
        return switch (reason) {
            case INSUFFICIENT_RESOURCE -> "\u00A7cToggle skill deactivated: Insufficient resources.";
            case OUT_OF_RANGE -> "\u00A7cToggle skill deactivated: Out of range.";
            case LINE_OF_SIGHT_BROKEN -> "\u00A7cToggle skill deactivated: Line of sight broken.";
            case TARGET_DIED -> "\u00A7cToggle skill deactivated: Target died.";
            case INTERRUPTED -> "\u00A7cToggle skill deactivated: Interrupted.";
            case UNKNOWN -> "\u00A77Toggle skill deactivated.";
            case CUSTOM -> null; // Skill handles its own message
            case CASTER_DIED -> null; // No message if caster died
        };
    }

    /**
     * Check if player has enough of a resource.
     * This is a placeholder for integration with resource systems like Iron's Spellbooks.
     *
     * @param player The player
     * @param skill  The skill with resource cost
     * @return true if player has enough resources
     */
    private static boolean hasEnoughResource(ServerPlayer player, Skill skill) {
        ResourceLocation resourceType = skill.getResourceType();
        if (resourceType == null) {
            return true;
        }

        // Placeholder: always return true
        // Actual implementation should check the player's resource (mana, energy, etc.)
        // Example integration points:
        // - Iron's Spellbooks: player.getData(ISpellData.MANA).getMana() >= cost
        // - Minecraft Attributes: player.getAttributeValue(XAttribute) >= cost
        return true;
    }

    /**
     * Consume a resource from the player.
     * This is a placeholder for integration with resource systems.
     *
     * @param player The player
     * @param skill  The skill with resource cost
     * @param amount The amount to consume
     */
    private static void consumeResource(ServerPlayer player, Skill skill, double amount) {
        ResourceLocation resourceType = skill.getResourceType();
        if (resourceType == null) {
            return;
        }

        // Placeholder: no actual consumption
        // Actual implementation should deduct from the player's resource
        // Example integration points:
        // - Iron's Spellbooks: player.getData(ISpellData.MANA).setMana(newMana)
        // - Minecraft Attributes: apply temporary attribute modifier
    }
}
