package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class KeyBindings {
    private static final List<KeyMapping> KEY_BINDINGS = new ArrayList<>();

    // Key binding constants
    public static KeyMapping ACTIVATE_TALENT_1;
    public static KeyMapping ACTIVATE_TALENT_2;
    public static KeyMapping ACTIVATE_TALENT_3;
    public static KeyMapping ACTIVATE_TALENT_4;
    public static KeyMapping ACTIVATE_TALENT_5;
    public static KeyMapping OPEN_TALENT_SCREEN;

    public static void register() {
        if (!com.complextalents.config.TalentClientConfig.enableKeyBindings.get()) {
            TalentsMod.LOGGER.info("Key bindings disabled in config");
            return;
        }

        // Create key bindings
        ACTIVATE_TALENT_1 = new KeyMapping(
                "key.complextalents.activate_talent_1",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.categories.complextalents"
        );
        ACTIVATE_TALENT_2 = new KeyMapping(
                "key.complextalents.activate_talent_2",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "key.categories.complextalents"
        );
        ACTIVATE_TALENT_3 = new KeyMapping(
                "key.complextalents.activate_talent_3",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories.complextalents"
        );
        ACTIVATE_TALENT_4 = new KeyMapping(
                "key.complextalents.activate_talent_4",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.complextalents"
        );
        ACTIVATE_TALENT_5 = new KeyMapping(
                "key.complextalents.activate_talent_5",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.complextalents"
        );
        OPEN_TALENT_SCREEN = new KeyMapping(
                "key.complextalents.open_talent_screen",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories.complextalents"
        );

        KEY_BINDINGS.add(ACTIVATE_TALENT_1);
        KEY_BINDINGS.add(ACTIVATE_TALENT_2);
        KEY_BINDINGS.add(ACTIVATE_TALENT_3);
        KEY_BINDINGS.add(ACTIVATE_TALENT_4);
        KEY_BINDINGS.add(ACTIVATE_TALENT_5);
        KEY_BINDINGS.add(OPEN_TALENT_SCREEN);

        TalentsMod.LOGGER.info("Key bindings registered");
    }

    public static void onKeyPressed(KeyMapping keyBinding) {
        Minecraft minecraft = Minecraft.getInstance();

        if (keyBinding == OPEN_TALENT_SCREEN && minecraft.screen == null) {
            minecraft.setScreen(new TalentScreen(minecraft.player));
            return;
        }

        // Handle talent activation
        int talentSlot = 0;
        if (keyBinding == ACTIVATE_TALENT_1) {
            talentSlot = 1;
        } else if (keyBinding == ACTIVATE_TALENT_2) {
            talentSlot = 2;
        } else if (keyBinding == ACTIVATE_TALENT_3) {
            talentSlot = 3;
        } else if (keyBinding == ACTIVATE_TALENT_4) {
            talentSlot = 4;
        } else if (keyBinding == ACTIVATE_TALENT_5) {
            talentSlot = 5;
        }

        if (talentSlot > 0) {
            activateTalent(talentSlot);
        }
    }

    private static void activateTalent(int slot) {
        // Send packet to server to activate talent
        // Implementation would send TalentActivationPacket with the appropriate talent ID
        TalentsMod.LOGGER.debug("Activating talent slot {}", slot);
    }

    public static List<KeyMapping> getKeyBindings() {
        return KEY_BINDINGS;
    }
}
