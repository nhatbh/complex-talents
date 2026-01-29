package com.complextalents.targeting.server;

import com.complextalents.targeting.*;
import com.complextalents.targeting.event.PlayerTargetingEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

/**
 * Server-side handler for skill use packets.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Validate skill exists and player owns it</li>
 *   <li>Validate cooldown is ready</li>
 *   <li>Validate resource cost is available</li>
 *   <li>Validate entity ID is still valid (if present)</li>
 *   <li>Fire {@link PlayerTargetingEvent} for skills to consume</li>
 * </ul>
 *
 * <p><b>Important:</b> The server does NOT recalculate raycasts or aim direction.
 * It trusts the client's targeting snapshot after performing minimal validation.</p>
 */
public class ServerTargetingHandler {

    /**
     * Handle a skill use packet from the client.
     *
     * @param player The player who sent the packet
     * @param skillId The ID of the skill being activated
     * @param snapshot The targeting snapshot from the client
     */
    public static void handleSkillUse(ServerPlayer player, ResourceLocation skillId, TargetingSnapshot snapshot) {
        Level level = player.level();

        // Validation Phase
        ValidationResult validation = validateSkillUse(player, skillId, snapshot);

        if (!validation.isValid()) {
            // Optional: Send failure feedback to client
            onValidationFailed(player, skillId, validation);
            return;
        }

        // Apply resource costs (cooldowns, mana, etc.)
        applyResourceCosts(player, skillId);

        // Resolve the target entity if present
        Entity targetEntity = null;
        if (snapshot.hasEntity()) {
            targetEntity = level.getEntity(snapshot.getTargetEntityId());

            // Additional validation: ensure entity still exists and is valid
            if (targetEntity == null || !targetEntity.isAlive()) {
                onValidationFailed(player, skillId, ValidationResult.invalid("Target entity no longer exists"));
                return;
            }
        }

        // Fire the targeting event for skills to consume
        PlayerTargetingEvent event = new PlayerTargetingEvent(
                player,
                skillId,
                snapshot.getAimDirection(),
                snapshot.getTargetPosition(),
                targetEntity,
                snapshot.isAlly(),
                snapshot.getResolvedTypes()
        );

        MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * Validate that the skill use is allowed.
     *
     * @param player The player
     * @param skillId The skill ID
     * @param snapshot The targeting snapshot
     * @return Validation result
     */
    private static ValidationResult validateSkillUse(ServerPlayer player, ResourceLocation skillId, TargetingSnapshot snapshot) {
        // TODO: Implement actual skill registry lookup when available
        // For now, perform basic validation

        // Check if skill exists (placeholder - integrate with your skill system)
        // if (!SkillRegistry.INSTANCE.hasSkill(skillId)) {
        //     return ValidationResult.invalid("Unknown skill: " + skillId);
        // }

        // Check if player owns the skill (placeholder)
        // if (!PlayerSkillData.get(player).hasSkill(skillId)) {
        //     return ValidationResult.invalid("Player does not own skill: " + skillId);
        // }

        // Check cooldown (placeholder)
        // if (!PlayerSkillData.get(player).isCooldownReady(skillId)) {
        //     return ValidationResult.invalid("Skill is on cooldown");
        // }

        // Check resource costs (placeholder)
        // if (!PlayerSkillData.get(player).hasResourcesFor(skillId)) {
        //     return ValidationResult.invalid("Insufficient resources");
        // }

        // Validate entity target if present
        if (snapshot.hasEntity()) {
            Entity target = player.level().getEntity(snapshot.getTargetEntityId());

            if (target == null) {
                return ValidationResult.invalid("Target entity not found");
            }

            if (!target.isAlive()) {
                return ValidationResult.invalid("Target entity is dead");
            }

            // Optional: Validate distance (cheat prevention)
            // double actualDistance = player.position().distanceTo(target.position());
            // if (actualDistance > snapshot.getDistance() * 1.5) {
            //     return ValidationResult.invalid("Target distance mismatch (possible cheat)");
            // }
        }

        return ValidationResult.valid();
    }

    /**
     * Apply resource costs for using a skill.
     *
     * @param player The player
     * @param skillId The skill ID
     */
    private static void applyResourceCosts(ServerPlayer player, ResourceLocation skillId) {
        // TODO: Implement resource cost application
        // PlayerSkillData.get(player).consumeResources(skillId);
        // PlayerSkillData.get(player).setCooldown(skillId, calculateCooldown(skillId));
    }

    /**
     * Handle validation failure.
     *
     * @param player The player
     * @param skillId The skill ID
     * @param validation The validation result
     */
    private static void onValidationFailed(ServerPlayer player, ResourceLocation skillId, ValidationResult validation) {
        // TODO: Send failure feedback packet to client
        // Could include visual effects, sound, or chat message

        // Log for debugging (disable in production)
        // player.level().getCraftingManager()
        //     .getLogger().debug("Skill use failed: " + validation.getReason());
    }

    /**
     * Simple validation result holder.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
