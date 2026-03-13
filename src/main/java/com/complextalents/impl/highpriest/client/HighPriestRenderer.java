package com.complextalents.impl.highpriest.client;

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
 * Displays the Faith resource bar and Grace passive stack indicators as
 * symmetric arc segments.
 * Faith (right) and Grace (left) are mirrored around the center of the screen.
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

    // Grace arc (left side) - spans from 240° to 120° (bottom-left to top-left)
    // Fill starts from bottom (240°) and goes up
    private static final float GRACE_BOTTOM_ANGLE = 240f;

    // Color definitions (ARGB) - all at 60% opacity (0x99)
    private static final int FAITH_TEXT_COLOR = 0x99FFD700; // Gold

    private static final int GRACE_BG_COLOR = 0x99000000;
    private static final int COMMAND_FILL_COLOR = 0x99FFD700; // Gold
    private static final int COMMAND_FULL_COLOR = 0x99FFAA00; // Bright Gold when maxed
    private static final int COMMAND_DIVIDER_COLOR = 0x99000000; // Dark divider lines

    // Cache for Faith text - only rebuild when values change
    private static String cachedFaithText = "";
    private static double lastFaithValue = -1;

    // Cache for Command text
    private static String cachedCommandText = "";
    private static int lastCommandValue = -1;
    private static int lastGraceValue = -1;
    private static int lastGraceCooldown = -1;
    private static String cachedGraceCooldownText = "";

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        RenderSystem.enableBlend();
        renderCommandArc(graphics, centerX, centerY);
        RenderSystem.disableBlend();
        renderLabels(graphics, centerX, centerY);
    }

    /**
     * Render Command stacks as arc segments on the LEFT side.
     * Repurposed from Grace arc.
     */
    private void renderCommandArc(GuiGraphics graphics, int centerX, int centerY) {
        int command = ClientPassiveStackData.getStackCount("command");
        int maxCommand = 10;
        boolean hasGrace = ClientPassiveStackData.getStackCount("grace") > 0;

        float segmentAngleLength = ARC_LENGTH / maxCommand; // 12° per stack

        // Draw empty stacks (background)
        for (int i = 0; i < maxCommand; i++) {
            float startAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle - segmentAngleLength + 0.8f, startAngle,
                    3, GRACE_BG_COLOR);
        }

        // Draw filled stacks
        int fillColor = command >= maxCommand ? COMMAND_FULL_COLOR : COMMAND_FILL_COLOR;

        // Visual indicator for Grace - glow if active
        if (hasGrace) {
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS - 1.5f, ARC_OUTER_RADIUS + 1.5f,
                    GRACE_BOTTOM_ANGLE - ARC_LENGTH, GRACE_BOTTOM_ANGLE,
                    16, 0x33E6F0FF); // Subtle blue glow for Grace
        }

        for (int i = 0; i < command && i < maxCommand; i++) {
            float startAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    startAngle - segmentAngleLength + 0.8f, startAngle,
                    3, fillColor);
        }

        // Draw dividers
        for (int i = 1; i < maxCommand; i++) {
            float dividerAngle = GRACE_BOTTOM_ANGLE - (i * segmentAngleLength);
            drawThickLine(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS, dividerAngle,
                    COMMAND_DIVIDER_COLOR);
        }
    }

    /**
     * Render labels - just the values, no names.
     */
    private void renderLabels(GuiGraphics graphics, int centerX, int centerY) {
        Minecraft minecraft = Minecraft.getInstance();

        // Faith value (right side) - just the number
        double faith = com.complextalents.impl.highpriest.client.ClientFaithData.getFaith();
        if (faith != lastFaithValue) {
            lastFaithValue = faith;
            cachedFaithText = formatFaithCount(faith);
        }

        int faithTextX = (int) (centerX + ARC_OUTER_RADIUS + 6);
        int faithTextY = centerY - 3;

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedFaithText,
                (int) (faithTextX / 0.7f), (int) (faithTextY / 0.7f), FAITH_TEXT_COLOR);
        graphics.pose().popPose();

        // Command value (left side)
        int command = ClientPassiveStackData.getStackCount("command");
        int grace = ClientPassiveStackData.getStackCount("grace");
        int maxCommand = 10;

        if (command != lastCommandValue || grace != lastGraceValue) {
            lastCommandValue = command;
            lastGraceValue = grace;
            cachedCommandText = "Command: " + command + "/" + maxCommand;
        }

        int commandTextWidth = minecraft.font.width(cachedCommandText);
        int commandTextX = (int) (centerX - ARC_OUTER_RADIUS - 8 - commandTextWidth * 0.7f);
        int commandTextY = centerY - 3;

        // Color based on Grace active state
        int textColor = grace > 0 ? 0x99E6F0FF : 0x99AAAAAA; // Bright blue if active, gray if inactive

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(minecraft.font, cachedCommandText,
                (int) (commandTextX / 0.7f), (int) (commandTextY / 0.7f), textColor);
        graphics.pose().popPose();

        // Grace Cooldown Timer (below Command text)
        int graceCooldown = ClientPassiveStackData.getStackCount("grace_cooldown");
        if (graceCooldown > 0) {
            if (graceCooldown != lastGraceCooldown) {
                lastGraceCooldown = graceCooldown;
                cachedGraceCooldownText = String.format("%.1fs", graceCooldown / 20.0f);
            }

            int cooldownWidth = minecraft.font.width(cachedGraceCooldownText);
            int cooldownX = (int) (centerX - ARC_OUTER_RADIUS - 8 - cooldownWidth * 0.6f);
            int cooldownY = centerY + 5;

            graphics.pose().pushPose();
            graphics.pose().scale(0.6f, 0.6f, 0.6f);
            graphics.drawString(minecraft.font, cachedGraceCooldownText,
                    (int) (cooldownX / 0.6f), (int) (cooldownY / 0.6f), 0x99AAAAAA);
            graphics.pose().popPose();
        }
    }

    /**
     * Format faith count for display, acting similar to Dark Mage's soul format.
     */
    private String formatFaithCount(double faith) {
        if (faith < 1000) {
            return String.format("%.2f Faith", faith);
        } else if (faith < 1000000) {
            return String.format("%.2fK Faith", faith / 1000.0);
        } else {
            return String.format("%.2fM Faith", faith / 1000000.0);
        }
    }

    /**
     * Draw a thick radial line (divider between Grace stacks).
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
     * Handles angle wrapping (angles can exceed 360°).
     *
     * @param graphics    The GUI graphics context
     * @param cx          Center X
     * @param cy          Center Y
     * @param innerRadius Inner radius of the ring
     * @param outerRadius Outer radius of the ring
     * @param startAngle  Start angle in degrees
     * @param endAngle    End angle in degrees (can be > 360)
     * @param segments    Number of segments for smoothness
     * @param color       ARGB color
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
