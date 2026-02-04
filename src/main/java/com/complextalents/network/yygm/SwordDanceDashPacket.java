package com.complextalents.network.yygm;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Network packet for Sword Dance dash visualization on the client.
 * Spawns purple trail particles as the player dashes.
 */
public class SwordDanceDashPacket {
    private final int playerId;
    private final Vec3 startPos;
    private final Vec3 endPos;
    private final int durationTicks;

    // Client-side tracking of active dashes
    private static final ConcurrentHashMap<Integer, DashData> ACTIVE_DASHES = new ConcurrentHashMap<>();

    public record DashData(int playerId, Vec3 startPos, Vec3 endPos, int durationTicks, int currentTick) {
        public DashData(int playerId, Vec3 startPos, Vec3 endPos, int durationTicks) {
            this(playerId, startPos, endPos, durationTicks, 0);
        }

        public DashData increment() {
            return new DashData(playerId, startPos, endPos, durationTicks, currentTick + 1);
        }

        public double getProgress() {
            return (double) currentTick / durationTicks;
        }

        public boolean isComplete() {
            return currentTick >= durationTicks;
        }

        public Vec3 getCurrentPosition() {
            double t = getProgress();
            // Accelerating curve: t^2 for smooth start
            double easedT = t * t;
            return startPos.add(endPos.subtract(startPos).scale(easedT));
        }
    }

    // Constructor for creating the packet
    public SwordDanceDashPacket(int playerId, Vec3 startPos, Vec3 endPos, int durationTicks) {
        this.playerId = playerId;
        this.startPos = startPos;
        this.endPos = endPos;
        this.durationTicks = durationTicks;
    }

    // Decode constructor
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
        // Store dash data for tick-based rendering
        ACTIVE_DASHES.put(playerId, new DashData(playerId, startPos, endPos, durationTicks));
    }

    /**
     * Client-side tick handler for dash rendering.
     * Call this from client tick event.
     */
    @OnlyIn(Dist.CLIENT)
    public static void clientTick() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        // Collect keys to iterate
        var keys = ACTIVE_DASHES.keySet().iterator();
        while (keys.hasNext()) {
            Integer key = keys.next();
            DashData dash = ACTIVE_DASHES.get(key);

            if (dash == null || dash.isComplete()) {
                keys.remove();
                continue;
            }

            // Increment and render
            DashData nextDash = dash.increment();
            ACTIVE_DASHES.put(key, nextDash);

            // Spawn trail particles
            renderDashTrail(level, nextDash);
        }
    }

    /**
     * Render purple/violet trail particles along the dash path.
     */
    @OnlyIn(Dist.CLIENT)
    private static void renderDashTrail(Level level, DashData dash) {
        // Purple/violet color for Sword Dance
        Vector3f purpleColor = new Vector3f(0.6f, 0.2f, 0.8f);
        DustParticleOptions purpleDust = new DustParticleOptions(purpleColor, 1.0f);

        Vec3 currentPos = dash.getCurrentPosition();

        // Spawn particles at current position
        int particleCount = 3 + level.random.nextInt(3);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.05 + level.random.nextDouble() * 0.1;

            double velX = Math.cos(angle) * speed;
            double velY = 0.1 + level.random.nextDouble() * 0.2;
            double velZ = Math.sin(angle) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(purpleDust,
                    currentPos.x + offsetX,
                    currentPos.y + 0.3,
                    currentPos.z + offsetZ,
                    velX, velY, velZ);
        }

        // Optional: Add a few particles trailing behind
        if (dash.currentTick > 0) {
            Vec3 behindPos = dash.startPos.add(
                    dash.endPos.subtract(dash.startPos)
                            .scale(Math.pow((dash.currentTick - 1.0) / dash.durationTicks, 2))
            );

            for (int i = 0; i < 2; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

                level.addParticle(purpleDust,
                        behindPos.x + offsetX,
                        behindPos.y + 0.3,
                        behindPos.z + offsetZ,
                        0, 0.05, 0);
            }
        }
    }

    /**
     * Clear all active dashes (call on world unload).
     */
    @OnlyIn(Dist.CLIENT)
    public static void clearAll() {
        ACTIVE_DASHES.clear();
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
}
