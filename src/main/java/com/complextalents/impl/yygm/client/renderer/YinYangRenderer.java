package com.complextalents.impl.yygm.client.renderer;

import com.complextalents.network.yygm.ClientEquilibriumData;
import com.complextalents.origin.client.OriginRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Custom HUD renderer for the Yin Yang Grandmaster origin.
 * Displays Equilibrium stacks (0-8) as vertical arc segments on the right side of the screen.
 * Shows timer (numeric + visual) until stacks expire.
 */
public class YinYangRenderer implements OriginRenderer {

    // Arc configuration
    private static final float ARC_INNER_RADIUS = 25f;
    private static final float ARC_OUTER_RADIUS = 28f;
    private static final float ARC_LENGTH = 120f; // degrees
    private static final int ARC_SEGMENTS = 40;

    // Right side arc (stacks) - spans from 300° (bottom-right) through 0° to 60° (top-right)
    private static final float STACKS_BOTTOM_ANGLE = 300f;

    // Left side arc (timer) - spans from 240° (bottom-left) through 180° to 120° (top-left)
    private static final float TIMER_BOTTOM_ANGLE = 240f;

    // 8 stacks total, each occupies a portion of the arc
    private static final int MAX_STACKS = 8;

    // Color definitions (ARGB) - 60% opacity (0x99)
    private static final int BG_COLOR = 0x99000000;
    private static final int FILL_COLOR = 0x99BB88FF;  // Violet-purple
    private static final int DIVIDER_COLOR = 0x99000000;

    // Timer colors
    private static final int TIMER_HIGH = 0x9944FF44;   // Green (>5s)
    private static final int TIMER_MED = 0x99FFFF44;    // Yellow (3-5s)
    private static final int TIMER_LOW = 0x99FF4444;    // Red (<3s)

    // Timer text Y offset
    private static final float TIMER_TEXT_Y_OFFSET = 35f;

