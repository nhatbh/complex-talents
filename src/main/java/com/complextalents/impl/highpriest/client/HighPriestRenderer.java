package com.complextalents.impl.highpriest.client;

import com.complextalents.origin.client.ClientOriginData;
import com.complextalents.origin.client.OriginRenderer;
import com.complextalents.passive.client.ClientPassiveStackData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Custom HUD renderer for the High Priest origin.
 * Displays the Piety resource bar and Grace passive stack indicators as symmetric arc segments.
 * Piety (right) and Grace (left) are mirrored around the center of the screen.
 * <p>
 * Performance optimizations:
 * - Caches text strings, only updates when values change
 * - Avoids String.format and Component creation
 * - Uses BufferBuilder for efficient arc rendering
 * </p>
 */
public class HighPriestRenderer implements OriginRenderer {

    // Shared arc configuration - both arcs use same size for symmetry
    private static final float ARC_INNER_RADIUS = 25f;
    private static final float ARC_OUTER_RADIUS = 28f;
    private static final float ARC_LENGTH = 120f; // degrees
    private static final int ARC_SEGMENTS = 40;

    // Piety arc (right side) - spans from 60° to 300° counter-clockwise (bottom to top on right)
    // Fill starts from bottom (300°) and goes up
    private static final float PIETY_BOTTOM_ANGLE = 300f;

    // Grace arc (left side) - spans from 240° to 120° (bottom-left to top-left)
    // Fill starts from bottom (240°) and goes up
    private static final float GRACE_BOTTOM_ANGLE = 240f;

    // Color definitions (ARGB) - all at 60% opacity (0x99)
    private static final int PIETY_BG_COLOR = 0x99000000;
    private static final int PIETY_FILL_COLOR = 0x99FFD700; // Gold
    private static final int PIETY_BORDER_COLOR = 0x99FFFFFF;

    private static final int GRACE_BG_COLOR = 0x99000000;
    private static final int GRACE_FILL_COLOR = 0x99E6F0FF; // Light blue
    private static final int GRACE_FULL_COLOR = 0x9900BFFF; // Deep sky blue when maxed
    private static final int GRACE_DIVIDER_COLOR = 0x99000000; // Dark divider lines

    // Cache for Piety text - only rebuild when values change
    private static String cachedPietyText = "";
    private static double lastPietyValue = -1;
    private static double lastPietyMax = -1;

