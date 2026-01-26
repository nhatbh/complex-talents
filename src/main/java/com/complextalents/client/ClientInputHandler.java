package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.ToggleCombatModePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles all client-side input for the combat mode system.
 * This includes key interception and mouse scroll prevention.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class ClientInputHandler {

    /**
     * Handle key input events to intercept hotbar keys in combat mode
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Don't process if in GUI or no player
        if (mc.screen != null || mc.player == null) {
            return;
        }

        // Only process key press events (not release)
        if (event.getAction() != 1) { // GLFW_PRESS = 1
            return;
        }

        // Check if combat mode is active
        if (!CombatModeClient.isCombatMode()) {
            return;
        }

        // Get hotbar key mappings
        KeyMapping[] hotbarKeys = mc.options.keyHotbarSlots;

        // Check if this key matches any hotbar slot 1-4
        for (int i = 0; i < Math.min(4, hotbarKeys.length); i++) {
            if (hotbarKeys[i].matches(event.getKey(), event.getScanCode())) {
                // Cancel the vanilla hotbar switch
                event.setCanceled(true);

                // Combat mode functionality removed with talent system
                TalentsMod.LOGGER.debug("Intercepted hotbar key {} in combat mode", i + 1);
                return; // Only handle one key at a time
            }
        }
    }

    /**
     * Handle mouse scroll events to prevent hotbar scrolling in combat mode
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Don't process if in GUI
        if (mc.screen != null) {
            return;
        }

        // Check if combat mode is active
        if (CombatModeClient.isCombatMode()) {
            // Cancel scroll wheel changing hotbar selection
            event.setCanceled(true);

            // Optionally: use scroll for something else in combat mode
            // For example, you could cycle through talent pages or zoom
            double scrollDelta = event.getScrollDelta();
            if (scrollDelta != 0) {
                TalentsMod.LOGGER.debug("Scroll prevented in combat mode (delta: {})", scrollDelta);
            }
        }
    }

    /**
     * Handle client tick events to check for toggle key
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Don't process if in GUI or no player
        if (mc.screen != null || mc.player == null) {
            return;
        }

        // Check for combat mode toggle
        while (KeyBindings.TOGGLE_COMBAT_MODE.consumeClick()) {
            // Toggle local state
            CombatModeClient.toggle();

            // Send to server for synchronization
            PacketHandler.sendToServer(new ToggleCombatModePacket());
        }

        // Talent screen removed with talent system
    }

    @Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            // Initialize key bindings
            KeyBindings.register();

            // Only register the toggle key
            event.register(KeyBindings.TOGGLE_COMBAT_MODE);

            TalentsMod.LOGGER.info("Registered combat mode key mappings");
        }
    }
}