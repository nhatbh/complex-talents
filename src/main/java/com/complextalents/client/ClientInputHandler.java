package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.ToggleCombatModePacket;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.client.ClientSkillData;
import com.complextalents.skill.client.SkillCastingClient;
import com.complextalents.skill.network.SkillCastPacket;
import com.complextalents.skill.network.SkillChannelStartPacket;
import net.minecraft.client.KeyMapping;
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
 * Unified client-side input handler for combat mode and skill casting.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Combat mode toggle handling</li>
 *   <li>Intercept hotbar keys 1-4 when combat mode is active</li>
 *   <li>Delegate channel tracking to SkillCastingClient</li>
 *   <li>Send SkillCastPacket to server</li>
 *   <li>Prevent hotbar scrolling in combat mode</li>
 * </ul>
 *
 * <p><b>Combat Mode Behavior:</b></p>
 * <ul>
 *   <li>Toggle key switches combat mode on/off</li>
 *   <li>Keys 1-4 activate skills instead of switching hotbar</li>
 *   <li>For channeling skills: hold to charge, release to cast</li>
 *   <li>Right-click cancels channeling</li>
 *   <li>Scroll wheel is disabled for hotbar switching</li>
 *   <li>Item use (right-click) is NOT blocked</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class ClientInputHandler {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Handle key input events - consume keys to prevent vanilla behavior.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (MC.screen != null || MC.player == null) {
            return;
        }

        // Combat mode toggle
        if (KeyBindings.TOGGLE_COMBAT_MODE.consumeClick()) {
            // Cancel channeling if toggling combat mode
            if (SkillCastingClient.isChanneling()) {
                SkillCastingClient.cancelChanneling();
                MC.player.displayClientMessage(Component.literal("§7Channeling canceled"), true);
            }
            // Also clear any pending channel start
            if (SkillCastingClient.hasPendingChannelStart()) {
                SkillCastingClient.clearPendingChannelStart();
            }
            CombatModeClient.toggle();
            PacketHandler.sendToServer(new ToggleCombatModePacket());
            return;
        }

        // Skill casting - consume hotbar keys 1-4 in combat mode
        if (CombatModeClient.isCombatMode()) {
            KeyMapping[] hotbarKeys = MC.options.keyHotbarSlots;
            for (int slotIndex = 0; slotIndex < Math.min(4, hotbarKeys.length); slotIndex++) {
                KeyMapping key = hotbarKeys[slotIndex];
                key.setDown(false);
                while (key.consumeClick());

                if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == getKeyCode(hotbarKeys[slotIndex])) {
                    handleSkillKeyPress(slotIndex);
                    return;
                }
                // Handle key release for finishing channeling
                if (event.getAction() == GLFW.GLFW_RELEASE && event.getKey() == getKeyCode(hotbarKeys[slotIndex])) {
                    handleSkillKeyRelease(slotIndex);
                    return;
                }
            }
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
    private static int getKeyCode(KeyMapping keyMapping) {
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
    private static void handleSkillKeyRelease(int slotIndex) {
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
            event.register(KeyBindings.TOGGLE_COMBAT_MODE);
            TalentsMod.LOGGER.info("Registered combat mode key mappings");
        }
    }
}
