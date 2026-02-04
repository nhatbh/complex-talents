package com.complextalents.network.yygm;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * Network packet for Sword Dance gate activation visualization.
 * Plays visual and audio effects when a gate is activated by Sword Dance.
 */
public class SwordDanceGateActivatePacket {
    private final int targetId;
    private final double angle;
    private final int gateType;
    private final int hitResult;

    // Hit result constants (matching YYGMGateHitEvent.HitResult)
    public static final int HIT_TRUE_GATE = 0;
    public static final int HIT_FALSE_GATE = 1;
    public static final int HIT_EMPTY_GATE = 2;

    // Gate type constants (matching YYGMGateHitEvent)
    public static final int GATE_YANG = 0;
    public static final int GATE_YIN = 1;
    public static final int GATE_NONE = -1;

    // Constructor for creating the packet
    public SwordDanceGateActivatePacket(int targetId, double angle, int gateType, int hitResult) {
        this.targetId = targetId;
        this.angle = angle;
        this.gateType = gateType;
        this.hitResult = hitResult;
    }

    // Decode constructor
    public SwordDanceGateActivatePacket(FriendlyByteBuf buffer) {
        this.targetId = buffer.readInt();
        this.angle = buffer.readDouble();
        this.gateType = buffer.readInt();
        this.hitResult = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(targetId);
        buffer.writeDouble(angle);
        buffer.writeInt(gateType);
        buffer.writeInt(hitResult);
    }

    public static SwordDanceGateActivatePacket decode(FriendlyByteBuf buffer) {
        return new SwordDanceGateActivatePacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        // Find the target entity
        net.minecraft.world.entity.Entity entity = level.getEntity(targetId);
        if (entity == null) {
            return;
        }

        Vec3 targetPos = entity.position();

        // Calculate gate position based on angle
        double distance = 1.5;
        double gateX = targetPos.x + Math.sin(angle) * distance;
        double gateZ = targetPos.z - Math.cos(angle) * distance;

        Vec3 gatePos = new Vec3(gateX, targetPos.y + 0.5, gateZ);

        // Render effect based on hit result
        switch (hitResult) {
            case HIT_TRUE_GATE -> renderTrueGateHit(level, gatePos, gateType);
            case HIT_FALSE_GATE -> renderFalseGateHit(level, gatePos);
            case HIT_EMPTY_GATE -> renderEmptyGateHit(level, gatePos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void renderTrueGateHit(Level level, Vec3 pos, int gateType) {
        Vector3f color = (gateType == GATE_YANG)
                ? new Vector3f(1.0f, 0.84f, 0.0f)  // Gold for Yang
                : new Vector3f(0.85f, 0.85f, 0.9f); // Silver for Yin

        DustParticleOptions dust = new DustParticleOptions(color, 1.2f);

        // Burst of particles
        int particleCount = 20 + level.random.nextInt(11);
        for (int i = 0; i < particleCount; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2.0 * level.random.nextDouble() - 1.0);
            double speed = 0.15 + level.random.nextDouble() * 0.25;

            double velX = Math.sin(phi) * Math.cos(theta) * speed;
            double velY = Math.sin(phi) * Math.sin(theta) * speed;
            double velZ = Math.cos(phi) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5 + 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(dust, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, velX, velY, velZ);
        }

        // Play metallic hit sound
        float pitch = (gateType == GATE_YANG) ? 1.2f : 1.5f;
        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.ANVIL_HIT, SoundSource.HOSTILE, 1.0f, pitch, false);
    }

    @OnlyIn(Dist.CLIENT)
    private void renderFalseGateHit(Level level, Vec3 pos) {
        // Red particles for wrong gate
        Vector3f redColor = new Vector3f(1.0f, 0.2f, 0.2f);
        DustParticleOptions redDust = new DustParticleOptions(redColor, 1.0f);

        int particleCount = 15 + level.random.nextInt(6);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + level.random.nextDouble() * 0.15;

            double velX = Math.cos(angle) * speed;
            double velY = 0.2 + level.random.nextDouble() * 0.3;
            double velZ = Math.sin(angle) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.4;

            level.addParticle(redDust, pos.x + offsetX, pos.y + 0.3, pos.z + offsetZ, velX, velY, velZ);
        }

        // Play shield break sound
        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.8f, 0.8f, false);
    }

    @OnlyIn(Dist.CLIENT)
    private void renderEmptyGateHit(Level level, Vec3 pos) {
        // Grey particles for empty gate
        Vector3f greyColor = new Vector3f(0.5f, 0.5f, 0.5f);
        DustParticleOptions greyDust = new DustParticleOptions(greyColor, 0.8f);

        int particleCount = 8 + level.random.nextInt(5);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.05 + level.random.nextDouble() * 0.1;

            double velX = Math.cos(angle) * speed;
            double velY = 0.1 + level.random.nextDouble() * 0.2;
            double velZ = Math.sin(angle) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(greyDust, pos.x + offsetX, pos.y + 0.3, pos.z + offsetZ, velX, velY, velZ);
        }
    }

    // Getters
    public int getTargetId() {
        return targetId;
    }

    public double getAngle() {
        return angle;
    }

    public int getGateType() {
        return gateType;
    }

    public int getHitResult() {
        return hitResult;
    }
}
