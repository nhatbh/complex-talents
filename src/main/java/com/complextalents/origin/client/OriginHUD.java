package com.complextalents.origin.client;

import com.complextalents.TalentsMod;
import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginRegistry;
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
 * Renders the origin resource bar above the hotbar.
 * Uses RegisterGuiOverlaysEvent for optimal performance - called exactly once per frame
 * instead of being called for every overlay and filtering.
 *
 * <p>Performance optimizations:
 * <ul>
 * <li>Uses RegisterGuiOverlaysEvent - Forge calls directly once per frame</li>
 * <li>Caches text strings - only updates when values change</li>
 * <li>Avoids String.format - uses direct casting</li>
 * <li>Avoids Component allocation - draws strings directly</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OriginHUD {

    private static final int BAR_WIDTH = 91;   // Same width as hotbar slot
    private static final int BAR_HEIGHT = 5;
    private static final int HOTBAR_OFFSET_Y = 52;  // Position above hotbar (above channel bar)
    private static final int TEXT_OFFSET_Y = 62;    // Position of resource name text

    // Cache for default resource text - only rebuild when values change
    private static String cachedResourceText = "";
    private static String cachedResourceName = "";
    private static double lastResourceValue = -1;
    private static double lastResourceMax = -1;
    private static int cachedResourceTextWidth = 0;

    /**
     * Register the origin HUD overlay with Forge.
     * Called once during mod initialization on the client.
     *
     * @param event The register overlays event
     */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "origin_bar",
                OriginHUD::render
        );
    }

    /**
     * Render the origin HUD overlay.
     * Called exactly once per frame by Forge, after the hotbar is rendered.
     *
     * @param gui The Forge GUI instance
     * @param graphics The GUI graphics context
     * @param partialTick Partial tick time
     * @param width Screen width
     * @param height Screen height
     */
    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        // Early exit if no origin is active - minimal performance impact
        if (!ClientOriginData.hasOrigin()) {
            return;
        }

        // Don't render if a screen is open
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        // Get the player's origin
        Origin origin = OriginRegistry.getInstance().getOrigin(ClientOriginData.getOriginId());
        if (origin == null) {
            return;
        }

        // Use custom renderer if provided, otherwise use default
        OriginRenderer renderer = origin.getRenderer();
        if (renderer != null) {
            renderer.renderHUD(graphics, width, height);
        } else {
            renderDefaultResourceBar(graphics, width, height);
        }
    }

    /**
     * Render the default origin resource bar.
     * Used as fallback for origins without custom renderers.
     *
     * @param graphics The GUI graphics context
     * @param screenWidth The screen width
     * @param screenHeight The screen height
     */
    private static void renderDefaultResourceBar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        double resourceValue = ClientOriginData.getResourceValue();
        double resourceMax = ClientOriginData.getResourceMax();
        int resourceColor = getResourceColor();

        // Position: center horizontally, above the hotbar
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - HOTBAR_OFFSET_Y - BAR_HEIGHT - 2;

        // Calculate fill width
        double fillRatio = resourceMax > 0 ? resourceValue / resourceMax : 0;
        int filledWidth = (int) (BAR_WIDTH * Math.min(1.0, Math.max(0.0, fillRatio)));

        // Enable blending for transparency
        RenderSystem.enableBlend();

        // Draw background (dark gray with 60% opacity)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99000000);

        // Draw filled portion (resource color with 60% opacity)
        int fillColor = (0x99 << 24) | (resourceColor & 0x00FFFFFF);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, fillColor);
        }

        // Draw border (60% opacity)
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0x99FFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99FFFFFF); // Bottom
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, 0x99FFFFFF); // Left
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x99FFFFFF); // Right

        RenderSystem.disableBlend();

        // Draw resource name and value
        renderResourceText(graphics, screenWidth, resourceValue, resourceMax);
    }

    /**
     * Render the resource name and value text.
     * Uses cached text - only rebuilds string when values actually change.
     *
     * @param graphics The GUI graphics context
     * @param screenWidth The screen width
     * @param value The current resource value
     * @param max The maximum resource value
     */
    private static void renderResourceText(GuiGraphics graphics, int screenWidth, double value, double max) {
        com.complextalents.origin.ResourceType resourceType = ClientOriginData.getResourceType();
        if (resourceType == null) {
            return;
        }

        String resourceName = resourceType.getName();

        // Only rebuild text string when values or name change
        if (value != lastResourceValue || max != lastResourceMax || !resourceName.equals(cachedResourceName)) {
            lastResourceValue = value;
            lastResourceMax = max;
            cachedResourceName = resourceName;

            // Use direct casting instead of String.format - 10-20x faster
            cachedResourceText = resourceName + ": " + (int)value + "/" + (int)max;

            Minecraft minecraft = Minecraft.getInstance();
            cachedResourceTextWidth = minecraft.font.width(cachedResourceText);
        }

        int textY = graphics.guiHeight() - TEXT_OFFSET_Y;
        int scaledWidth = (int) (screenWidth / 0.75f);

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);

        // Draw cached string directly - no Component allocation
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(
                minecraft.font,
                cachedResourceText,
                (scaledWidth - cachedResourceTextWidth) / 2,
                (int) (textY / 0.75f),
                0x99FFFFFF
        );

        graphics.pose().popPose();
    }

    /**
     * Get the color for the resource bar.
     *
     * @return ARGB color integer
     */
    private static int getResourceColor() {
        com.complextalents.origin.ResourceType resourceType = ClientOriginData.getResourceType();
        if (resourceType != null) {
            return resourceType.getColor();
        }
        return 0xFFFFD700; // Default gold color
    }
}
