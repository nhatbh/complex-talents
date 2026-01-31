package com.complextalents.impl.highpriest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side data container for holy energy beams.
 * <p>
 * Uses UUID-based identification with lazy entity resolution.
 * Beams are rendered as glowing golden energy tethers that pulse red on damage.
 * Uses additive blending with FULL_BRIGHT lightmap (240) for pure glow.
 * <p>
 * Key features for brightness:
 * - uv2(240) = FULL_BRIGHT, ignores world lighting
 * - Additive blending (GL_SRC_ALPHA, GL_ONE) adds color, never darkens
 * - Color-only write (depthMask=false) prevents depth darkening
 * - Inner white core prevents brown edges
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "complextalents", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HolyBeamClientData {

    /**
     * Store active beam info for rendering.
     * Key: Ordered Pair of UUIDs, Value: Beam data
     */
    private static final Map<com.mojang.datafixers.util.Pair<UUID, UUID>, Beam> BEAMS = new ConcurrentHashMap<>();

    /**
     * Information about a beam for rendering purposes.
     */
    public static class Beam {
        public final UUID sourceUUID;
        public final UUID targetUUID;

        // Lazily resolved entity IDs
        public int sourceEntityId = -1;
        public int targetEntityId = -1;

        // Visual state
        public int pulseTicks = 0;
        public int fadeOutTicks = -1; // -1 = active, >= 0 = fading out

        // Cached current positions for smooth interpolation
        public Vec3 lastSourcePos;
        public Vec3 lastTargetPos;

        // Maximum range for this tether - beam doesn't render if entities are farther apart
        public double range;

        public Beam(UUID sourceUUID, UUID targetUUID) {
            this.sourceUUID = sourceUUID;
            this.targetUUID = targetUUID;
        }
    }

    /**
     * Create an ordered key from two UUIDs.
     * Order-independent for consistent lookup regardless of packet order.
     */
    public static com.mojang.datafixers.util.Pair<UUID, UUID> key(UUID a, UUID b) {
        return a.compareTo(b) <= 0
            ? com.mojang.datafixers.util.Pair.of(a, b)
            : com.mojang.datafixers.util.Pair.of(b, a);
    }

    /**
     * Handle activate beam packet - create or refresh a beam.
     */
    public static void handleActivate(UUID sourceUUID, UUID targetUUID, double range) {
        var beamKey = key(sourceUUID, targetUUID);
        var existing = BEAMS.get(beamKey);

        if (existing != null) {
            // Refresh existing beam - cancel any fade out and update range
            existing.fadeOutTicks = -1;
            existing.range = range;
        } else {
            // Create new beam
            var beam = new Beam(sourceUUID, targetUUID);
            beam.range = range;
            BEAMS.put(beamKey, beam);
        }
    }

    /**
     * Handle pulse beam packet - trigger red damage pulse.
     */
    public static void handlePulse(UUID sourceUUID, UUID targetUUID) {
        var beam = BEAMS.get(key(sourceUUID, targetUUID));
        if (beam != null) {
            beam.pulseTicks = 12;
        }
    }

    /**
     * Handle deactivate beam packet - trigger fade out.
     */
    public static void handleDeactivate(UUID sourceUUID, UUID targetUUID) {
        var beam = BEAMS.get(key(sourceUUID, targetUUID));
        if (beam != null && beam.fadeOutTicks == -1) {
            beam.fadeOutTicks = 10;
        }
    }

    /**
     * Find an entity by UUID in the level.
     * Uses a large search area around the player to find entities.
     */
    @Nullable
    private static Entity findEntityByUUID(net.minecraft.world.level.Level level, UUID uuid) {
        // Try using a large bounding box search
        // This is less efficient than direct access but works on client side
        Vec3 center = Minecraft.getInstance().player != null
            ? Minecraft.getInstance().player.position()
            : Vec3.ZERO;
        AABB searchBox = new AABB(center, center).inflate(128); // 128 block search radius

        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Client tick handler - resolves entities and updates beam state.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Iterator<Beam> it = BEAMS.values().iterator();
        while (it.hasNext()) {
            Beam beam = it.next();

            // Resolve source entity
            if (beam.sourceEntityId == -1) {
                Entity e = findEntityByUUID(level, beam.sourceUUID);
                if (e != null) {
                    beam.sourceEntityId = e.getId();
                }
            }

            // Resolve target entity
            if (beam.targetEntityId == -1) {
                Entity e = findEntityByUUID(level, beam.targetUUID);
                if (e != null) {
                    beam.targetEntityId = e.getId();
                }
            }

            // Pulse decay
            if (beam.pulseTicks > 0) {
                beam.pulseTicks--;
            }

            // Fade-out logic
            if (beam.fadeOutTicks >= 0 && --beam.fadeOutTicks <= 0) {
                it.remove();
                continue;
            }

            // Safety: entity vanished after resolve
            if (beam.sourceEntityId != -1 && beam.targetEntityId != -1) {
                if (level.getEntity(beam.sourceEntityId) == null ||
                    level.getEntity(beam.targetEntityId) == null) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Render event handler - renders all active holy beams.
     */
    @SubscribeEvent
    public static void renderBeams(RenderLevelStageEvent event) {
        // Render every tick for smooth tracking
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (BEAMS.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Setup render state for glowing beam
        setupGlowRenderState();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        for (Beam beam : BEAMS.values()) {
            renderBeam(buffer, poseStack, beam, level, event.getPartialTick());
        }

        BufferUploader.drawWithShader(buffer.end());

        // Restore render state
        restoreRenderState();

        poseStack.popPose();
    }

    /**
     * Setup render state for additive glow effect with full brightness.
     */
    private static void setupGlowRenderState() {
        // Additive blending - adds color to framebuffer, never darkens
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // Color-only write (no depth) prevents darkening from nearby geometry
        RenderSystem.depthMask(false);

        // Disable backface culling so beam is visible from all angles
        RenderSystem.disableCull();

        // LEQUAL depth test ensures proper layering
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    /**
     * Restore render state to defaults.
     */
    private static void restoreRenderState() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    /**
     * Render a single holy beam between two entities.
     * Renders an outer glow beam and inner white core for maximum brightness.
     */
    private static void renderBeam(BufferBuilder buffer, PoseStack poseStack, Beam beam, net.minecraft.world.level.Level level, float partialTick) {
        if (beam.sourceEntityId == -1 || beam.targetEntityId == -1) {
            return;
        }

        Entity from = level.getEntity(beam.sourceEntityId);
        Entity to = level.getEntity(beam.targetEntityId);
        if (from == null || to == null) {
            return;
        }

        // Use chest level (50% height) to connect at center mass
        double fromHeight = from.getBbHeight() * 0.5;
        double toHeight = to.getBbHeight() * 0.5;

        // Get interpolated positions for smooth rendering
        Vec3 start = from.getPosition(partialTick).add(0, fromHeight, 0);
        Vec3 endPos = to.getPosition(partialTick).add(0, toHeight, 0);

        // Check if entities are within range - don't render if out of range
        double distance = start.distanceTo(endPos);
        if (distance > beam.range) {
            return;
        }

        Vec3 dir = endPos.subtract(start).normalize();

        float time = level.getGameTime() + partialTick;

        // Calculate pulse (0.0 = no pulse, 1.0 = full pulse)
        float pulse = beam.pulseTicks > 0
            ? (float) Math.sin(time * 0.8f) * 0.5f + 0.5f
            : 0f;

        // Calculate fade (1.0 = full opacity, 0.0 = invisible)
        float fade = beam.fadeOutTicks >= 0
            ? beam.fadeOutTicks / 10f
            : 1f;

        // Full bright lightmap value (240 = LightTexture.FULL_BRIGHT)
        // This makes the beam ignore world lighting completely
        int fullBright = LightTexture.FULL_BRIGHT;

        Matrix4f mat = poseStack.last().pose();

        // Outer glow beam - golden to red on pulse
        // Colors biased toward white to avoid brown appearance
        float outerWidth = 0.08f + pulse * 0.04f;
        float outerR = 1.0f;
        float outerG = Mth.lerp(pulse, 0.95f, 0.3f);
        float outerB = Mth.lerp(pulse, 0.6f, 0.2f);
        float outerA = 0.8f * fade;

        // Inner white core - prevents brown edges, adds extra glow
        // This is the key to avoiding the muddy look
        float innerWidth = outerWidth * 0.4f;
        float innerR = 1.0f;
        float innerG = Mth.lerp(pulse, 0.98f, 0.5f);
        float innerB = Mth.lerp(pulse, 0.9f, 0.4f);
        float innerA = 1.0f * fade;

        // Build orthonormal basis for the beam
        Vec3 perp1 = findPerpendicular(dir).normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();

        // Draw outer beam (4 quads for square rod, visible from all angles)
        for (int i = 0; i < 4; i++) {
            double angle1 = i * Math.PI / 2;
            double angle2 = (i + 1) * Math.PI / 2;
            Vec3 offset1 = perp1.scale(Math.cos(angle1)).add(perp2.scale(Math.sin(angle1))).scale(outerWidth);
            Vec3 offset2 = perp1.scale(Math.cos(angle2)).add(perp2.scale(Math.sin(angle2))).scale(outerWidth);

            drawQuadFullBright(buffer, mat, start, endPos, offset1, offset2,
                    outerR, outerG, outerB, outerA, fullBright);
        }

        // Draw inner white core (4 quads, thinner)
        // The white core prevents color corruption from lighting
        for (int i = 0; i < 4; i++) {
            double angle1 = i * Math.PI / 2;
            double angle2 = (i + 1) * Math.PI / 2;
            Vec3 offset1 = perp1.scale(Math.cos(angle1)).add(perp2.scale(Math.sin(angle1))).scale(innerWidth);
            Vec3 offset2 = perp1.scale(Math.cos(angle2)).add(perp2.scale(Math.sin(angle2))).scale(innerWidth);

            drawQuadFullBright(buffer, mat, start, endPos, offset1, offset2,
                    innerR, innerG, innerB, innerA, fullBright);
        }
    }

    /**
     * Find a perpendicular vector to the given direction.
     */
    private static Vec3 findPerpendicular(Vec3 v) {
        if (Math.abs(v.x) < 0.1) {
            return new Vec3(1, 0, 0);
        }
        return new Vec3(0, 1, 0);
    }

    /**
     * Draw a quad between two points with FULL_BRIGHT lightmap.
     * Uses uv2(240) to ignore world lighting and always render at full brightness.
     */
    private static void drawQuadFullBright(BufferBuilder buffer, Matrix4f mat, Vec3 start, Vec3 end,
                                           Vec3 offset1, Vec3 offset2, float r, float g, float b, float a, int lightmap) {
        Vec3 v1 = start.add(offset1);
        Vec3 v2 = start.add(offset2);
        Vec3 v3 = end.add(offset2);
        Vec3 v4 = end.add(offset1);

        // Vertex 1 - position, color, texture coords (unused), lightmap (240 = FULL_BRIGHT)
        buffer.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(lightmap)
                .endVertex();

        // Vertex 2
        buffer.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(lightmap)
                .endVertex();

        // Vertex 3
        buffer.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(lightmap)
                .endVertex();

        // Vertex 4
        buffer.vertex(mat, (float) v4.x, (float) v4.y, (float) v4.z)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(lightmap)
                .endVertex();
    }

    /**
     * Clean up all beams (e.g., on world unload).
     */
    public static void cleanup() {
        BEAMS.clear();
    }
}
