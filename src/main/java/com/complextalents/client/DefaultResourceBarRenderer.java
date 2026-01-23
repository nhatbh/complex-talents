package com.complextalents.client;

import com.complextalents.talent.ResourceBarConfig;
import com.complextalents.talent.ResourceBarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Default resource bar renderer - simple horizontal bar
 */
@OnlyIn(Dist.CLIENT)
public class DefaultResourceBarRenderer implements ResourceBarRenderer {
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       float currentValue, float maxValue, ResourceBarConfig config, Component displayName) {
        // Calculate fill percentage
        float percentage = maxValue > 0 ? currentValue / maxValue : 0.0f;
        int fillWidth = (int) (width * percentage);

        // Draw background (dark gray)
        graphics.fill(x, y, x + width, y + height, 0xFF000000);

        // Draw border (lighter gray)
        graphics.fill(x, y, x + width, y + 1, 0xFF555555); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF555555); // Bottom
        graphics.fill(x, y, x + 1, y + height, 0xFF555555); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF555555); // Right

        // Draw filled portion with resource color
        if (fillWidth > 0) {
            int color = 0xFF000000 | config.getColor();
            graphics.fill(x + 1, y + 1, x + fillWidth - 1, y + height - 1, color);

            // Add a lighter overlay for shine effect
            int shineColor = 0x44FFFFFF;
            int shineHeight = Math.max(1, height / 3);
            graphics.fill(x + 1, y + 1, x + fillWidth - 1, y + shineHeight, shineColor);
        }

        // Draw text if enabled
        if (shouldShowResourceName() || shouldShowNumericValue()) {
            String text = "";
            if (shouldShowResourceName() && shouldShowNumericValue()) {
                text = String.format("%s: %.0f/%.0f", displayName.getString(), currentValue, maxValue);
            } else if (shouldShowNumericValue()) {
                text = String.format("%.0f/%.0f", currentValue, maxValue);
            } else if (shouldShowResourceName()) {
                text = displayName.getString();
            }

            // Get font and calculate text position
            Font font = Minecraft.getInstance().font;
            int textWidth = font.width(text);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - 8) / 2;

            // Draw text shadow and text
            graphics.drawString(font, text, textX + 1, textY + 1, 0xFF000000, false);
            graphics.drawString(font, text, textX, textY, 0xFFFFFFFF, false);
        }
    }

    @Override
    public int getPreferredHeight() {
        return 10;
    }

    @Override
    public int getPreferredWidth() {
        return 100;
    }
}
