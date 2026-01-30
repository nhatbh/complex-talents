package com.complextalents.origin.client;

import com.complextalents.TalentsMod;
import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders the origin resource bar above the hotbar.
 * Mirrors ChannelHUD pattern.
 *
 * <p>FPS Optimization: Early exit when no origin is active minimizes performance impact.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT)
public class OriginHUD {

    private static final int BAR_WIDTH = 91;   // Same width as hotbar slot
    private static final int BAR_HEIGHT = 5;
    private static final int HOTBAR_OFFSET_Y = 52;  // Position above hotbar (above channel bar)
    private static final int TEXT_OFFSET_Y = 62;    // Position of resource name text

    /**
     * Render the origin resource bar overlay.
     *
     * <p>Called once per frame after vanilla overlays are rendered.</p>
     *
     * @param event The render overlay event
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Early exit if no origin is active - minimal performance impact
        if (!ClientOriginData.hasOrigin()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Don't render if a screen is open
        if (minecraft.screen != null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Get the player's origin
        Origin origin = OriginRegistry.getInstance().getOrigin(ClientOriginData.getOriginId());
        if (origin == null) {
            return;
        }

        // Use custom renderer if provided, otherwise use default
        OriginRenderer renderer = origin.getRenderer();
        if (renderer != null) {
            renderer.renderHUD(graphics, screenWidth, screenHeight);
        } else {
            renderDefaultResourceBar(graphics, screenWidth, screenHeight);
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

        // Draw background (dark gray with transparency)
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x88000000);

        // Draw filled portion (resource color with alpha)
        int fillColor = (0xFF << 24) | (resourceColor & 0x00FFFFFF);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, fillColor);
        }

        // Draw border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0xFFFFFFFF); // Top
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF); // Bottom
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, 0xFFFFFFFF); // Left
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF); // Right

        // Draw resource name and value
        renderResourceText(graphics, screenWidth, resourceValue, resourceMax);
    }

    /**
     * Render the resource name and value text.
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
        String valueText = String.format("%.0f/%.0f", value, max);
        String fullText = resourceName + ": " + valueText;

        int textY = graphics.guiHeight() - TEXT_OFFSET_Y;
        int textColor = 0xFFFFFF;

        // Get Minecraft instance for font
        Minecraft minecraft = Minecraft.getInstance();

        // Render centered text
        Component textComponent = Component.literal(fullText);
        int textWidth = minecraft.font.width(textComponent);
        int scaledWidth = (int) (screenWidth / 0.75f);

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);

        // Use draw method instead of renderComponent for simple text
        graphics.drawString(
                minecraft.font,
                textComponent,
                (scaledWidth - textWidth) / 2,
                (int) (textY / 0.75f),
                textColor
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
