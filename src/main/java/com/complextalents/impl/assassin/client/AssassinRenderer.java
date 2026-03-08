package com.complextalents.impl.assassin.client;

import com.complextalents.TalentsMod;
import com.complextalents.impl.assassin.effect.AssassinEffects;
import com.complextalents.origin.client.OriginRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.complextalents.origin.client.ClientOriginData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-side renderer for Assassin kit.
 * Handles HUD (Stealth Gauge), Exposed marks, and absolute invisibility.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AssassinRenderer implements OriginRenderer {

    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/misc/white.png");

    // Stealth Gauge Arc Configuration (Left Side - Mirrored from DarkMage)
    private static final float ARC_INNER_RADIUS = 25f;
    private static final float ARC_OUTER_RADIUS = 28f;
    private static final float ARC_LENGTH = 120f; // degrees
    private static final int ARC_SEGMENTS = 40;
    
    // Arc position (left side) - spans from 120° to 240°
    private static final float ARC_BOTTOM_ANGLE = 120f;

    // Color definitions (ARGB) - 60% opacity (0x99)
    private static final int STEALTH_BG_COLOR = 0x99000000;
    private static final int STEALTH_FILL_COLOR = 0x99FFFFFF; // White/Gray for stealth
    private static final int STEALTH_BORDER_COLOR = 0x99BBBBBB;

    @Override
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        double gauge = ClientAssassinData.getStealthGauge();
        double maxGauge = ClientAssassinData.getMaxStealthGauge();
        float percent = (float) (gauge / maxGauge);

        // Only display stealth bar if it is not full
        if (percent >= 1.0f)
            return;

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        RenderSystem.enableBlend();
        renderStealthArc(graphics, centerX, centerY, percent);
        renderLabels(graphics, centerX, centerY, gauge, maxGauge);
        RenderSystem.disableBlend();
    }

    private void renderStealthArc(GuiGraphics graphics, int centerX, int centerY, float percent) {
        // Draw background arc (full empty bar)
        drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, STEALTH_BG_COLOR);

        // Draw filled portion (represents current gauge)
        if (percent > 0) {
            float fillAngleLength = ARC_LENGTH * percent;
            drawArcSegment(graphics, centerX, centerY, ARC_INNER_RADIUS, ARC_OUTER_RADIUS,
                    ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + fillAngleLength,
                    (int) (ARC_SEGMENTS * percent) + 1, STEALTH_FILL_COLOR);
        }

        // Draw arc outline
        drawArcOutline(graphics, centerX, centerY, ARC_OUTER_RADIUS,
                ARC_BOTTOM_ANGLE, ARC_BOTTOM_ANGLE + ARC_LENGTH,
                ARC_SEGMENTS, STEALTH_BORDER_COLOR);
    }

    private void renderLabels(GuiGraphics graphics, int centerX, int centerY, double gauge, double maxGauge) {
        Minecraft mc = Minecraft.getInstance();
        
        // Stealth label (left side)
        String label = "STEALTH";
        int labelWidth = mc.font.width(label);
        int labelX = (int) (centerX - ARC_OUTER_RADIUS - 6 - labelWidth * 0.7f);
        int labelY = centerY - 5;

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(mc.font, label, (int) (labelX / 0.7f), (int) (labelY / 0.7f), 0x99FFFFFF);
        graphics.pose().popPose();

        // Value label (below STEALTH)
        String valueText = (int) gauge + "/" + (int) maxGauge;
        int valueWidth = mc.font.width(valueText);
        int valueX = (int) (centerX - ARC_OUTER_RADIUS - 6 - valueWidth * 0.6f);
        int valueY = centerY + 3;

        graphics.pose().pushPose();
        graphics.pose().scale(0.6f, 0.6f, 0.6f);
        graphics.drawString(mc.font, valueText, (int) (valueX / 0.6f), (int) (valueY / 0.6f), 0x99CCCCCC);
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        ResourceLocation assassinId = ResourceLocation.fromNamespaceAndPath("complextalents", "assassin");
        boolean isAssassin = assassinId.equals(ClientOriginData.getOriginId());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity) || entity == mc.player)
                continue;

            boolean hasExposed = livingEntity.hasEffect(AssassinEffects.EXPOSE_WEAKNESS.get());

            if (isAssassin || hasExposed) {
                // Calculate position relative to camera
                double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
                double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
                double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

                Vec3 cameraPos = camera.getPosition();
                double dx = lerpX - cameraPos.x;
                double dy = lerpY - cameraPos.y;
                double dz = lerpZ - cameraPos.z;

                poseStack.pushPose();
                poseStack.translate(dx, dy, dz);

                // 2. Render Backstab Arc (Assassin only)
                if (isAssassin) {
                    renderBackstabArc(poseStack, bufferSource, livingEntity, partialTick);
                }

                // 3. Render Exposed Mark (Anyone with the effect)
                if (hasExposed) {
                    renderExposedMark(poseStack, bufferSource, livingEntity);
                }

                poseStack.popPose();
            }
        }

        // Force draw to ensure visibility
        bufferSource.endBatch(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // Handle Absolute Invisibility
        if (entity.hasEffect(AssassinEffects.SHADOW_WALK.get())) {
            Player localPlayer = Minecraft.getInstance().player;
            if (entity != localPlayer) {
                // Completely hide others for the local player
                event.setCanceled(true);
            } else {
                // Local player is semi-transparent to themselves
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.3f);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(AssassinEffects.SHADOW_WALK.get())) {
            // Hide nametag
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && localPlayer.hasEffect(AssassinEffects.SHADOW_WALK.get())) {
            // Hide own hand/items in first person
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == Minecraft.getInstance().player && entity.hasEffect(AssassinEffects.SHADOW_WALK.get())) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static void renderBackstabArc(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
            float partialTicks) {
        poseStack.pushPose();

        // Lift slightly off ground
        poseStack.translate(0, 0.05, 0);

        // Rotate to match entity's Y rotation (Smooth with partial ticks)
        // Entity Y Rot: 0 = South, 90 = West, 180 = North, 270 = East
        // We want to render the arc behind the entity.
        float yRot = entity.getViewYRot(partialTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

        com.mojang.blaze3d.vertex.VertexConsumer vc = buffer
                .getBuffer(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        float width = Math.max(entity.getBbWidth(), 0.8f);
        float innerRadius = width * 1.1f;
        float outerRadius = width * 1.15f; // Slimmer!

        // Render a single arc from -45 to 45 degrees (North / Behind if facing South)
        // In our coordinate system (sin(a), -cos(a)):
        // a=0 is North (0, -1)
        // If entity faces South (yRot=0), its back is North.
        float startAngle = -45f;
        float endAngle = 45f;
        int segments = 20;
        int light = 15728880;

        // Cooldown check
        long[] cooldownRange = ClientAssassinData.getEntityCooldown(entity.getId());
        if (entity.level() != null) {
            long currentTime = entity.level().getGameTime();

            if (cooldownRange != null && currentTime < cooldownRange[1]) {
                float progress = (float) (currentTime - cooldownRange[0])
                        / (float) (cooldownRange[1] - cooldownRange[0]);
                progress = Math.max(0, Math.min(1, progress));

                // Render grey filling arc based on progress
                float currentEnd = startAngle + (endAngle - startAngle) * progress;
                renderArcSegments(vc, pose, startAngle, currentEnd, segments, innerRadius, outerRadius, 0.5f, 0.5f,
                        0.5f, 0.5f, light);
            } else {
                // Ready: Full red arc
                renderArcSegments(vc, pose, startAngle, endAngle, segments, innerRadius, outerRadius, 1.0f, 0.0f, 0.0f,
                        0.5f, light);
            }
        }

        poseStack.popPose();
    }

    private static void renderArcSegments(com.mojang.blaze3d.vertex.VertexConsumer vc, Matrix4f pose, float startAngle,
            float endAngle, int segments, float innerRadius, float outerRadius, float r, float g, float b, float a,
            int light) {
        if (startAngle >= endAngle)
            return;
        float angleStep = (endAngle - startAngle) / segments;
        for (int i = 0; i < segments; i++) {
            float a1 = (float) Math.toRadians(startAngle + angleStep * i);
            float a2 = (float) Math.toRadians(startAngle + angleStep * (i + 1));

            float x1i = (float) Math.sin(a1) * innerRadius;
            float z1i = (float) -Math.cos(a1) * innerRadius;
            float x1o = (float) Math.sin(a1) * outerRadius;
            float z1o = (float) -Math.cos(a1) * outerRadius;

            float x2i = (float) Math.sin(a2) * innerRadius;
            float z2i = (float) -Math.cos(a2) * innerRadius;
            float x2o = (float) Math.sin(a2) * outerRadius;
            float z2o = (float) -Math.cos(a2) * outerRadius;

            drawTrapezoid(vc, pose, x1i, z1i, x1o, z1o, x2o, z2o, x2i, z2i, r, g, b, a, light);
        }
    }

    private static void renderExposedMark(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity) {
        poseStack.pushPose();

        // Translate to center mass (center of bounding box)
        poseStack.translate(0, entity.getBbHeight() / 2.0f, 0);

        // billboard rendering: Face camera
        Minecraft mc = Minecraft.getInstance();
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        // Scale with mob size
        float scale = entity.getBbHeight() * 0.4f; // Increased for visibility
        poseStack.scale(scale, scale, scale);

        // Render with emissive to ignore lighting
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffer
                .getBuffer(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        // Draw red "X"
        float thickness = 0.15f;
        float size = 1.6f;
        int light = 15728880;

        // Slant 1 (Top-Left to Bottom-Right)
        drawRotatedQuad(vc, pose, 45, size, thickness, 1.0f, 0, 0, 1.0f, light);
        // Slant 2 (Top-Right to Bottom-Left)
        drawRotatedQuad(vc, pose, -45, size, thickness, 1.0f, 0, 0, 1.0f, light);

        poseStack.popPose();
    }

    private static void drawTrapezoid(com.mojang.blaze3d.vertex.VertexConsumer vc, Matrix4f pose, float x1, float z1,
            float x2, float z2, float x3, float z3, float x4, float z4, float r, float g, float b, float a, int light) {
        vc.vertex(pose, x1, 0, z1).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2, 0, z2).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 1, 0).endVertex();
        vc.vertex(pose, x3, 0, z3).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 1, 0).endVertex();
        vc.vertex(pose, x4, 0, z4).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 1, 0).endVertex();
    }

    private static void drawRotatedQuad(com.mojang.blaze3d.vertex.VertexConsumer vc, Matrix4f pose, float angleDeg,
            float length, float thickness, float r, float g, float b, float a, int light) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float hw = length / 2f;
        float ht = thickness / 2f;

        // Corner points before rotation
        float[][] points = {
                { -hw, -ht }, { hw, -ht }, { hw, ht }, { -hw, ht }
        };

        float[] x = new float[4];
        float[] y = new float[4];

        for (int i = 0; i < 4; i++) {
            x[i] = points[i][0] * cos - points[i][1] * sin;
            y[i] = points[i][0] * sin + points[i][1] * cos;
        }

        vc.vertex(pose, x[0], y[0], 0).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vc.vertex(pose, x[1], y[1], 0).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vc.vertex(pose, x[2], y[2], 0).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vc.vertex(pose, x[3], y[3], 0).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
    }

    private void drawArcSegment(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius,
                                float startAngle, float endAngle, int segments, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        var buf = tesselator.getBuilder();
        buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

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

    private void drawArcOutline(GuiGraphics graphics, float cx, float cy, float radius,
                                float startAngle, float endAngle, int segments, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        var buf = tesselator.getBuilder();
        buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

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