    // Cache for timer text
    private static String cachedTimerText = "";
    private static float lastTimerValue = -1f;

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        RenderSystem.enableBlend();
        renderEquilibriumArc(graphics, centerX, centerY);
        renderTimerArc(graphics, centerX, centerY);
        RenderSystem.disableBlend();
        renderLabels(graphics, centerX, centerY);
    }

    /**
     * Render Equilibrium stacks as arc segments on the RIGHT side.
     * Spans from 300° (bottom-right) through 0° to 60° (top-right).
     * Stacks fill from bottom (300°) upward toward top (60°).
     */
    private void renderEquilibriumArc(GuiGraphics graphics, int centerX, int centerY) {
        int equilibrium = ClientEquilibriumData.getEquilibrium();

        float segmentAngleLength = ARC_LENGTH / MAX_STACKS; // 15° per stack

        // Draw empty stacks (background) - from bottom to top
        for (int i = 0; i < MAX_STACKS; i++) {
            float startAngle = STACKS_BOTTOM_ANGLE + (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle, startAngle + segmentAngleLength - 0.8f,
                    3, BG_COLOR);
        }

        // Draw filled stacks - from bottom upward
        for (int i = 0; i < equilibrium && i < MAX_STACKS; i++) {
            float startAngle = STACKS_BOTTOM_ANGLE + (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle, startAngle + segmentAngleLength - 0.8f,
                    3, FILL_COLOR);
        }

        // Draw divider lines between stacks
        for (int i = 1; i < MAX_STACKS; i++) {
            float dividerAngle = STACKS_BOTTOM_ANGLE + (i * segmentAngleLength) - 0.4f;
            drawThickLine(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS, dividerAngle, DIVIDER_COLOR);
        }
    }

    /**
     * Render timer arc on the LEFT side.
     * Spans from 240° (bottom-left) through 180° to 120° (top-left).
     * Shows time remaining as a filled arc from bottom upward.
     */
    private void renderTimerArc(GuiGraphics graphics, int centerX, int centerY) {
        float secondsRemaining = ClientEquilibriumData.getSecondsRemaining();

        if (secondsRemaining <= 0) {
            return;
        }

        // Clamp to 10 seconds max
        float displayTime = Math.min(secondsRemaining, 10f);

        // Determine color based on time remaining
        int timerColor;
        if (displayTime > 5f) {
            timerColor = TIMER_HIGH;
        } else if (displayTime > 3f) {
            timerColor = TIMER_MED;
        } else {
            timerColor = TIMER_LOW;
        }

        // Flash in the last second
        if (displayTime < 1f) {
            long flashTime = System.currentTimeMillis() % 200;
            if (flashTime < 100) {
                timerColor = TIMER_LOW; // Full opacity during flash
            } else {
                timerColor = 0x66FF4444; // Lower opacity
            }
        }

        // Draw background arc (empty)
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                TIMER_BOTTOM_ANGLE - ARC_LENGTH, TIMER_BOTTOM_ANGLE,
                ARC_SEGMENTS, BG_COLOR);

        // Draw filled portion based on time remaining
        float fillRatio = displayTime / 10f;
        float fillAngleLength = ARC_LENGTH * fillRatio;

        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                TIMER_BOTTOM_ANGLE - fillAngleLength, TIMER_BOTTOM_ANGLE,
                (int) (ARC_SEGMENTS * fillRatio) + 1, timerColor);
    }

    /**
     * Render labels - timer text on the left side.
     */
    private void renderLabels(GuiGraphics graphics, int centerX, int centerY) {
        float secondsRemaining = ClientEquilibriumData.getSecondsRemaining();

        // Only show timer text if we have time remaining
        if (secondsRemaining <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Clamp to 10 seconds max
        float displayTime = Math.min(secondsRemaining, 10f);

        // Determine color based on time remaining
        int timerColor;
        if (displayTime > 5f) {
            timerColor = TIMER_HIGH;
        } else if (displayTime > 3f) {
            timerColor = TIMER_MED;
        } else {
            timerColor = TIMER_LOW;
        }

        // Flash in the last second
        if (displayTime < 1f) {
            long flashTime = System.currentTimeMillis() % 200;
            if (flashTime < 100) {
                timerColor = TIMER_LOW; // Full opacity during flash
            } else {
                timerColor = 0x66FF4444; // Lower opacity
            }
        }

        // Update cached text
        if (displayTime != lastTimerValue) {
            lastTimerValue = displayTime;
            cachedTimerText = String.format("%.1fs", displayTime);
        }

        // Draw timer text (left side, below the timer arc)
        int timerTextWidth = minecraft.font.width(cachedTimerText);
        int timerTextX = (int) (centerX - ARC_OUTER_RADIUS - 6 - timerTextWidth * 0.7f);
        int timerTextY = centerY + (int) TIMER_TEXT_Y_OFFSET - 4;

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedTimerText,
                (int) (timerTextX / 0.7f), (int) (timerTextY / 0.7f), timerColor);
        graphics.pose().popPose();
    }

    /**
     * Draw a thick radial line (divider between stacks).
     */
    private void drawThickLine(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                               float angleDegrees, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        var buf = tesselator.getBuilder();

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        double rad = Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        // Create a thick line (2 pixels wide on each side)
        float thickness = 4f;

        // Perpendicular offset (rotated 90 degrees)
        float perpCos = -sin * thickness;
        float perpSin = cos * thickness;

        // Four vertices for the thick line
        float x1 = cx + cos * innerRadius + perpCos;
        float y1 = cy + sin * innerRadius + perpSin;
        float x2 = cx + cos * outerRadius + perpCos;
        float y2 = cy + sin * outerRadius + perpSin;
        float x3 = cx + cos * innerRadius - perpCos;
        float y3 = cy + sin * innerRadius - perpSin;
        float x4 = cx + cos * outerRadius - perpCos;
        float y4 = cy + sin * outerRadius - perpSin;

        // Triangle 1
        buf.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
        buf.vertex(x3, y3, 0).color(r, g, b, a).endVertex();
        buf.vertex(x2, y2, 0).color(r, g, b, a).endVertex();

        // Triangle 2
        buf.vertex(x3, y3, 0).color(r, g, b, a).endVertex();
        buf.vertex(x4, y4, 0).color(r, g, b, a).endVertex();
        buf.vertex(x2, y2, 0).color(r, g, b, a).endVertex();

        tesselator.end();
    }

    /**
     * Draw a filled ring arc segment using BufferBuilder.
     * Creates a thick curved bar between two radii.
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

            // Normalize angles to 0-360 for calculation, but preserve arc continuity
            double rad1 = Math.toRadians(a1);
            double rad2 = Math.toRadians(a2);

            float cos1 = (float) Math.cos(rad1);
            float sin1 = (float) Math.sin(rad1);
            float cos2 = (float) Math.cos(rad2);
            float sin2 = (float) Math.sin(rad2);

            // Four vertices of the quad segment
            float outer1x = cx + cos1 * outerRadius;
            float outer1y = cy + sin1 * outerRadius;
            float outer2x = cx + cos2 * outerRadius;
            float outer2y = cy + sin2 * outerRadius;
            float inner1x = cx + cos1 * innerRadius;
            float inner1y = cy + sin1 * innerRadius;
            float inner2x = cx + cos2 * innerRadius;
            float inner2y = cy + sin2 * innerRadius;

            // Triangle 1: outer1 -> inner1 -> outer2
            buf.vertex(outer1x, outer1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();

            // Triangle 2: inner1 -> inner2 -> outer2
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner2x, inner2y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }
}
