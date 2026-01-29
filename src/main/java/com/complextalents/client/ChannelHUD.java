package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.skill.Skill;
import com.complextalents.skill.client.SkillCastingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders the channeling progress bar when a player is channeling a skill.
 *
 * <p>FPS Optimization: Early exit when not channeling minimizes performance impact.</p>
 *
 * <p>The HUD renders once per frame during the Post event phase,
 * after vanilla overlays have been drawn.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class ChannelHUD {

    private static final int BAR_WIDTH = 91;  // Same width as hotbar slot
    private static final int BAR_HEIGHT = 5;
    private static final int HOTBAR_OFFSET_Y = 42;  // Position above hotbar

    /**
     * Render the channel progress bar overlay.
     *
     * <p>Called once per frame after vanilla overlays are rendered.</p>
     *
     * @param event The render overlay event
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Early exit if not channeling - minimal performance impact
        if (!SkillCastingClient.isChanneling()) {
            return;
        }

        Skill skill = SkillCastingClient.getCurrentChannelingSkill();
        if (skill == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Don't render if a screen is open
        if (minecraft.screen != null) {
            return;
        }

        double progress = SkillCastingClient.getChannelProgress();
        double maxChannelTime = skill.getMaxChannelTime();

        GuiGraphics graphics = event.getGuiGraphics();

        renderChannelBar(graphics, progress, maxChannelTime);
    }

    /**
     * Render the channel progress bar.
     *
     * @param graphics The GUI graphics context
     * @param progress The channel progress (0.0 to 1.0)
     * @param maxChannelTime The maximum channel time in seconds
     */
    private static void renderChannelBar(GuiGraphics graphics, double progress, double maxChannelTime) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position: center horizontally, just above the hotbar
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - HOTBAR_OFFSET_Y - BAR_HEIGHT - 2;

        // Draw background (dark gray with transparency)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x88000000);

        // Draw progress bar (color based on progress: yellow -> orange -> red)
        int color = getProgressColor(progress);
        int filledWidth = (int) (BAR_WIDTH * progress);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, color);
        }

        // Draw border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0xFFFFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF); // Bottom
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, 0xFFFFFFFF); // Left
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF); // Right
    }

    /**
     * Get the color for the progress bar based on progress.
     * Yellow (start) -> Orange (middle) -> Red (full)
     *
     * @param progress The channel progress (0.0 to 1.0)
     * @return ARGB color integer
     */
    private static int getProgressColor(double progress) {
        int r, g, b;

        if (progress < 0.5) {
            // Yellow to Orange
            double t = progress * 2; // 0 to 1
            r = 255;
            g = (int) (255 - t * 100); // 255 -> 155
            b = 0;
        } else {
            // Orange to Red
            double t = (progress - 0.5) * 2; // 0 to 1
            r = 255;
            g = (int) (155 - t * 155); // 155 -> 0
            b = 0;
        }

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
