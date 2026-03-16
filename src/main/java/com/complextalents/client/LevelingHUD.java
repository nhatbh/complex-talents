package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.client.ClientLevelingData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD overlay for Leveling system.
 * Renders Level, XP Bar, and Chunk Fatigue.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LevelingHUD {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 4;
    private static final int OFFSET_Y = 7; // Right of hotbar
    private static final int OFFSET_X = 4; // Distance from hotbar edge

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "leveling_hud",
                LevelingHUD::render);
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (Minecraft.getInstance().options.hideGui || Minecraft.getInstance().screen != null) {
            return;
        }

        double fatigue = ClientLevelingData.getChunkFatigue();
        // Position to the right of the hotbar (hotbar is 182 wide, centered)
        int hotbarStartX = (width - 182) / 2;
        int x = hotbarStartX + 182 + OFFSET_X;
        int y = height - OFFSET_Y;

        renderXPBar(graphics, x, y, fatigue);
        renderStats(graphics, x, y);
    }

    private static void renderXPBar(GuiGraphics graphics, int x, int y, double fatigue) {
        double currentXP = ClientLevelingData.getCurrentXP();
        double xpForNext = ClientLevelingData.getXpForNext();
        double progress = Math.min(1.0, currentXP / xpForNext);

        RenderSystem.enableBlend();

        // 1. Background (Glassmorphism style)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x66000000);

        // 2. Progress (Dynamically colored based on fatigue)
        int filledWidth = (int) (BAR_WIDTH * progress);
        if (filledWidth > 0) {
            int color = getDynamicXPColor(fatigue);
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, color);
        }

        // 4. Subtle Glow/Border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0x44FFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0x44FFFFFF); // Bottom

        // 5. XP Text (Scaled down)
        String xpText = (int) currentXP + " / " + (int) xpForNext;
        int textWidth = Minecraft.getInstance().font.width(xpText);
        float scale = 0.5f;

        graphics.pose().pushPose();
        // Move to the center of the bar
        graphics.pose().translate(x + (BAR_WIDTH / 2.0f), y + (BAR_HEIGHT / 2.0f), 0);
        graphics.pose().scale(scale, scale, 1.0f);
        // Draw centered (offset by half width/height of the scaled text)
        graphics.drawString(Minecraft.getInstance().font, xpText, (int)(-textWidth / 2.0f), -4, 0xFFFFFFFF, true);
        graphics.pose().popPose();

        RenderSystem.disableBlend();
    }

    private static void renderStats(GuiGraphics graphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        int pLevel = ClientLevelingData.getLevel();
        String levelText = "Lvl " + pLevel;
        float scale = 0.7f;

        // Level Text (Left side of bar, slightly above)
        graphics.pose().pushPose();
        graphics.pose().translate(x, y - (10 * scale), 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(mc.font, levelText, 0, 0, 0xFFFFFFFF, true);

        // Skill Points Text (Right side of bar, slightly above)
        int availableSP = ClientLevelingData.getAvailableSkillPoints();
        if (availableSP > 0) {
            String spText = availableSP + " SP";
            int spWidth = mc.font.width(spText);
            // Translate to the right side of the bar (accounting for scale)
            graphics.pose().translate((BAR_WIDTH / scale) - spWidth, 0, 0);
            graphics.drawString(mc.font, spText, 0, 0, 0xFF55FF55, true); // Greenish color
        }
        graphics.pose().popPose();
    }

    private static int getDynamicXPColor(double fatigue) {
        // Vibrant Green (1.0) -> Yellow (0.6) -> Sickly Orange/Brown (0.0)
        int r, g, b;
        if (fatigue >= 0.5) {
            double t = (fatigue - 0.5) * 2.0; // 0 to 1
            r = (int) (255 - t * 221); // 255 -> 34
            g = 255;
            b = (int) (34 - t * 0); // 34
        } else {
            double t = fatigue * 2.0; // 0 to 1
            r = 255;
            g = (int) (136 + t * 119); // 136 -> 255
            b = (int) (34 - t * 0); // 34
        }
        return (0xAA << 24) | (r << 16) | (g << 8) | b;
    }
}
