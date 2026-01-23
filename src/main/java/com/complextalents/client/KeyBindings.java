package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.TalentActivationPacket;
import com.complextalents.network.ToggleCombatModePacket;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import com.complextalents.talent.TalentSlotType;
import com.complextalents.talent.TalentType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
    public static KeyMapping TOGGLE_COMBAT_MODE;

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
        TOGGLE_COMBAT_MODE = new KeyMapping(
                "key.complextalents.toggle_combat_mode",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.complextalents"
        );

        KEY_BINDINGS.add(ACTIVATE_TALENT_1);
        KEY_BINDINGS.add(ACTIVATE_TALENT_2);
        KEY_BINDINGS.add(ACTIVATE_TALENT_3);
        KEY_BINDINGS.add(ACTIVATE_TALENT_4);
        KEY_BINDINGS.add(ACTIVATE_TALENT_5);
        KEY_BINDINGS.add(OPEN_TALENT_SCREEN);
        KEY_BINDINGS.add(TOGGLE_COMBAT_MODE);

        TalentsMod.LOGGER.info("Key bindings registered");
    }

    public static void onKeyPressed(KeyMapping keyBinding) {
        Minecraft minecraft = Minecraft.getInstance();

        if (keyBinding == OPEN_TALENT_SCREEN && minecraft.screen == null) {
            minecraft.setScreen(new TalentScreen(minecraft.player));
            return;
        }

        if (keyBinding == TOGGLE_COMBAT_MODE) {
            PacketHandler.sendToServer(new ToggleCombatModePacket());
            return;
        }

        // Handle talent activation (Z, X, C, V, B keys)
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        minecraft.player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            // Map slot number to TalentSlotType
            TalentSlotType slotType = switch (slot) {
                case 1 -> TalentSlotType.DEFINITION;
                case 2 -> TalentSlotType.HARMONY;
                case 3 -> TalentSlotType.CRESCENDO;
                case 4 -> TalentSlotType.RESONANCE;
                case 5 -> TalentSlotType.FINALE;
                default -> null;
            };

            if (slotType == null) return;

            ResourceLocation talentId = talents.getTalentInSlot(slotType);
            if (talentId == null) {
                TalentsMod.LOGGER.debug("No talent equipped in slot {}", slotType);
                return;
            }

            Talent talent = TalentRegistry.getTalent(talentId);
            if (talent == null) {
                TalentsMod.LOGGER.warn("Talent {} not found in registry", talentId);
                return;
            }

            // Only activate if it's an Active or Hybrid talent
            if (talent.getType() == TalentType.ACTIVE || talent.getType() == TalentType.HYBRID) {
                PacketHandler.sendToServer(new TalentActivationPacket(talentId));
                TalentsMod.LOGGER.debug("Activating talent {} from slot {}", talentId, slotType);
            } else {
                TalentsMod.LOGGER.debug("Talent {} in slot {} is passive, cannot activate", talentId, slotType);
            }
        });
    }

    /**
     * Called when hotbar keys 1-4 are pressed in Combat Mode
     */
    public static void onCombatModeHotbarKey(int slot) {
        if (slot < 1 || slot > 4) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        minecraft.player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            // Map hotbar slot to TalentSlotType: 1=Harmony, 2=Crescendo, 3=Resonance, 4=Finale
            TalentSlotType slotType = switch (slot) {
                case 1 -> TalentSlotType.HARMONY;
                case 2 -> TalentSlotType.CRESCENDO;
                case 3 -> TalentSlotType.RESONANCE;
                case 4 -> TalentSlotType.FINALE;
                default -> null;
            };

            if (slotType == null) return;

            ResourceLocation talentId = talents.getTalentInSlot(slotType);
            if (talentId == null) {
                TalentsMod.LOGGER.debug("No talent equipped in slot {}", slotType);
                return;
            }

            Talent talent = TalentRegistry.getTalent(talentId);
            if (talent == null) {
                TalentsMod.LOGGER.warn("Talent {} not found in registry", talentId);
                return;
            }

            // Only activate if it's an Active or Hybrid talent
            if (talent.getType() == TalentType.ACTIVE || talent.getType() == TalentType.HYBRID) {
                PacketHandler.sendToServer(new TalentActivationPacket(talentId));
                TalentsMod.LOGGER.debug("Combat Mode: Activating talent {} from slot {}", talentId, slotType);
            } else {
                TalentsMod.LOGGER.debug("Talent {} in slot {} is passive, ignoring key press", talentId, slotType);
            }
        });
    }

    public static List<KeyMapping> getKeyBindings() {
        return KEY_BINDINGS;
    }
}
