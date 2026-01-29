package com.complextalents.skill.form;

import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Manages skill form state for players.
 * <p>
 * Handles activation, deactivation, expiration, and enhancement resolution.
 * This is the primary API for working with skill forms.
 */
public class SkillFormManager {

    /**
     * Activate a skill form for a player.
     *
     * @param player     The player activating the form
     * @param formSkillId The form skill ID
     * @return true if activation succeeded
     */
    public static boolean activateForm(ServerPlayer player, ResourceLocation formSkillId) {
        SkillFormDefinition definition = SkillFormRegistry.getInstance().getForm(formSkillId);
        if (definition == null) {
            player.sendSystemMessage(Component.literal("\u00A7cForm skill not defined: " + formSkillId));
            return false;
        }

        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return false;
        }

        // Check if already in a form
        if (data.getActiveForm() != null) {
            player.sendSystemMessage(Component.literal("\u00A7cAlready in a form! Deactivate current form first."));
            return false;
        }

        // Set the active form
        data.setActiveForm(formSkillId);

        // Calculate expiration time
        long durationTicks = (long) (definition.getDuration() * 20);
        long expirationTime = player.level().getGameTime() + durationTicks;
        data.setFormExpiration(expirationTime);

        // Activate the toggle
        data.setToggleActive(formSkillId, true);

        // Feedback
        player.sendSystemMessage(Component.literal(
                "\u00A76\u00A7lFORM ACTIVATED: " + formSkillId.getPath().toUpperCase()
        ));

        return true;
    }

    /**
     * Deactivate the current form for a player.
     *
     * @param player The player to deactivate the form for
     */
    public static void deactivateForm(ServerPlayer player) {
        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return;
        }

        ResourceLocation activeForm = data.getActiveForm();
        if (activeForm == null) {
            return;
        }

        data.setActiveForm(null);
        data.setFormExpiration(0);
        data.setToggleActive(activeForm, false);

        // Start cooldown
        SkillFormDefinition definition = SkillFormRegistry.getInstance().getForm(activeForm);
        if (definition != null) {
            data.setCooldown(activeForm, definition.getCooldown());
        }

        player.sendSystemMessage(Component.literal("\u00A7cForm ended."));
    }

    /**
     * Get the enhanced skill ID for a skill slot, if a form is active.
     *
     * @param player    The player
     * @param slotIndex The skill slot (0-2)
     * @return The enhanced skill ID, or null if no enhancement
     */
    @Nullable
    public static ResourceLocation getEnhancedSkillId(ServerPlayer player, int slotIndex) {
        // Only slots 0-2 can be enhanced
        if (slotIndex < 0 || slotIndex > 2) {
            return null;
        }

        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return null;
        }

        // Check if form is active and not expired
        ResourceLocation activeForm = data.getActiveForm();
        if (activeForm == null) {
            return null;
        }

        long currentTime = player.level().getGameTime();
        if (currentTime >= data.getFormExpiration()) {
            // Expired - deactivate
            deactivateForm(player);
            return null;
        }

        // Get the enhancement
        SkillFormDefinition definition = SkillFormRegistry.getInstance().getForm(activeForm);
        if (definition == null) {
            return null;
        }

        return definition.getEnhancedSkillId(slotIndex);
    }

    /**
     * Check if a player is currently in a form.
     *
     * @param player The player to check
     * @return true if a form is active
     */
    public static boolean isInForm(ServerPlayer player) {
        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return false;
        }

        ResourceLocation activeForm = data.getActiveForm();
        if (activeForm == null) {
            return false;
        }

        // Check expiration
        long currentTime = player.level().getGameTime();
        if (currentTime >= data.getFormExpiration()) {
            deactivateForm(player);
            return false;
        }

        return true;
    }

    /**
     * Get the remaining duration of the active form (in seconds).
     *
     * @param player The player to check
     * @return Remaining seconds, or 0 if no form is active
     */
    public static double getRemainingDuration(ServerPlayer player) {
        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return 0;
        }

        ResourceLocation activeForm = data.getActiveForm();
        if (activeForm == null) {
            return 0;
        }

        long currentTime = player.level().getGameTime();
        long expiration = data.getFormExpiration();

        if (currentTime >= expiration) {
            deactivateForm(player);
            return 0;
        }

        return (expiration - currentTime) / 20.0;
    }

    /**
     * Get the icon override for a skill slot, if a form is active (for future HUD implementation).
     *
     * @param player    The player
     * @param slotIndex The skill slot (0-2)
     * @return The icon override, or null if no override
     */
    @Nullable
    public static ResourceLocation getIconOverride(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex > 2) {
            return null;
        }

        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return null;
        }

        ResourceLocation activeForm = data.getActiveForm();
        if (activeForm == null) {
            return null;
        }

        // Check expiration
        long currentTime = player.level().getGameTime();
        if (currentTime >= data.getFormExpiration()) {
            deactivateForm(player);
            return null;
        }

        SkillFormDefinition definition = SkillFormRegistry.getInstance().getForm(activeForm);
        if (definition == null) {
            return null;
        }

        return definition.getIconOverride(slotIndex);
    }
}
