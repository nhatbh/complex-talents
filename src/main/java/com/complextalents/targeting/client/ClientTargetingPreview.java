package com.complextalents.targeting.client;

import com.complextalents.client.CombatModeClient;
import com.complextalents.targeting.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side preview system for targeting visuals.
 *
 * <p>Renders:</p>
 * <ul>
 *   <li>Entity highlights (outline/box around targeted entity)</li>
 *   <li>Ground reticles (position indicator on ground)</li>
 *   <li>Beams (line from player to target)</li>
 *   <li>Range indicators (sphere showing max range)</li>
 * </ul>
 *
 * <p>This class is client-only and never sends data to the server.
 * It consumes {@link TargetingSnapshot} instances produced by
 * {@link ClientTargetingResolver}.</p>
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ClientTargetingPreview {

    private static final Minecraft MC = Minecraft.getInstance();
    private static TargetingSnapshot snapshot;

    public static void updateSnapshot(TargetingSnapshot s) {
        snapshot = s;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = MC.player;
        if (player == null || !CombatModeClient.isCombatMode()) {
            snapshot = null;
            return;
        }

        snapshot = ClientTargetingResolver.getInstance().resolve(
                TargetingRequest.builder(player)
                        .maxRange(32)
                        .allowedTypes(TargetType.ENTITY, TargetType.POSITION)
                        .allowTargetSelf(true)
                        .build()
        );
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;
        if (snapshot == null) return;

        PoseStack pose = event.getPoseStack();
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();

        MultiBufferSource.BufferSource buffer =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        renderReticle(pose, buffer);

        pose.popPose();
        buffer.endBatch();
    }

    private static void renderReticle(PoseStack pose, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Vec3 p = snapshot.getTargetPosition();

        float x = (float) p.x;
        float y = (float) p.y + 0.01f; // prevent z-fighting
        float z = (float) p.z;
        float size = 0.4f;

        // Cross
        line(vc, pose, x - size, y, z, x + size, y, z, 1, 1, 1, 0.6f);
        line(vc, pose, x, y, z - size, x, y, z + size, 1, 1, 1, 0.6f);

        // Circle
        circle(vc, pose, x, y, z, size * 1.5f, 16);
    }

    /* ================= HELPERS ================= */

    private static void line(VertexConsumer vc, PoseStack pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        PoseStack.Pose p = pose.last();
        int packedLight = 0xF000F0; // Full brightness
        vc.vertex(p.pose(), x1, y1, z1)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(packedLight)
                .normal(p.normal(), 0, 1, 0)
                .endVertex();
        vc.vertex(p.pose(), x2, y2, z2)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(packedLight)
                .normal(p.normal(), 0, 1, 0)
                .endVertex();
    }

    private static void circle(VertexConsumer vc, PoseStack pose,
                               float x, float y, float z,
                               float radius, int segments) {
        float step = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;

            line(vc, pose,
                    x + (float) Math.cos(a1) * radius, y, z + (float) Math.sin(a1) * radius,
                    x + (float) Math.cos(a2) * radius, y, z + (float) Math.sin(a2) * radius,
                    1, 1, 1, 0.5f);
        }
    }
}
