package com.complextalents.impl.darkmage.client;

import com.complextalents.origin.client.OriginRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Custom HUD renderer for the Dark Mage origin.
 * Displays soul count, Blood Pact status, and Phylactery cooldown.
 * <p>
 * Layout:
 * - Soul counter text on the right side of screen center (always visible)
 * - Phylactery cooldown arc on the right side (only visible when on cooldown)
 * - "BLOOD PACT" indicator when active (pulsing red)
 * </p>
 */
public class DarkMageRenderer implements OriginRenderer {

    // Phylactery cooldown arc configuration
    private static final float ARC_INNER_RADIUS = 25f;
    private static final float ARC_OUTER_RADIUS = 28f;
    private static final float ARC_LENGTH = 120f; // degrees
    private static final int ARC_SEGMENTS = 40;

    // Arc position (right side) - spans from 300° to 420° (wraps to 60°)
    private static final float ARC_BOTTOM_ANGLE = 300f;

    // Color definitions (ARGB) - all at 60% opacity (0x99)
    private static final int PHYLACTERY_BG_COLOR = 0x99000000;
    private static final int PHYLACTERY_FILL_COLOR = 0x9900FFAA; // Cyan/teal for death-defy
    private static final int PHYLACTERY_BORDER_COLOR = 0x99FFFFFF;

    // Cache for soul text - only rebuild when value changes
    private static String cachedSoulText = "";
    private static int lastSoulValue = -1;

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Only render Phylactery cooldown arc if on cooldown
        if (ClientSoulData.isPhylacteryOnCooldown()) {
            RenderSystem.enableBlend();
            renderPhylacteryCooldownArc(graphics, centerX, centerY);
            RenderSystem.disableBlend();
        }

