package com.complextalents.client;

import com.complextalents.TalentsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side combat mode state management.
 * This class tracks combat mode state locally to avoid network latency.
 */
@OnlyIn(Dist.CLIENT)
public class CombatModeClient {
    private static boolean combatMode = false;
    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN = 250; // 250ms cooldown to prevent spam

    /**
     * Check if combat mode is currently enabled
     */
    public static boolean isCombatMode() {
        return combatMode;
    }

    /**
     * Toggle combat mode on/off
     * @return The new combat mode state
     */
    public static boolean toggle() {
        long currentTime = System.currentTimeMillis();

        // Prevent toggle spam
        if (currentTime - lastToggleTime < TOGGLE_COOLDOWN) {
            return combatMode;
        }

        combatMode = !combatMode;
        lastToggleTime = currentTime;

        // Display feedback to player
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Component message = combatMode
                ? Component.literal("§6§lCombat Mode: §a§lON §7(Keys 1-4 activate talents)")
                : Component.literal("§6§lCombat Mode: §c§lOFF §7(Keys 1-4 select hotbar)");
            mc.player.displayClientMessage(message, true);

            TalentsMod.LOGGER.debug("Combat Mode toggled to: {}", combatMode);
        }

        return combatMode;
    }

    /**
     * Set combat mode state directly (used for synchronization)
     */
    public static void setCombatMode(boolean enabled) {
        combatMode = enabled;
    }

    /**
     * Reset combat mode (called on disconnect/world change)
     */
    public static void reset() {
        combatMode = false;
        lastToggleTime = 0;
    }
}