package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.client.ClientSkillData;
import com.complextalents.skill.client.SkillCastingClient;
import com.complextalents.skill.network.SkillCastPacket;
import com.complextalents.skill.network.SkillChannelStartPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side input handler for direct skill casting.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Handle skill key press for slot 0 (SKILL_1)</li>
 *   <li>Delegate channel tracking to SkillCastingClient</li>
 *   <li>Send SkillCastPacket to server</li>
 * </ul>
 *
 * <p><b>Skill Activation:</b></p>
 * <ul>
 *   <li>SKILL_1 key (default: Z) activates skill in slot 0</li>
 *   <li>For channeling skills: hold to charge, release to cast</li>
 *   <li>Right-click cancels channeling</li>
 *   <li>Key is reconfigurable in the Controls menu</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class ClientInputHandler {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Handle key input events.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (MC.screen != null || MC.player == null) {
            return;
        }

        // Check SKILL_1 key (slot 0)
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == getKeyCode(KeyBindings.SKILL_1)) {
            if (KeyBindings.SKILL_1.consumeClick()) {
                handleSkillKeyPress(0);
                return;
            }
        }
        if (event.getAction() == GLFW.GLFW_RELEASE && event.getKey() == getKeyCode(KeyBindings.SKILL_1)) {
            handleSkillKeyRelease(0);
        }
    }

    /**
     * Handle mouse input - cancel channeling on right-click.
     */
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (MC.screen != null || MC.player == null) {
            return;
        }

        // Cancel channeling on right-click
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && event.getAction() == GLFW.GLFW_PRESS
                && SkillCastingClient.isChanneling()) {
            SkillCastingClient.cancelChanneling();
            MC.player.displayClientMessage(Component.literal("§7Channeling canceled"), true);
            event.setCanceled(true);
        }
    }

    /**
     * Get the GLFW key code from a KeyMapping.
     */
    private static int getKeyCode(net.minecraft.client.KeyMapping keyMapping) {
        return keyMapping.getKey().getValue();
    }

    /**
     * Handle skill key press - request server validation before starting channeling.
     * The server will check cooldowns and resources, then respond with approval/denial.
     */
    private static void handleSkillKeyPress(int slotIndex) {
        ResourceLocation skillId = ClientSkillData.getSkillInSlot(slotIndex);
        if (skillId == null) {
            MC.player.displayClientMessage(Component.literal("§cNo skill in slot " + (slotIndex + 1)), true);
            return;
        }

        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            MC.player.displayClientMessage(Component.literal("§cUnknown skill: " + skillId), true);
            return;
        }

        // Don't allow multiple pending channel requests
        if (SkillCastingClient.hasPendingChannelStart()) {
            return;
        }

        // Mark pending state so we know we're waiting for server response
        SkillCastingClient.setPendingChannelStart(slotIndex);

        // Send request to server to validate cooldowns and resources
        // Server will respond and we'll start channeling only if approved
        PacketHandler.sendToServer(new SkillChannelStartPacket(skillId, slotIndex));
    }

    /**
     * Handle skill key release - finish channeling and send packet.
     */
    public static void handleSkillKeyRelease(int slotIndex) {
        // If player released the key before server responded, clear the pending state
        if (SkillCastingClient.hasPendingChannelStart() && SkillCastingClient.getPendingSlot() == slotIndex) {
            SkillCastingClient.clearPendingChannelStart();
            return;
        }

        if (!SkillCastingClient.isChanneling() || SkillCastingClient.getCurrentSlot() != slotIndex) {
            return;
        }

        long channelTimeMs = SkillCastingClient.endChanneling();
        double maxChannelTime = SkillCastingClient.getMaxChannelTime();

        ResourceLocation skillId = ClientSkillData.getSkillInSlot(slotIndex);
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);

        if (skill == null) {
            return;
        }

        // Validate channel time
        double validatedTime = SkillCastingClient.validateChannelTime(skill, channelTimeMs, maxChannelTime);
        if (validatedTime < 0) {
            return; // Validation failed
        }

        // Display feedback
        SkillCastingClient.displayCastFeedback(validatedTime);

        // Resolve targeting using skill's configuration and send cast packet with channel time
        PacketHandler.sendToServer(new SkillCastPacket(
                skillId, slotIndex, (int) (validatedTime * 1000),
                SkillCastingClient.resolveTargeting(skill)
        ));
    }

    // ==================== Key Mapping Registration ====================

    @Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            KeyBindings.register();
            event.register(KeyBindings.SKILL_1);
            TalentsMod.LOGGER.info("Registered skill key mappings");
        }
    }
}
