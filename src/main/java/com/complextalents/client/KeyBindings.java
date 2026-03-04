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
    public static KeyMapping SKILL_1;

    public static void register() {
        // Create key bindings
        SKILL_1 = new KeyMapping(
                "key.complextalents.skill_1",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.categories.complextalents"
        );

        TalentsMod.LOGGER.info("Key bindings initialized");
    }
}