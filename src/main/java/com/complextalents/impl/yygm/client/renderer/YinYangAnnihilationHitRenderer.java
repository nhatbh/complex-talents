package com.complextalents.impl.yygm.client.renderer;

import com.complextalents.TalentsMod;
import com.complextalents.network.yygm.YinYangAnnihilationHitPacket;
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

/**
 * Renderer for Yin Yang Annihilation hit effect.
 * Renders expanding Yin Yang rings for each attack hit on the target.
 * Multiple animations can stack (multiple rings visible at once).
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class YinYangAnnihilationHitRenderer {

    private static final ResourceLocation YINYANG_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/yygm/yinyang.png");

    /**
     * Render the expanding ring animations in the AFTER_TRANSLUCENT_BLOCKS stage.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Clean up finished animations
        YinYangAnnihilationHitPacket.ClientHitData.cleanupFinished(mc.level.getGameTime());

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Render all active hit animations for all entities
        for (Integer entityId : YinYangAnnihilationHitPacket.ClientHitData.getEntitiesWithHits()) {
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Calculate position
            double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
            double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
            double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

            Vec3 cameraPos = camera.getPosition();
            double dx = lerpX - cameraPos.x;
            double dy = lerpY - cameraPos.y;
            double dz = lerpZ - cameraPos.z;

            long currentTime = mc.level.getGameTime();

            // Render all hit animations for this entity (stacking)
            for (YinYangAnnihilationHitPacket.HitAnimationData hitData :
                    YinYangAnnihilationHitPacket.ClientHitData.getHits(entityId)) {

                poseStack.pushPose();
                poseStack.translate(dx, dy + 0.01, dz); // Just above ground

                // Get animation parameters
                float scale = hitData.getScale(currentTime);
                float alpha = hitData.getAlpha(currentTime);
                float rotation = hitData.getRotation(currentTime);

                // Apply rotation
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

                // Render the Yin Yang circle
                renderYinYangCircle(poseStack, bufferSource, scale, alpha);

                poseStack.popPose();
            }
        }

        bufferSource.endBatch(RenderType.entityTranslucent(YINYANG_TEXTURE));
    }

    /**
     * Render the Yin Yang circle with current scale and alpha.
     */
    private static void renderYinYangCircle(PoseStack poseStack, MultiBufferSource buffer,
                                             float scale, float alpha) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(YINYANG_TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int light = 15728880; // Full brightness

        // Scale is diameter, so halfSize is radius
        float radius = scale / 2.0f;

        // Draw quad with texture coordinates and alpha
        vc.vertex(pose, -radius, 0, -radius).color(1.0f, 1.0f, 1.0f, alpha).uv(0, 0)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, -radius, 0, radius).color(1.0f, 1.0f, 1.0f, alpha).uv(0, 1)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, radius, 0, radius).color(1.0f, 1.0f, 1.0f, alpha).uv(1, 1)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
        vc.vertex(pose, radius, 0, -radius).color(1.0f, 1.0f, 1.0f, alpha).uv(1, 0)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 1, 0).endVertex();
    }
}