    // Cache for Grace text
    private static String cachedGraceText = "";
    private static int lastGraceValue = -1;

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        RenderSystem.enableBlend();
        renderPietyArc(graphics, centerX, centerY);
        renderGraceArc(graphics, centerX, centerY);
        RenderSystem.disableBlend();
        renderLabels(graphics, centerX, centerY);
    }

    /**
     * Render the Piety resource as a ring arc on the RIGHT side.
     * Spans from 300° (bottom-right) through 0° to 60° (top-right).
     * Fills from bottom (300°) upward toward top (60°).
     */
    private void renderPietyArc(GuiGraphics graphics, int centerX, int centerY) {
        double piety = ClientOriginData.getResourceValue();
        double max = ClientOriginData.getResourceMax();
        double fillRatio = max > 0 ? Math.min(1.0, Math.max(0.0, piety / max)) : 0;

        // Draw background arc (full empty bar) - from 300° to 420° (wraps to 60°)
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                PIETY_BOTTOM_ANGLE, PIETY_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, PIETY_BG_COLOR);

        // Draw filled portion - from bottom (300°) going up
        if (fillRatio > 0) {
            float fillAngleLength = ARC_LENGTH * (float) fillRatio;
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    PIETY_BOTTOM_ANGLE, PIETY_BOTTOM_ANGLE + fillAngleLength,
                    (int) (ARC_SEGMENTS * fillRatio) + 1, PIETY_FILL_COLOR);
        }

        // Draw arc outline
        drawArcOutline(graphics, centerX, centerY, ARC_OUTER_RADIUS,
                PIETY_BOTTOM_ANGLE, PIETY_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, PIETY_BORDER_COLOR);
    }

    /**
     * Render Grace stacks as arc segments on the LEFT side.
     * Spans from 240° (bottom-left) through 180° to 120° (top-left).
     * Fills from bottom (240°) upward toward top (120°).
     * Each of 10 stacks occupies 12° of the arc.
     */
    private void renderGraceArc(GuiGraphics graphics, int centerX, int centerY) {
        int grace = ClientPassiveStackData.getStackCount("grace");
        int maxGrace = 10;

        float segmentAngleLength = ARC_LENGTH / maxGrace; // 12° per stack

        // Draw empty stacks (background) - from bottom to top
        for (int i = 0; i < maxGrace; i++) {
            // Start from bottom (240°) and go up (counter-clockwise, so subtract)
            float startAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle - segmentAngleLength + 0.8f, startAngle,
                    3, GRACE_BG_COLOR);
        }

        // Draw filled stacks - from bottom upward
        boolean isMaxed = grace >= maxGrace;
        int fillColor = isMaxed ? GRACE_FULL_COLOR : GRACE_FILL_COLOR;

        for (int i = 0; i < grace && i < maxGrace; i++) {
            float startAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle - segmentAngleLength + 0.8f, startAngle,
                    3, fillColor);
        }

        // Draw thicker divider lines between stacks
        for (int i = 1; i < maxGrace; i++) {
            float dividerAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawThickLine(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS, dividerAngle, GRACE_DIVIDER_COLOR);
        }
    }

    /**
     * Render labels - just the values, no names.
     */
    private void renderLabels(GuiGraphics graphics, int centerX, int centerY) {
        Minecraft minecraft = Minecraft.getInstance();

        // Piety value (right side) - just the number
        double piety = ClientOriginData.getResourceValue();
        double pietyMax = ClientOriginData.getResourceMax();
        if (piety != lastPietyValue || pietyMax != lastPietyMax) {
            lastPietyValue = piety;
            lastPietyMax = pietyMax;
            cachedPietyText = (int) piety + "/" + (int) pietyMax;
        }

        int pietyTextX = (int) (centerX + ARC_OUTER_RADIUS + 6);
        int pietyTextY = centerY - 3;

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedPietyText,
                (int) (pietyTextX / 0.7f), (int) (pietyTextY / 0.7f), 0x99FFFFFF);
        graphics.pose().popPose();

        // Grace value (left side) - just the count/max
        int grace = ClientPassiveStackData.getStackCount("grace");
        int maxGrace = 10;

        if (grace != lastGraceValue) {
            lastGraceValue = grace;
            cachedGraceText = grace + "/" + maxGrace;
        }

        int graceTextWidth = minecraft.font.width(cachedGraceText);
        int graceTextX = (int) (centerX - ARC_OUTER_RADIUS - 6 - graceTextWidth * 0.7f);
        int graceTextY = centerY - 3;
        int graceColor = grace >= 10 ? 0x99FFAA00 : 0x99FFFFFF;

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedGraceText,
                (int) (graceTextX / 0.7f), (int) (graceTextY / 0.7f), graceColor);
        graphics.pose().popPose();
    }

    /**
     * Draw a thick radial line (divider between Grace stacks).
     */
    private void drawThickLine(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float angleDegrees, int color) {
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
     * Handles angle wrapping (angles can exceed 360°).
     *
     * @param graphics The GUI graphics context
     * @param cx Center X
     * @param cy Center Y
     * @param innerRadius Inner radius of the ring
     * @param outerRadius Outer radius of the ring
     * @param startAngle Start angle in degrees
     * @param endAngle End angle in degrees (can be > 360)
     * @param segments Number of segments for smoothness
     * @param color ARGB color
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

    /**
     * Draw an arc outline using line segments.
     *
     * @param graphics The GUI graphics context
     * @param cx Center X
     * @param cy Center Y
     * @param radius Radius of the arc
     * @param startAngle Start angle in degrees
     * @param endAngle End angle in degrees (can be > 360)
     * @param segments Number of segments
     * @param color ARGB color
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