        // Always render labels (soul count text + Blood Pact indicator)
        renderLabels(graphics, centerX, centerY);
    }

    /**
     * Render the Phylactery cooldown as a ring arc on the RIGHT side.
     * Only visible when Phylactery (death-defy passive) is on cooldown.
     * Arc drains as cooldown expires (full = just triggered, empty = ready).
     */
    private void renderPhylacteryCooldownArc(GuiGraphics graphics, int centerX, int centerY) {
        float cooldownRatio = ClientSoulData.getPhylacteryCooldownRatio();

        // Draw background arc (full empty bar)
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, PHYLACTERY_BG_COLOR);

        // Draw filled portion (represents remaining cooldown)
        if (cooldownRatio > 0) {
            float fillAngleLength = ARC_LENGTH * cooldownRatio;
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + fillAngleLength,
                    (int) (ARC_SEGMENTS * cooldownRatio) + 1, PHYLACTERY_FILL_COLOR);
        }

        // Draw arc outline
        drawArcOutline(graphics, centerX, centerY, ARC_OUTER_RADIUS,
                ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, PHYLACTERY_BORDER_COLOR);
    }

    /**
     * Render labels - soul count, Phylactery cooldown timer, and Blood Pact indicator.
     */
    private void renderLabels(GuiGraphics graphics, int centerX, int centerY) {
        Minecraft minecraft = Minecraft.getInstance();
        int souls = ClientSoulData.getSouls();
        boolean bloodPactActive = ClientSoulData.isBloodPactActive();
        boolean phylacteryOnCooldown = ClientSoulData.isPhylacteryOnCooldown();

        // Soul count (right side)
        if (souls != lastSoulValue) {
            lastSoulValue = souls;
            cachedSoulText = formatSoulCount(souls);
        }

        int soulTextX = (int) (centerX + ARC_OUTER_RADIUS + 6);
        int soulTextY = centerY - 3;
        int soulColor = bloodPactActive ? 0x99FF6666 : 0x99CC99FF; // Light red when active, light purple otherwise

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedSoulText,
                (int) (soulTextX / 0.7f), (int) (soulTextY / 0.7f), soulColor);
        graphics.pose().popPose();

        // Phylactery cooldown timer (below soul count, only when on cooldown)
        if (phylacteryOnCooldown) {
            float cooldownSeconds = ClientSoulData.getPhylacteryCooldownSeconds();
            String cooldownText = formatCooldown(cooldownSeconds);
            int cooldownTextX = (int) (centerX + ARC_OUTER_RADIUS + 6);
            int cooldownTextY = centerY + 5;

            graphics.pose().pushPose();
            graphics.pose().scale(0.6f, 0.6f, 0.6f);
            graphics.drawString(minecraft.font, cooldownText,
                    (int) (cooldownTextX / 0.6f), (int) (cooldownTextY / 0.6f), 0x9900FFAA); // Cyan color
            graphics.pose().popPose();
        }

        // Blood Pact indicator (left side, only when active)
        if (bloodPactActive) {
            String pactText = "BLOOD PACT";
            int pactTextWidth = minecraft.font.width(pactText);
            int pactTextX = (int) (centerX - ARC_OUTER_RADIUS - 6 - pactTextWidth * 0.7f);
            int pactTextY = centerY - 3;

            // Pulsing effect based on game time
            long time = minecraft.level != null ? minecraft.level.getGameTime() : 0;
            float pulse = (float) (0.7 + 0.3 * Math.sin(time * 0.2)); // Pulse between 0.7 and 1.0
            int alpha = (int) (pulse * 255);
            int pactColor = (alpha << 24) | 0xFF0000;

            graphics.pose().pushPose();
            graphics.pose().scale(0.7f, 0.7f, 0.7f);
            graphics.drawString(minecraft.font, pactText,
                    (int) (pactTextX / 0.7f), (int) (pactTextY / 0.7f), pactColor);
            graphics.pose().popPose();
        }
    }

    /**
     * Format cooldown time for display (e.g., "4:32" for 4 minutes 32 seconds).
     */
    private String formatCooldown(float seconds) {
        int totalSeconds = (int) seconds;
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    /**
     * Format soul count for display.
     * Uses K/M suffixes for large numbers.
     */
    private String formatSoulCount(int souls) {
        if (souls < 1000) {
            return souls + " Souls";
        } else if (souls < 1000000) {
            return String.format("%.1fK Souls", souls / 1000.0);
        } else {
            return String.format("%.1fM Souls", souls / 1000000.0);
        }
    }

    /**
     * Draw a filled ring arc segment using BufferBuilder.
     */
    private void drawArcSegment(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                                float startAngle, float endAngle, int segments, int color) {

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        var buf = tesselator.getBuilder();

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = startAngle + angleStep * i;
            float a2 = startAngle + angleStep * (i + 1);

            double rad1 = Math.toRadians(a1);
            double rad2 = Math.toRadians(a2);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            float outer1x = cx + cos1 * outerRadius;
            float outer1y = cy + sin1 * outerRadius;
            float outer2x = cx + cos2 * outerRadius;
            float outer2y = cy + sin2 * outerRadius;
            float inner1x = cx + cos1 * innerRadius;
            float inner1y = cy + sin1 * innerRadius;
            float inner2x = cx + cos2 * innerRadius;
            float inner2y = cy + sin2 * innerRadius;

            // Triangle 1
            buf.vertex(outer1x, outer1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();

            // Triangle 2
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner2x, inner2y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }

    /**
     * Draw an arc outline using line segments.
     */
    private void drawArcOutline(GuiGraphics graphics, float cx, float cy, float radius,
                                float startAngle, float endAngle, int segments, int color) {

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        var buf = tesselator.getBuilder();

        buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        float angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = startAngle + angleStep * i;
            float a2 = startAngle + angleStep * (i + 1);

            double rad1 = Math.toRadians(a1);
            double rad2 = Math.toRadians(a2);

            float x1 = cx + (float) Math.cos(rad1) * radius;
            float y1 = cy + (float) Math.sin(rad1) * radius;
            float x2 = cx + (float) Math.cos(rad2) * radius;
            float y2 = cy + (float) Math.sin(rad2) * radius;

            buf.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
            buf.vertex(x2, y2, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }
}
