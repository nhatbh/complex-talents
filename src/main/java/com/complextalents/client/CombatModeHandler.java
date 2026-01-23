package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class CombatModeHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Don't intercept if in a GUI
        if (minecraft.screen != null) {
            return;
        }

        // Don't intercept if player doesn't exist
        if (minecraft.player == null) {
            return;
        }

        // Check if Combat Mode is enabled
        minecraft.player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            if (!talents.isCombatModeEnabled()) {
                return; // Combat Mode disabled, don't intercept
            }

            // Check if this is a hotbar key press (1-4)
            int slot = -1;
            if (event.getKey() == GLFW.GLFW_KEY_1) {
                slot = 1;
            } else if (event.getKey() == GLFW.GLFW_KEY_2) {
                slot = 2;
            } else if (event.getKey() == GLFW.GLFW_KEY_3) {
                slot = 3;
            } else if (event.getKey() == GLFW.GLFW_KEY_4) {
                slot = 4;
            }

            // If it's a hotbar key (1-4) and action is PRESS or REPEAT
            if (slot > 0 && (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT)) {
                // Cancel the vanilla hotbar key behavior
                event.setCanceled(true);

                // Only activate on PRESS, not REPEAT
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    // Activate the corresponding talent
                    KeyBindings.onCombatModeHotbarKey(slot);
                }
            }
        });
    }
}
