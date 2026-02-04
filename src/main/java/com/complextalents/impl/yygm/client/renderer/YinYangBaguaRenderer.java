package com.complextalents.impl.yygm.client.renderer;

import com.complextalents.TalentsMod;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * World overlay renderer for YYGM gates.
 * Renders the Bagua compass under harmonized entities with:
 * - Gold (Yang) gate
 * - Silver (Yin) gate
 * - Inner octagon fill indicating next required gate
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class YinYangBaguaRenderer {


    // Textures
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    // Geometry Constants
    private static final float OUTER_RADIUS_MULT = 1.5f;
    private static final float INNER_RADIUS_MULT = 0.6f;
    private static final float LINE_THICKNESS = 0.04f;

    // Trigram Data (Bottom to Top)
    private static final int[][] TRIGRAMS = {
            {0, 0, 0}, {0, 0, 1}, {0, 1, 0}, {0, 1, 1},
            {1, 1, 1}, {1, 1, 0}, {1, 0, 1}, {1, 0, 0}
    };

    /**
     * CHANGED: Replaced RenderLivingEvent.Post with RenderLevelStageEvent.
     * This renders the overlay in the world pass, independent of the entity's renderer.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Render after translucent objects (water, ice, stained glass) so our glow shows properly
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick();
        
        // We need to use the global buffer source
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Loop through all entities in the client world
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity livingEntity && shouldRenderBagua(livingEntity)) {

                // 1. Calculate Entity Position relative to Camera
                double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
                double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
                double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

                Vec3 cameraPos = camera.getPosition();
                double dx = lerpX - cameraPos.x;
                double dy = lerpY - cameraPos.y;
                double dz = lerpZ - cameraPos.z;

                poseStack.pushPose();

                // 2. Move PoseStack to the entity
                poseStack.translate(dx, dy + 0.05, dz); // +0.05 to lift off ground

                // 2.5. Rotate the entire Bagua overlay by 22.5 degrees
                poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

                // 3. Render
                int light = 15728880; // Full brightness

                // Render Harmonized gates (dual-gate system)
                for (UUID playerUuid : YinYangGateStateSyncPacket.ClientGateData.getPlayersForEntity(entity.getId())) {
                    renderBaguaForPlayer(livingEntity, playerUuid, poseStack, bufferSource, light);
                }

                // Render Exposed gates (Eight Formation Battle Array - all 8 gates)
                for (UUID playerUuid : ExposedStateSyncPacket.ClientExposedData.getPlayersForEntity(entity.getId())) {
                    renderExposedGatesForPlayer(livingEntity, playerUuid, poseStack, bufferSource, light);
                }

                poseStack.popPose();
            }
        }
        
        // Ensure our buffers are drawn immediately to avoid ordering issues
        // (Optional: remove if it causes flickering with other transparency, but usually recommended for custom events)
        bufferSource.endBatch(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
    }

    private static boolean shouldRenderBagua(LivingEntity entity) {
        return YinYangGateStateSyncPacket.ClientGateData.hasGates(entity.getId())
            || ExposedStateSyncPacket.ClientExposedData.hasExposed(entity.getId());
    }

    private static void renderBaguaForPlayer(LivingEntity entity, UUID playerUuid,
                                             PoseStack poseStack, MultiBufferSource buffer,
                                             int light) {
        YinYangGateStateSyncPacket.YYGateData gateData = YinYangGateStateSyncPacket.ClientGateData.getGateData(entity.getId(), playerUuid);
        if (gateData == null) return;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        float entityWidth = Math.max(entity.getBbWidth(), 0.8f);
        float outerRadius = entityWidth * OUTER_RADIUS_MULT;
        float innerRadius = outerRadius * INNER_RADIUS_MULT;

        // Render inner octagon fill to indicate next required gate
        int nextRequired = gateData.getNextRequired();
        float innerFillR, innerFillG, innerFillB, innerFillA;
        if (nextRequired == 0) {
            innerFillR = 1.0f; innerFillG = 0.84f; innerFillB = 0.0f; innerFillA = 0.3f; // Gold for Yang
        } else {
            innerFillR = 0.88f; innerFillG = 0.88f; innerFillB = 0.88f; innerFillA = 0.3f; // Silver for Yin
        }
        // Draw filled octagon for center area
        for (int oct = 0; oct < 8; oct++) {
            float octAngle1 = (float) Math.toRadians(oct * 45.0f);
            float octAngle2 = (float) Math.toRadians((oct + 1) * 45.0f);
            float ox1 = (float) Math.sin(octAngle1) * innerRadius;
            float oz1 = (float) -Math.cos(octAngle1) * innerRadius;
            float ox2 = (float) Math.sin(octAngle2) * innerRadius;
            float oz2 = (float) -Math.cos(octAngle2) * innerRadius;
            drawTrapezoid(vertexConsumer, pose, 0, 0, ox1, oz1, ox2, oz2, 0, 0, innerFillR, innerFillG, innerFillB, innerFillA, 15728880);
        }

        for (int i = 0; i < 8; i++) {
            float angleDeg = i * 45.0f;
            float angleRad = (float) Math.toRadians(angleDeg);
            float nextAngleRad = (float) Math.toRadians((i + 1) * 45.0f);

            // Coordinates
            float sin = (float) Math.sin(angleRad);
            float cos = (float) -Math.cos(angleRad);
            float nextSin = (float) Math.sin(nextAngleRad);
            float nextCos = (float) -Math.cos(nextAngleRad);

            float xInner = sin * innerRadius; float zInner = cos * innerRadius;
            float xOuter = sin * outerRadius; float zOuter = cos * outerRadius;
            float nextXInner = nextSin * innerRadius; float nextZInner = nextCos * innerRadius;
            float nextXOuter = nextSin * outerRadius; float nextZOuter = nextCos * outerRadius;

            int state = getGateState(gateData, i);

            // A. Filled Trapezoid
            if (state != 2) {
                float r, g, b, a;
                if (state == 1) { r=1.0f; g=0.84f; b=0.0f; a=0.4f; } // Yang (gold)
                else { r=0.88f; g=0.88f; b=0.88f; a=0.4f; } // Yin (silver)

                drawTrapezoid(vertexConsumer, pose, xInner, zInner, xOuter, zOuter, nextXOuter, nextZOuter, nextXInner, nextZInner, r, g, b, a, light);
            }

            // B. Grid Lines
            float lr=1.0f, lg=0.9f, lb=0.5f, la=0.8f;
            drawThickLine(vertexConsumer, pose, xInner, 0, zInner, xOuter, 0, zOuter, lr, lg, lb, la, light);
            drawThickLine(vertexConsumer, pose, xInner, 0, zInner, nextXInner, 0, nextZInner, lr, lg, lb, la, light);
            drawThickLine(vertexConsumer, pose, xOuter, 0, zOuter, nextXOuter, 0, nextZOuter, lr, lg, lb, la, light);

            // C. Trigrams (Corrected Rotation Logic)
            float centerAngleDeg = angleDeg + 22.5f;
            float centerAngleRad = (float) Math.toRadians(centerAngleDeg);
            float trigramDist = (innerRadius + outerRadius) / 2.0f;
            float tx = (float) Math.sin(centerAngleRad) * trigramDist;
            float tz = (float) -Math.cos(centerAngleRad) * trigramDist;

            poseStack.pushPose();
            poseStack.translate(tx, 0.015f, tz);
            
            // 180 - Angle ensures they always face OUTWARD
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - centerAngleDeg));
            
            float scale = entityWidth * 0.5f;
            poseStack.scale(scale, 1.0f, scale);

            int trigramColor = (state == 1) ? 0xFFFFD700 : (state == 0 ? 0xFFE0E0E0 : 0xFFCCCCCC);
            renderTrigram(poseStack.last().pose(), vertexConsumer, TRIGRAMS[i], trigramColor, light);
            poseStack.popPose();
        }
    }

    private static int getGateState(YinYangGateStateSyncPacket.YYGateData gateData, int compassIndex) {
        int yangGate = gateData.getYangGate();
        int yinGate = gateData.getYinGate();

        // 1 = Yang gate (gold), 0 = Yin gate (silver), 2 = No gate
        if (compassIndex == yangGate) {
            return 1; // Yang gate
        } else if (compassIndex == yinGate) {
            return 0; // Yin gate
        }
        return 2; // No gate at this direction
    }

    // --- GEOMETRY HELPERS ---

    private static void drawTrapezoid(VertexConsumer vc, Matrix4f pose, float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4, float r, float g, float b, float a, int light) {
        vc.vertex(pose, x1, 0, z1).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2, 0, z2).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x3, 0, z3).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x4, 0, z4).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
    }

    private static void drawThickLine(VertexConsumer vc, Matrix4f pose, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a, int light) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;
        float px = (-dz / len) * (LINE_THICKNESS / 2);
        float pz = (dx / len) * (LINE_THICKNESS / 2);
        vc.vertex(pose, x1+px, y1, z1+pz).color(r, g, b, a).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(pose, x1-px, y1, z1-pz).color(r, g, b, a).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(pose, x2-px, y2, z2-pz).color(r, g, b, a).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(pose, x2+px, y2, z2+pz).color(r, g, b, a).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
    }

    private static void renderTrigram(Matrix4f pose, VertexConsumer vc, int[] lines, int colorHex, int light) {
        float r = ((colorHex >> 16) & 0xFF) / 255f;
        float g = ((colorHex >> 8) & 0xFF) / 255f;
        float b = (colorHex & 0xFF) / 255f;
        float width = 0.6f;
        float height = 0.12f;
        float gap = 0.06f;
        float breakGap = 0.1f;
        for (int i = 0; i < 3; i++) {
            float zOffset = (i - 1) * (height + gap);
            if (lines[i] == 1) {
                drawQuadXY(vc, pose, -width / 2, zOffset, width, height, r, g, b, 0.9f, light);
            } else {
                float segWidth = (width - breakGap) / 2;
                drawQuadXY(vc, pose, -width / 2, zOffset, segWidth, height, r, g, b, 0.9f, light);
                drawQuadXY(vc, pose, breakGap / 2, zOffset, segWidth, height, r, g, b, 0.9f, light);
            }
        }
    }

    private static void drawQuadXY(VertexConsumer vc, Matrix4f pose, float x, float z, float w, float h, float r, float g, float b, float a, int light) {
        float x2 = x + w;
        float z2 = z + h;
        vc.vertex(pose, x, 0, z).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x, 0, z2).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2, 0, z2).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, x2, 0, z).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
    }

    /**
     * Render Exposed gates (Eight Formation Battle Array Ultimate).
     * All 8 gates are active simultaneously - 4 Yang (gold) and 4 Yin (silver).
     * Completed gates are shown with reduced opacity.
     * Inner octagon fill shows the next required gate type.
     */
    private static void renderExposedGatesForPlayer(LivingEntity entity, UUID playerUuid,
                                                      PoseStack poseStack, MultiBufferSource buffer, int light) {
        ExposedStateSyncPacket.ExposedData exposedData = ExposedStateSyncPacket.ClientExposedData.getExposedData(entity.getId(), playerUuid);
        if (exposedData == null) return;

        // Check if expired - don't render if expiration time has passed
        long currentTime = Minecraft.getInstance().level.getGameTime();
        if (currentTime >= exposedData.getExpirationTick()) {
            // Remove expired data and don't render
            ExposedStateSyncPacket.ClientExposedData.removePlayerExposedData(entity.getId(), playerUuid);
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        float entityWidth = Math.max(entity.getBbWidth(), 0.8f);
        float outerRadius = entityWidth * OUTER_RADIUS_MULT;
        float innerRadius = outerRadius * INNER_RADIUS_MULT;

        // Draw inner octagon fill to indicate next required gate type
        int nextRequired = exposedData.getNextRequired();
        float innerFillR, innerFillG, innerFillB, innerFillA;
        if (nextRequired == 0) {
            innerFillR = 1.0f; innerFillG = 0.84f; innerFillB = 0.0f; innerFillA = 0.3f; // Gold for Yang
        } else {
            innerFillR = 0.88f; innerFillG = 0.88f; innerFillB = 0.88f; innerFillA = 0.3f; // Silver for Yin
        }
        // Draw filled octagon for center area
        for (int oct = 0; oct < 8; oct++) {
            float octAngle1 = (float) Math.toRadians(oct * 45.0f);
            float octAngle2 = (float) Math.toRadians((oct + 1) * 45.0f);
            float ox1 = (float) Math.sin(octAngle1) * innerRadius;
            float oz1 = (float) -Math.cos(octAngle1) * innerRadius;
            float ox2 = (float) Math.sin(octAngle2) * innerRadius;
            float oz2 = (float) -Math.cos(octAngle2) * innerRadius;
            drawTrapezoid(vertexConsumer, pose, 0, 0, ox1, oz1, ox2, oz2, 0, 0, innerFillR, innerFillG, innerFillB, innerFillA, 15728880);
        }

        // Render all 8 gates
        for (int i = 0; i < 8; i++) {
            float angleDeg = i * 45.0f;
            float angleRad = (float) Math.toRadians(angleDeg);
            float nextAngleRad = (float) Math.toRadians((i + 1) * 45.0f);

            float sin = (float) Math.sin(angleRad);
            float cos = (float) -Math.cos(angleRad);
            float nextSin = (float) Math.sin(nextAngleRad);
            float nextCos = (float) -Math.cos(nextAngleRad);

            float xInner = sin * innerRadius; float zInner = cos * innerRadius;
            float xOuter = sin * outerRadius; float zOuter = cos * outerRadius;
            float nextXInner = nextSin * innerRadius; float nextZInner = nextCos * innerRadius;
            float nextXOuter = nextSin * outerRadius; float nextZOuter = nextCos * outerRadius;

            // Get gate type (0 = Yang/gold, 1 = Yin/silver) from Exposed data
            int gateType = exposedData.getGateTypeAtDirection(i);
            boolean isCompleted = exposedData.isGateCompleted(i);

            // Gate color and opacity based on type and completion
            float r, g, b, a;
            if (gateType == 0) { // Yang (gold)
                r = 1.0f; g = 0.84f; b = 0.0f;
            } else { // Yin (silver)
                r = 0.88f; g = 0.88f; b = 0.88f;
            }

            // Completed gates have lower opacity
            a = isCompleted ? 0.15f : 0.5f;

            // Draw filled trapezoid for the gate
            drawTrapezoid(vertexConsumer, pose, xInner, zInner, xOuter, zOuter, nextXOuter, nextZOuter, nextXInner, nextZInner, r, g, b, a, light);

            // Draw grid lines
            float lr = 1.0f, lg = 0.9f, lb = 0.5f, la = isCompleted ? 0.3f : 0.8f;
            drawThickLine(vertexConsumer, pose, xInner, 0, zInner, xOuter, 0, zOuter, lr, lg, lb, la, light);
            drawThickLine(vertexConsumer, pose, xInner, 0, zInner, nextXInner, 0, nextZInner, lr, lg, lb, la, light);
            drawThickLine(vertexConsumer, pose, xOuter, 0, zOuter, nextXOuter, 0, nextZOuter, lr, lg, lb, la, light);

            // Draw trigram
            float centerAngleDeg = angleDeg + 22.5f;
            float centerAngleRad = (float) Math.toRadians(centerAngleDeg);
            float trigramDist = (innerRadius + outerRadius) / 2.0f;
            float tx = (float) Math.sin(centerAngleRad) * trigramDist;
            float tz = (float) -Math.cos(centerAngleRad) * trigramDist;

            poseStack.pushPose();
            poseStack.translate(tx, 0.015f, tz);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - centerAngleDeg));

            float scale = entityWidth * 0.5f;
            poseStack.scale(scale, 1.0f, scale);

            int trigramColor = (gateType == 0) ? 0xFFFFD700 : 0xFFE0E0E0;
            int trigramLines[] = TRIGRAMS[i];
            renderTrigram(poseStack.last().pose(), vertexConsumer, trigramLines, trigramColor, light);
            poseStack.popPose();
        }
    }
}
