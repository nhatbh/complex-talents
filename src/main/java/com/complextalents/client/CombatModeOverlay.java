package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.talent.ActiveTalent;
import com.complextalents.talent.HybridTalent;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import com.complextalents.talent.TalentSlotType;
import com.complextalents.talent.TalentType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class CombatModeOverlay {

    private static final int SLOT_SIZE = 48;
    private static final int SLOT_SPACING = 4;
    private static final int PADDING = 10;
    private static final int KEYBIND_OFFSET = 4;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            // Only show HUD in Combat Mode
            if (!talents.isCombatModeEnabled()) {
                return;
            }

            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();

            // Position: bottom right corner
            int startX = screenWidth - PADDING - SLOT_SIZE;
            int startY = screenHeight - PADDING - (SLOT_SIZE * 5 + SLOT_SPACING * 4);

            // Render Combat Mode indicator
            renderCombatModeIndicator(graphics, screenWidth, screenHeight);

            // Define the slot order (top to bottom)
            TalentSlotType[] slots = {
                TalentSlotType.HARMONY,
                TalentSlotType.CRESCENDO,
                TalentSlotType.RESONANCE,
                TalentSlotType.FINALE,
                TalentSlotType.DEFINITION
            };

            String[] keybinds = {"1", "2", "3", "4", "Z"};

            for (int i = 0; i < slots.length; i++) {
                int y = startY + (SLOT_SIZE + SLOT_SPACING) * i;
                renderTalentSlot(graphics, minecraft, talents, slots[i], keybinds[i], startX, y);
            }
        });
    }

    private static void renderCombatModeIndicator(GuiGraphics graphics, int screenWidth, int screenHeight) {
        // Render "COMBAT MODE" text in top right
        Component text = Component.literal("§6§lCOMBAT MODE");
        int textWidth = Minecraft.getInstance().font.width(text);
        int x = screenWidth - PADDING - textWidth;
        int y = PADDING;

        // Background
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, 0x88000000);

        // Pulsing border effect
        int pulseColor = getPulseColor();
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y - 1, pulseColor);
        graphics.fill(x - 4, y + 10, x + textWidth + 4, y + 11, pulseColor);
        graphics.fill(x - 4, y - 2, x - 3, y + 11, pulseColor);
        graphics.fill(x + textWidth + 3, y - 2, x + textWidth + 4, y + 11, pulseColor);

        // Text
        graphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFFFFFF);
    }

    private static void renderTalentSlot(GuiGraphics graphics, Minecraft minecraft,
                                         com.complextalents.capability.PlayerTalents talents,
                                         TalentSlotType slotType, String keybind,
                                         int x, int y) {
        ResourceLocation talentId = talents.getTalentInSlot(slotType);
        Talent talent = talentId != null ? TalentRegistry.getTalent(talentId) : null;

        boolean hasActiveTalent = talent != null &&
            (talent.getType() == TalentType.ACTIVE || talent.getType() == TalentType.HYBRID);

        // Determine slot state
        boolean isOnCooldown = false;
        float cooldownPercent = 0.0f;
        int cooldownTicks = 0;

        if (talent != null && hasActiveTalent) {
            cooldownTicks = talents.getTalentCooldown(talentId);
            isOnCooldown = cooldownTicks > 0;

            if (isOnCooldown) {
                int maxCooldown = 0;
                if (talent instanceof ActiveTalent activeTalent) {
                    maxCooldown = activeTalent.getCooldownTicks();
                } else if (talent instanceof HybridTalent hybridTalent) {
                    maxCooldown = hybridTalent.getCooldownTicks();
                }
                cooldownPercent = maxCooldown > 0 ? (float) cooldownTicks / maxCooldown : 0.0f;
            }
        }

        // Background - darker if on cooldown or no talent
        int bgColor;
        if (talent == null) {
            bgColor = 0x99111111; // Very dark for empty slot
        } else if (isOnCooldown) {
            bgColor = 0x99333333; // Dark gray for cooldown
        } else if (hasActiveTalent) {
            bgColor = 0x99004400; // Dark green for ready active talent
        } else {
            bgColor = 0x99222222; // Gray for passive talent
        }

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

        // Border
        int borderColor;
        if (talent == null) {
            borderColor = 0xFF666666; // Gray for empty
        } else if (isOnCooldown) {
            borderColor = 0xFFFF4444; // Red for cooldown
        } else if (hasActiveTalent) {
            borderColor = 0xFF00FF00; // Bright green for ready
        } else {
            borderColor = 0xFF888888; // Light gray for passive
        }

        // Draw border
        graphics.fill(x, y, x + SLOT_SIZE, y + 2, borderColor);
        graphics.fill(x, y + SLOT_SIZE - 2, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        graphics.fill(x, y, x + 2, y + SLOT_SIZE, borderColor);
        graphics.fill(x + SLOT_SIZE - 2, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

        // Cooldown overlay (radial sweep effect)
        if (isOnCooldown && cooldownPercent > 0) {
            int overlayHeight = (int) (SLOT_SIZE * cooldownPercent);
            graphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + 2 + overlayHeight, 0xBB000000);
        }

        // Talent icon/text
        if (talent != null) {
            // Get first letter of talent name
            String talentName = talent.getName().getString();
            String initial = talentName.isEmpty() ? "?" : talentName.substring(0, 1).toUpperCase();

            // Draw large initial
            int textColor = hasActiveTalent && !isOnCooldown ? 0x00FF00 : 0xFFFFFF;
            graphics.pose().pushPose();
            graphics.pose().translate(x + SLOT_SIZE / 2.0f, y + SLOT_SIZE / 2.0f - 8, 0);
            graphics.pose().scale(2.0f, 2.0f, 1.0f);
            String displayText = initial;
            int textWidth = minecraft.font.width(displayText);
            graphics.drawString(minecraft.font, displayText, -textWidth / 2, 0, textColor, true);
            graphics.pose().popPose();
        }

        // Keybind indicator
        int keybindX = x + KEYBIND_OFFSET;
        int keybindY = y + KEYBIND_OFFSET;
        int keybindSize = 16;

        // Keybind background
        graphics.fill(keybindX, keybindY, keybindX + keybindSize, keybindY + keybindSize, 0xCC000000);

        // Keybind text
        int keybindTextX = keybindX + keybindSize / 2 - minecraft.font.width(keybind) / 2;
        int keybindTextY = keybindY + keybindSize / 2 - 4;
        graphics.drawString(minecraft.font, keybind, keybindTextX, keybindTextY, 0xFFFFFF);

        // Cooldown time text
        if (isOnCooldown) {
            int cooldownSeconds = (cooldownTicks + 19) / 20; // Round up
            String cooldownText = String.valueOf(cooldownSeconds);

            graphics.pose().pushPose();
            graphics.pose().translate(x + SLOT_SIZE / 2.0f, y + SLOT_SIZE - 12, 0);
            graphics.pose().scale(1.5f, 1.5f, 1.0f);
            int cdTextWidth = minecraft.font.width(cooldownText);
            graphics.drawString(minecraft.font, cooldownText, -cdTextWidth / 2, 0, 0xFFFFFF, true);
            graphics.pose().popPose();
        }

        // Slot name
        String slotName = slotType.getDisplayName();
        int nameY = y + SLOT_SIZE + 2;
        int nameX = x + SLOT_SIZE / 2 - minecraft.font.width(slotName) / 2;
        graphics.drawString(minecraft.font, slotName, nameX, nameY, 0xAAAAAA, true);
    }

    private static int getPulseColor() {
        // Create pulsing gold color effect
        long time = System.currentTimeMillis();
        float pulse = (float) Math.sin(time / 200.0) * 0.5f + 0.5f; // 0.0 to 1.0
        int brightness = (int) (128 + pulse * 127); // 128 to 255

        return 0xFF000000 | (brightness << 16) | (brightness << 8) | 0;
    }
}
