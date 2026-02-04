package com.complextalents.network.yygm;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.complextalents.TalentsMod;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Network packet for Sword Dance dash with client-side prediction.
 * Server sends dash parameters once, client interpolates smoothly per frame.
 */
public class SwordDanceDashPacket {
    private final int playerId;
    private final Vec3 startPos;
    private final Vec3 endPos;
    private final int durationTicks;

    public SwordDanceDashPacket(int playerId, Vec3 startPos, Vec3 endPos, int durationTicks) {
        this.playerId = playerId;
        this.startPos = startPos;
        this.endPos = endPos;
        this.durationTicks = durationTicks;
    }

    public SwordDanceDashPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readInt();
        this.startPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.endPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.durationTicks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(playerId);
        buffer.writeDouble(startPos.x);
        buffer.writeDouble(startPos.y);
        buffer.writeDouble(startPos.z);
        buffer.writeDouble(endPos.x);
        buffer.writeDouble(endPos.y);
        buffer.writeDouble(endPos.z);
        buffer.writeInt(durationTicks);
    }

    public static SwordDanceDashPacket decode(FriendlyByteBuf buffer) {
        return new SwordDanceDashPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        ClientDashHandler.startDash(playerId, startPos, endPos, durationTicks);
    }

    // Getters
    public int getPlayerId() {
        return playerId;
    }

    public Vec3 getStartPos() {
        return startPos;
    }

    public Vec3 getEndPos() {
        return endPos;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /**
     * Client-side dash handler with per-frame interpolation.
     * This creates smooth movement without the teleport-stutter effect.
     */
    @Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientDashHandler {
        private static final ConcurrentHashMap<Integer, ActiveDash> ACTIVE_DASHES = new ConcurrentHashMap<>();

        public static void startDash(int playerId, Vec3 startPos, Vec3 endPos, int durationTicks) {
            ACTIVE_DASHES.put(playerId, new ActiveDash(startPos, endPos, durationTicks));
        }

        /**
         * Track tick progress for timing and particle effects.
         */
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Level level = Minecraft.getInstance().level;
            if (level == null) {
                ACTIVE_DASHES.clear();
                return;
            }

            var keys = ACTIVE_DASHES.keySet().iterator();
            while (keys.hasNext()) {
                Integer key = keys.next();
                ActiveDash dash = ACTIVE_DASHES.get(key);

                if (dash == null || dash.isComplete()) {
                    keys.remove();
                    continue;
                }

                dash.tick++;
                renderDashParticles(level, dash);
            }
        }

        /**
         * Per-frame interpolation - THIS is where smoothness happens.
         * Uses partial tick for sub-tick movement.
         */
        @SubscribeEvent
        public static void onRenderTick(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            ActiveDash dash = ACTIVE_DASHES.get(player.getId());
            if (dash == null) return;

            // Get partial tick for smooth interpolation between ticks
            float partial = mc.getDeltaTracker().getRealtimeDeltaTicks();

            // Calculate progress with partial tick for sub-tick precision
            float t = (dash.tick + partial) / dash.durationTicks;
            t = Math.max(0, Math.min(1, t));

            // Smoothstep easing: 3t² - 2t³
            // This creates smooth acceleration and deceleration
            t = t * t * (3 - 2 * t);

            // Interpolate position
            Vec3 currentPos = dash.startPos.lerp(dash.endPos, t);

            // Set local player position for smooth visuals
            // This is client-side prediction only - server enforces final position
            player.setPos(currentPos.x, currentPos.y, currentPos.z);
        }

        private static void renderDashParticles(Level level, ActiveDash dash) {
            Vector3f purpleColor = new Vector3f(0.6f, 0.2f, 0.8f);
            DustParticleOptions purpleDust = new DustParticleOptions(purpleColor, 1.0f);

            // Get current interpolated position
            float t = (float) dash.tick / dash.durationTicks;
            t = Math.max(0, Math.min(1, t));
            t = t * t * (3 - 2 * t); // smoothstep

            Vec3 currentPos = dash.startPos.lerp(dash.endPos, t);

            // Spawn particles at current position
            int particleCount = 2 + level.random.nextInt(2);
            for (int i = 0; i < particleCount; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double speed = 0.05 + level.random.nextDouble() * 0.08;

                double velX = Math.cos(angle) * speed;
                double velY = 0.05 + level.random.nextDouble() * 0.15;
                double velZ = Math.sin(angle) * speed;

                double offsetX = (level.random.nextDouble() - 0.5) * 0.4;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.4;

                level.addParticle(purpleDust,
                        currentPos.x + offsetX,
                        currentPos.y + 0.3,
                        currentPos.z + offsetZ,
                        velX, velY, velZ);
            }
        }

        public static void clearAll() {
            ACTIVE_DASHES.clear();
        }
    }

    /**
     * Active dash state for client-side interpolation.
     */
    public static class ActiveDash {
        public final Vec3 startPos;
        public final Vec3 endPos;
        public final int durationTicks;
        public int tick = 0;

        public ActiveDash(Vec3 startPos, Vec3 endPos, int durationTicks) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.durationTicks = durationTicks;
        }

        public boolean isComplete() {
            return tick >= durationTicks;
        }

        public Vec3 getInterpolatedPos(float partialTicks) {
            float t = Math.min(1, (tick + partialTicks) / durationTicks);
            t = t * t * (3 - 2 * t); // smoothstep
            return startPos.lerp(endPos, t);
        }
    }
}
