package com.complextalents.targeting.client;

import com.complextalents.skill.Skill;
import com.complextalents.skill.client.ChannelManager;
import com.complextalents.targeting.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side preview system for targeting visuals while channeling.
 *
 * <p>Renders different indicators based on targeting type:</p>
 * <ul>
 *   <li><b>ENTITY:</b> Box outline around targeted entity</li>
 *   <li><b>POSITION:</b> Crosshair and circle at ground position</li>
 *   <li><b>DIRECTION:</b> Direction arrow with range circle</li>
 * </ul>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>Only renders when actively channeling a skill</li>
 *   <li>Uses the channeling skill's targeting configuration (range, allowed types)</li>
 *   <li>Updates in real-time as the player moves their crosshair</li>
 *   <li>Color indicates ally (green) vs enemy (red) for entity targeting</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ClientTargetingPreview {

    private static final Minecraft MC = Minecraft.getInstance();
    private static TargetingSnapshot snapshot;
    private static TargetType currentTargetingType;

    private ClientTargetingPreview() {}

    /**
     * Update the targeting preview based on the channeling skill's configuration.
     * Only updates when actively channeling a skill.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Only render preview when channeling
        if (!ChannelManager.isChanneling()) {
            snapshot = null;
            currentTargetingType = null;
            return;
        }

        Player player = MC.player;
        if (player == null) {
            snapshot = null;
            currentTargetingType = null;
            return;
        }

        // Get the skill currently being channeled
        Skill skill = ChannelManager.getCurrentChannelingSkill();
        if (skill == null) {
            snapshot = null;
            currentTargetingType = null;
            return;
        }

        currentTargetingType = skill.getTargetingType();

        // Build targeting request using skill's configuration
        TargetingRequest.Builder requestBuilder = TargetingRequest.builder(player)
                .maxRange(skill.getMaxRange());

        // Set allowed types based on skill's targeting type
        switch (currentTargetingType) {
            case NONE -> {
                // No preview for self-target skills
                snapshot = null;
                currentTargetingType = null;
                return;
            }
            case DIRECTION -> {
                requestBuilder.allowedTypes(TargetType.DIRECTION, TargetType.POSITION);
                break;
            }
            case POSITION -> {
                requestBuilder.allowedTypes(TargetType.POSITION);
                break;
            }
            case ENTITY -> {
                requestBuilder.allowedTypes(TargetType.ENTITY, TargetType.POSITION)
                        .allowTargetSelf(skill.allowsSelfTarget())
                        .targetAllyOnly(skill.targetsAllyOnly())
                        .targetPlayerOnly(skill.targetsPlayerOnly());
                break;
            }
        }

        snapshot = ClientTargetingResolver.getInstance().resolve(requestBuilder.build());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return;
        }
        if (snapshot == null || currentTargetingType == null) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();

        MultiBufferSource.BufferSource buffer =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        // Render based on targeting type
        switch (currentTargetingType) {
            case ENTITY -> renderEntityReticle(pose, buffer);
            case POSITION -> renderPositionReticle(pose, buffer);
            case DIRECTION -> renderDirectionReticle(pose, buffer);
            case NONE -> {
                // No preview for self-target skills
            }
        }

        pose.popPose();
        buffer.endBatch();
    }

    /**
     * Render reticle for entity targeting.
     * Shows a box outline around the targeted entity.
     * Color indicates ally (green) or enemy (red).
     */
    private static void renderEntityReticle(PoseStack pose, MultiBufferSource buffer) {
        if (!snapshot.hasEntity()) {
            // No entity targeted, fall back to position reticle
            renderPositionReticle(pose, buffer);
            return;
        }

        Entity entity = MC.player.level().getEntity(snapshot.getTargetEntityId());
        if (entity == null || !entity.isAlive()) {
            return;
        }

        // Determine color based on ally status
        float r = snapshot.isAlly() ? 0.0f : 1.0f;
        float g = snapshot.isAlly() ? 1.0f : 0.0f;
        float b = 0.0f;

        // Get entity bounding box
        Vec3 pos = snapshot.getTargetPosition();
        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;

        // Entity dimensions (use approximate sizes)
        float width = entity.getBbWidth() * 0.5f + 0.2f;
        float height = entity.getBbHeight() + 0.1f;
        float eyeY = (float) entity.getEyeY();

        VertexConsumer vc = buffer.getBuffer(RenderType.lines());

        // Draw 3D box around entity
        // Bottom rectangle
        box(vc, pose, x - width, y, z - width, x + width, y + height, z + width, r, g, b, 0.5f);

        // Draw "X" on top
        line(vc, pose, x - width * 0.7f, eyeY + 0.3f, z - width * 0.7f,
                x + width * 0.7f, eyeY + 0.3f, z + width * 0.7f, r, g, b, 0.7f);
        line(vc, pose, x + width * 0.7f, eyeY + 0.3f, z - width * 0.7f,
                x - width * 0.7f, eyeY + 0.3f, z + width * 0.7f, r, g, b, 0.7f);
    }

    /**
     * Render reticle for position targeting.
     * Shows a crosshair and circle at the target position on the ground.
     */
    private static void renderPositionReticle(PoseStack pose, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Vec3 p = snapshot.getTargetPosition();

        float x = (float) p.x;
        float y = (float) p.y + 0.01f; // prevent z-fighting
        float z = (float) p.z;
        float size = 0.7f;

        // Cross
        line(vc, pose, x - size, y, z, x + size, y, z, 1, 1, 1, 0.7f);
        line(vc, pose, x, y, z - size, x, y, z + size, 1, 1, 1, 0.7f);

        // Circle outline
        circle(vc, pose, x, y, z, size * 1.5f, 24, 1, 1, 1, 0.5f);

        // Diagonal fills
        line(vc, pose, x - size * 0.5f, y, z - size * 0.5f, x + size * 0.5f, y, z + size * 0.5f, 1, 1, 1, 0.3f);
        line(vc, pose, x + size * 0.5f, y, z - size * 0.5f, x - size * 0.5f, y, z + size * 0.5f, 1, 1, 1, 0.3f);
    }

    /**
     * Render reticle for direction targeting.
     * Shows a crosshair at target point with a range circle at player position.
     */
    private static void renderDirectionReticle(PoseStack pose, MultiBufferSource buffer) {
        Vec3 p = snapshot.getTargetPosition();

        float x = (float) p.x;
        float y = (float) p.y + 0.01f;
        float z = (float) p.z;
        float size = 0.7f;

        // Small crosshair at target position
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        line(vc, pose, x - size, y, z, x + size, y, z, 1, 0.8f, 0.4f, 0.6f);
        line(vc, pose, x, y, z - size, x, y, z + size, 1, 0.8f, 0.4f, 0.6f);

        // Small circle
        circle(vc, pose, x, y, z, size, 12, 1, 0.8f, 0.4f, 0.4f);
    }
    /* ================= HELPERS ================= */

    /**
     * Draw a 3D box outline.
     */
    private static void box(VertexConsumer vc, PoseStack pose,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float r, float g, float b, float a) {
        // Bottom face
        line(vc, pose, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(vc, pose, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(vc, pose, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(vc, pose, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top face
        line(vc, pose, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(vc, pose, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(vc, pose, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(vc, pose, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical edges
        line(vc, pose, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(vc, pose, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(vc, pose, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(vc, pose, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

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
                               float radius, int segments,
                               float r, float g, float b, float a) {
        float step = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;

            line(vc, pose,
                    x + (float) Math.cos(a1) * radius, y, z + (float) Math.sin(a1) * radius,
                    x + (float) Math.cos(a2) * radius, y, z + (float) Math.sin(a2) * radius,
                    r, g, b, a);
        }
    }
}
