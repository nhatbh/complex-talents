package com.complextalents.network.yygm;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * Network packet for spawning YYGM gate effects on the client.
 * <p>
 * Effect types:
 * - YANG_HIT: Gold DUST particle burst + metallic hit sound
 * - YIN_HIT: Silver DUST particle burst + metallic hit sound
 * - DISCORD: Purple particles + weak sound
 * - GATE_SPAWN: Small sparkle effect when gate spawns
 * </p>
 */
public class SpawnYinYangGateFXPacket {
    public enum EffectType {
        YANG_HIT,
        YIN_HIT,
        DISCORD,
        GATE_SPAWN
    }

    private final EffectType effectType;
    private final Vec3 position;

    // Constructor for creating the packet
    public SpawnYinYangGateFXPacket(Vec3 position, EffectType effectType) {
        this.position = position;
        this.effectType = effectType;
    }

    // Decode constructor
    public SpawnYinYangGateFXPacket(FriendlyByteBuf buffer) {
        this.effectType = buffer.readEnum(EffectType.class);
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(effectType);
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    public static SpawnYinYangGateFXPacket decode(FriendlyByteBuf buffer) {
        return new SpawnYinYangGateFXPacket(buffer);
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

        switch (effectType) {
            case YANG_HIT -> renderYangHit(level, position);
            case YIN_HIT -> renderYinHit(level, position);
            case DISCORD -> renderDiscord(level, position);
            case GATE_SPAWN -> renderGateSpawn(level, position);
        }
    }

    /**
     * Renders Yang gate hit - gold DUST particle burst with metallic sound.
     */
    @OnlyIn(Dist.CLIENT)
    private static void renderYangHit(Level level, Vec3 pos) {
        // Gold dust color
        Vector3f goldColor = new Vector3f(1.0f, 0.84f, 0.0f);
        DustParticleOptions goldDust = new DustParticleOptions(goldColor, 1.2f);

        int particleCount = 25 + level.random.nextInt(11);
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

            level.addParticle(goldDust, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, velX, velY, velZ);
        }

        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.ANVIL_HIT, SoundSource.HOSTILE, 1.0f, 1.2f, false);
    }

    /**
     * Renders Yin gate hit - silver DUST particle burst with metallic sound.
     */
    @OnlyIn(Dist.CLIENT)
    private static void renderYinHit(Level level, Vec3 pos) {
        // Silver dust color
        Vector3f silverColor = new Vector3f(0.85f, 0.85f, 0.9f);
        DustParticleOptions silverDust = new DustParticleOptions(silverColor, 1.2f);

        int particleCount = 25 + level.random.nextInt(11);
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

            level.addParticle(silverDust, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, velX, velY, velZ);
        }

        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.ANVIL_HIT, SoundSource.HOSTILE, 1.0f, 1.5f, false);
    }

    /**
     * Renders Discord effect - purple particles with weak sound.
     */
    @OnlyIn(Dist.CLIENT)
    private static void renderDiscord(Level level, Vec3 pos) {
        // Purple dust color
        Vector3f purpleColor = new Vector3f(0.6f, 0.1f, 0.8f);
        DustParticleOptions purpleDust = new DustParticleOptions(purpleColor, 1.0f);

        int particleCount = 15 + level.random.nextInt(11);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + level.random.nextDouble() * 0.15;

            double velX = Math.cos(angle) * speed;
            double velY = 0.2 + level.random.nextDouble() * 0.3;
            double velZ = Math.sin(angle) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.4;

            level.addParticle(purpleDust, pos.x + offsetX, pos.y + 0.3, pos.z + offsetZ, velX, velY, velZ);
        }

        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.8f, 0.8f, false);
    }

    /**
     * Renders gate spawn effect - small sparkle.
     */
    @OnlyIn(Dist.CLIENT)
    private static void renderGateSpawn(Level level, Vec3 pos) {
        // Small sparkle effect
        int particleCount = 5 + level.random.nextInt(4);
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.05 + level.random.nextDouble() * 0.1;

            double velX = Math.cos(angle) * speed;
            double velY = 0.1 + level.random.nextDouble() * 0.2;
            double velZ = Math.sin(angle) * speed;

            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x + offsetX, pos.y + 0.2, pos.z + offsetZ, velX, velY, velZ);
        }
    }

    // Getters
    public EffectType getEffectType() {
        return effectType;
    }

    public Vec3 getPosition() {
        return position;
    }
}
