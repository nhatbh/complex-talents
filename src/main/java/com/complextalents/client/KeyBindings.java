package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Key bindings for the mod
 */
public class KeyBindings {
    // Key binding constants
    public static KeyMapping TOGGLE_COMBAT_MODE;

    public static void register() {
        // Create key bindings
        TOGGLE_COMBAT_MODE = new KeyMapping(
                "key.complextalents.toggle_combat_mode",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.complextalents"
        );

        TalentsMod.LOGGER.info("Key bindings initialized");
    }
}