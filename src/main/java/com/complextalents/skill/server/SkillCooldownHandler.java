package com.complextalents.skill.server;

import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.SkillCastRequestEvent;
import com.complextalents.skill.event.SkillPostExecuteEvent;
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

        // Check active cooldown
        if (skillData.isOnCooldown(skillId)) {
            event.setCanceled(true);
            double remaining = skillData.getCooldown(skillId);
            event.setFailureReason(String.format("Cooldown: %.1fs", remaining));
            return;
        }

        // Get skill from registry
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            event.setCanceled(true);
            event.setFailureReason("Unknown skill");
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

        // Handle toggle skills: if already active, deactivate and cancel (successful toggle-off)
        if (skill.isToggleable() && skillData.isToggleActive(skillId)) {
            event.setCanceled(true);
            skillData.setToggleActive(skillId, false);
            // Don't set failure reason - this is a successful toggle-off
            player.sendSystemMessage(Component.literal("\u00A77Toggle skill deactivated"));
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

        // Apply active cooldown
        if (skill.getActiveCooldown() > 0) {
            skillData.setCooldown(skillId, skill.getActiveCooldown());
        }

        // Activate toggle if applicable
        if (skill.isToggleable()) {
            skillData.setToggleActive(skillId, true);
            player.sendSystemMessage(Component.literal("\u00A7aToggle skill activated"));
        }

        // Consume resources
        if (skill.getResourceCost() > 0) {
            consumeResource(player, skill, skill.getResourceCost());
        }
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
