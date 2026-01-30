package com.complextalents.origin.example;

import com.complextalents.origin.client.ClientOriginData;
import com.complextalents.origin.client.OriginRenderer;
import com.complextalents.passive.client.ClientPassiveStackData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Custom HUD renderer for the Cleric origin.
 * Displays the Piety resource bar and Grace passive stack indicators.
 */
public class ClericRenderer implements OriginRenderer {

    private static final int BAR_WIDTH = 91;
    private static final int BAR_HEIGHT = 5;
    private static final int RESOURCE_OFFSET_Y = 52;
    private static final int TEXT_OFFSET_Y = 62;
    private static final int GRACE_OFFSET_Y = 75;

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        renderPietyBar(graphics, screenWidth, screenHeight);
        renderGraceStacks(graphics, screenWidth, screenHeight);
    }

    /**
     * Render the Piety resource bar.
     */
    private void renderPietyBar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        double piety = ClientOriginData.getResourceValue();
        double max = ClientOriginData.getResourceMax();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - RESOURCE_OFFSET_Y - BAR_HEIGHT - 2;

        // Calculate fill width
        double fillRatio = max > 0 ? piety / max : 0;
        int filledWidth = (int) (BAR_WIDTH * Math.min(1.0, Math.max(0.0, fillRatio)));

        // Draw background
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x88000000);

        // Draw filled portion (gold color for Piety)
        int fillColor = 0xFFFFD700;
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, fillColor);
        }

        // Draw border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF);

        // Draw resource text
        renderPietyText(graphics, screenWidth, piety, max);
    }

    /**
     * Render the Piety resource text.
     */
    private void renderPietyText(GuiGraphics graphics, int screenWidth, double piety, double max) {
        String text = String.format("Piety: %.0f/%.0f", piety, max);

        int textY = graphics.guiHeight() - TEXT_OFFSET_Y;
        Minecraft minecraft = Minecraft.getInstance();

        Component textComponent = Component.literal(text);
        int textWidth = minecraft.font.width(textComponent);
        int scaledWidth = (int) (screenWidth / 0.75f);

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);

        graphics.drawString(
                minecraft.font,
                textComponent,
                (scaledWidth - textWidth) / 2,
                (int) (textY / 0.75f),
                0xFFFFFF
        );

        graphics.pose().popPose();
    }

    /**
     * Render Grace stacks as dots/icons.
     * Filled dots = current stacks, empty dots = remaining capacity.
     */
    private void renderGraceStacks(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int grace = ClientPassiveStackData.getStackCount("grace");
        int maxGrace = 10;

        int dotSize = 6;
        int spacing = 8;
        int startX = (screenWidth - (maxGrace * spacing)) / 2;
        int y = screenHeight - GRACE_OFFSET_Y;

        // Color for filled Grace stacks (light blue)
        int filledColor = 0xFFE6F0FF;
        // Color for empty stacks (dark, transparent)
        int emptyColor = 0x44000000;

        for (int i = 0; i < maxGrace; i++) {
            int x = startX + (i * spacing);
            int color = i < grace ? filledColor : emptyColor;
            graphics.fill(x, y, x + dotSize, y + dotSize, color);
        }

        // Draw "Grace" label
        Minecraft minecraft = Minecraft.getInstance();
        String label = "Grace";
        int labelWidth = minecraft.font.width(label);
        int labelY = y - 10;

        graphics.drawString(
                minecraft.font,
                Component.literal(label),
                (screenWidth - labelWidth) / 2,
                labelY,
                0xAACCCCCC  // Light gray, slightly transparent
        );
    }
}
