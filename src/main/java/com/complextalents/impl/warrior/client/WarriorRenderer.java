package com.complextalents.impl.warrior.client;
 
import com.complextalents.impl.warrior.WarriorOriginHandler;
import com.complextalents.origin.client.ClientOriginData;
import com.complextalents.origin.client.OriginRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
 
/**
 * Custom HUD renderer for the Warrior origin.
 * Displays Style Meter (left) and Shield (right) while charging.
 */
public class WarriorRenderer implements OriginRenderer {
 
    private static final float ARC_INNER_RADIUS = 25f;
    private static final float ARC_OUTER_RADIUS = 28f;
    private static final float ARC_LENGTH = 120f;
 
    // Left Arc (Style) - 240 to 120 (bottom-left to top-left)
    private static final float STYLE_BOTTOM_ANGLE = 240f;
    
    // Right Arc (Shield) - 300 to 60 (bottom-right to top-right)
    private static final float SHIELD_BOTTOM_ANGLE = 300f;
 
    // Colors
    private static final int SHIELD_FULL_COLOR = 0x9944AAFF; // Blue
    private static final int SHIELD_EMPTY_COLOR = 0x99FF4444; // Red
    private static final int SHIELD_BG_COLOR = 0x66000000;
 
    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
 
        RenderSystem.enableBlend();
        renderStyleArc(graphics, centerX, centerY);
        renderShieldArc(graphics, centerX, centerY);
        RenderSystem.disableBlend();
        
        renderLabels(graphics, centerX, centerY);
    }
 
    private void renderStyleArc(GuiGraphics graphics, int centerX, int centerY) {
        double points = ClientOriginData.getResourceValue();
        WarriorOriginHandler.StyleRank rank = WarriorOriginHandler.StyleRank.getRank(points);
        
        float progress;
        if (rank == WarriorOriginHandler.StyleRank.SSS && points >= 1000) {
            progress = 1.0f;
        } else {
            progress = (float) ((points - rank.min) / (double) (rank.max - rank.min + 1));
        }
        progress = Math.min(1.0f, Math.max(0.0f, progress));
        
        int fillColor = rank.color;
 
        // Background
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS, 
                STYLE_BOTTOM_ANGLE - ARC_LENGTH, STYLE_BOTTOM_ANGLE, 16, SHIELD_BG_COLOR);
        
        // Fill
        if (progress > 0) {
            float fillAngle = progress * ARC_LENGTH;
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    STYLE_BOTTOM_ANGLE - fillAngle, STYLE_BOTTOM_ANGLE, 16, fillColor);
        }
    }
 
    private void renderShieldArc(GuiGraphics graphics, int centerX, int centerY) {
        double shield = ClientOriginData.getShieldValue();
        double shieldMax = ClientOriginData.getShieldMax();
        
        if (shield <= 0 || shieldMax <= 0) return;
 
        float progress = (float) (shield / shieldMax);
        progress = Math.min(1.0f, Math.max(0.0f, progress));
        
        // Colors for fading
        int r1 = (SHIELD_FULL_COLOR >> 16) & 0xFF;
        int g1 = (SHIELD_FULL_COLOR >> 8) & 0xFF;
        int b1 = SHIELD_FULL_COLOR & 0xFF;
        int a1 = (SHIELD_FULL_COLOR >> 24) & 0xFF;
        
        int r2 = (SHIELD_EMPTY_COLOR >> 16) & 0xFF;
        int g2 = (SHIELD_EMPTY_COLOR >> 8) & 0xFF;
        int b2 = SHIELD_EMPTY_COLOR & 0xFF;
        int a2 = (SHIELD_EMPTY_COLOR >> 24) & 0xFF;
        
        // Interpolate color (Blue -> Red)
        int r = (int) (r2 + (r1 - r2) * progress);
        int g = (int) (g2 + (g1 - g2) * progress);
        int b = (int) (b2 + (b1 - b2) * progress);
        int a = (int) (a2 + (a1 - a2) * progress);
        int fillColor = (a << 24) | (r << 16) | (g << 8) | b;
 
        // Background
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                SHIELD_BOTTOM_ANGLE, SHIELD_BOTTOM_ANGLE + ARC_LENGTH, 16, SHIELD_BG_COLOR);
        
        // Fill
        if (progress > 0) {
            float fillAngle = progress * ARC_LENGTH;
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    SHIELD_BOTTOM_ANGLE, SHIELD_BOTTOM_ANGLE + fillAngle, 16, fillColor);
        }
    }
 
    private void renderLabels(GuiGraphics graphics, int centerX, int centerY) {
        Minecraft mc = Minecraft.getInstance();
        double points = ClientOriginData.getResourceValue();
        WarriorOriginHandler.StyleRank rank = WarriorOriginHandler.StyleRank.getRank(points);
        
        // 1. Style Rank Label (Left)
        String rankText = rank.name;
        int rankColor = (rank.color & 0x00FFFFFF) | 0xFF000000; // Force opaque for text
        
        int rankX = (int) (centerX - ARC_OUTER_RADIUS - 10 - mc.font.width(rankText) * 0.8f);
        int rankY = centerY - 10;
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 0.8f);
        graphics.drawString(mc.font, rankText, (int)(rankX / 0.8f), (int)(rankY / 0.8f), rankColor);
        graphics.pose().popPose();
 
        // 2. Style Value (Left, below rank)
        String styleValueText;
        WarriorOriginHandler.StyleRank[] ranks = WarriorOriginHandler.StyleRank.values();
        int nextIndex = rank.ordinal() + 1;
        if (nextIndex < ranks.length) {
            styleValueText = String.format("%.0f to %s", ranks[nextIndex].min - points, ranks[nextIndex].name);
        } else {
            styleValueText = rank.fullName;
        }

        int valX = (int) (centerX - ARC_OUTER_RADIUS - 10 - mc.font.width(styleValueText) * 0.5f);
        int valY = centerY + 2;
 
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 0.5f);
        graphics.drawString(mc.font, styleValueText, (int)(valX / 0.5f), (int)(valY / 0.5f), 0xFFAAAAAA);
        graphics.pose().popPose();
 
        // 3. Shield Value (Right)
        double shield = ClientOriginData.getShieldValue();
        double shieldMax = ClientOriginData.getShieldMax();
        if (shield > 0 && shieldMax > 0) {
            String shieldText = String.format("%.0f HP", shield);
            int shieldX = (int) (centerX + ARC_OUTER_RADIUS + 10);
            int shieldY = centerY - 3;
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.7f, 0.7f, 0.7f);
            graphics.drawString(mc.font, shieldText, (int)(shieldX / 0.7f), (int)(shieldY / 0.7f), 0xFFAABBFF);
            graphics.pose().popPose();
        }
    }
 
    private void drawArcSegment(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                               float startAngle, float endAngle, int segments, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
 
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
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
 
            buf.vertex(outer1x, outer1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();
 
            buf.vertex(inner1x, inner1y, 0).color(r, g, b, a).endVertex();
            buf.vertex(inner2x, inner2y, 0).color(r, g, b, a).endVertex();
            buf.vertex(outer2x, outer2y, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
    }
}
