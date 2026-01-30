package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.skill.Skill;
import com.complextalents.skill.client.SkillCastingClient;
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
 * Renders the channeling progress bar when a player is channeling a skill.
 * Uses RegisterGuiOverlaysEvent for optimal performance - called exactly once per frame
 * instead of being called for every overlay and filtering.
 *
 * <p>Performance optimizations:
 * <ul>
 * <li>Uses RegisterGuiOverlaysEvent - Forge calls directly once per frame</li>
 * <li>Early exit when not channeling - minimal performance impact</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ChannelHUD {

    private static final int BAR_WIDTH = 91;  // Same width as hotbar slot
    private static final int BAR_HEIGHT = 5;
    private static final int HOTBAR_OFFSET_Y = 42;  // Position above hotbar

    /**
     * Register the channel HUD overlay with Forge.
     * Called once during mod initialization on the client.
     *
     * @param event The register overlays event
     */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "channel_bar",
                ChannelHUD::render
        );
    }

    /**
     * Render the channel progress bar overlay.
     * Called exactly once per frame by Forge, after the hotbar is rendered.
     *
     * @param gui The Forge GUI instance
     * @param graphics The GUI graphics context
     * @param partialTick Partial tick time
     * @param width Screen width
     * @param height Screen height
     */
    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        // Early exit if not channeling - minimal performance impact
        if (!SkillCastingClient.isChanneling()) {
            return;
        }

        Skill skill = SkillCastingClient.getCurrentChannelingSkill();
        if (skill == null) {
            return;
        }

        // Don't render if a screen is open
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        double progress = SkillCastingClient.getChannelProgress();
        double maxChannelTime = skill.getMaxChannelTime();

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

        // Enable blending for transparency
        RenderSystem.enableBlend();

        // Draw background (dark gray with 60% opacity)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99000000);

        // Draw progress bar (color based on progress: yellow -> orange -> red, 60% opacity)
        int color = getProgressColor(progress);
        int filledWidth = (int) (BAR_WIDTH * progress);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, color);
        }

        // Draw border (60% opacity)
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0x99FFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99FFFFFF); // Bottom
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, 0x99FFFFFF); // Left
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99FFFFFF); // Right

        RenderSystem.disableBlend();
    }

    /**
     * Get the color for the progress bar based on progress (60% opacity).
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

        return (0x99 << 24) | (r << 16) | (g << 8) | b;
    }
}
