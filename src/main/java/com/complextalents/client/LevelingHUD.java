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

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int OFFSET_Y = 32; // Above hotbar

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "leveling_hud",
                LevelingHUD::render
        );
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (Minecraft.getInstance().options.hideGui || Minecraft.getInstance().screen != null) {
            return;
        }

        int x = (width - BAR_WIDTH) / 2;
        int y = height - OFFSET_Y - BAR_HEIGHT;

        renderXPBar(graphics, x, y);
        renderStats(graphics, x, y, width);
    }

    private static void renderXPBar(GuiGraphics graphics, int x, int y) {
        double currentXP = ClientLevelingData.getCurrentXP();
        double xpForNext = ClientLevelingData.getXpForNext();
        double progress = Math.min(1.0, currentXP / xpForNext);

        RenderSystem.enableBlend();
        
        // Background (Glassmorphism style)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x66000000);
        
        // Progress (Vibrant Green)
        int filledWidth = (int) (BAR_WIDTH * progress);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, 0xAA22FF22);
        }

        // Subtile Glow/Border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0x44FFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0x44FFFFFF); // Bottom
        
        RenderSystem.disableBlend();
    }

    private static void renderStats(GuiGraphics graphics, int x, int y, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        int pLevel = ClientLevelingData.getLevel();
        double fatigue = ClientLevelingData.getChunkFatigue();
        
        String levelText = "Lvl " + pLevel;
        String fatigueText = "Fatigue: " + (int)(fatigue * 100) + "%";
        
        // Level Text (Left side of bar)
        graphics.drawString(mc.font, levelText, x, y - 10, 0xFFFFFFFF, true);
        
        // Fatigue Text (Right side of bar, colored based on value)
        int fatigueColor = getFatigueColor(fatigue);
        int fatigueWidth = mc.font.width(fatigueText);
        graphics.drawString(mc.font, fatigueText, x + BAR_WIDTH - fatigueWidth, y - 10, fatigueColor, true);
    }

    private static int getFatigueColor(double fatigue) {
        if (fatigue >= 0.8) return 0xFF22FF22; // Green
        if (fatigue >= 0.4) return 0xFFFFFF22; // Yellow
        return 0xFFFF2222; // Red
    }
}
