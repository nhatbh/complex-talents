package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.elemental.ElementType;
import com.complextalents.util.IronParticleHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class ElementFXRenderer {

    public static void play(Level level, Vec3 pos, ElementType element, int stackCount) {
        spawnParticles(level, pos, element, stackCount);
        playSound(level, pos, element, stackCount);
    }

    private static void spawnParticles(Level level, Vec3 pos, ElementType element, int stackCount) {
        // Use element-specific particle patterns for more variety
        switch (element) {
            case FIRE -> spawnFireParticles(level, pos, stackCount);
            case AQUA -> spawnAquaParticles(level, pos, stackCount);
            case ICE -> spawnIceParticles(level, pos, stackCount);
            case LIGHTNING -> spawnLightningParticles(level, pos, stackCount);
            case NATURE -> spawnNatureParticles(level, pos, stackCount);
            case ENDER -> spawnEnderParticles(level, pos, stackCount);
        }
    }

    private static void playSound(Level level, Vec3 pos, ElementType element, int stackCount) {
        float volume = 0.2f; // Louder for higher stacks
        float pitch = 1.0f + (stackCount * 0.15f); // Higher pitch for higher stacks

        switch (element) {
            case FIRE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                volume, pitch, false);
            case AQUA -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                volume, pitch, false);
            case ICE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                volume, pitch + 0.3f, false);
            case LIGHTNING -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                volume * 0.5f, pitch, false);
            case NATURE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS,
                volume, pitch, false);
            case ENDER -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                volume * 0.7f, pitch + 0.5f, false);
        }
    }

    private static void spawnFireParticles(Level level, Vec3 pos, int stackCount) {
        // Rising flames and embers
        ParticleOptions fireParticle = IronParticleHelper.getIronParticle("fire");
        int particleCount = 3;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(fireParticle,
                pos.x + offsetX, pos.y + 0.2, pos.z + offsetZ,
                0, 0.15 + (stackCount * 0.02), 0);
        }
    }

    private static void spawnAquaParticles(Level level, Vec3 pos, int stackCount) {
        // Bubbling water effect
        int particleCount = 15;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = 2*(level.random.nextDouble() - 0.5) * 0.7;
            double offsetZ = 2*(level.random.nextDouble() - 0.5) * 0.7;
            level.addParticle(ParticleTypes.SPLASH,
                pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                0, 0.12 + (stackCount * 0.015), 0);
        }
    }

    private static void spawnIceParticles(Level level, Vec3 pos, int stackCount) {
        // Swirling snowflakes
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");
        int particleCount = 3;
        double radius = 0.7 + (stackCount * 0.1);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.05);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double height = level.random.nextDouble() * (1.0 + stackCount * 0.2);
            level.addParticle(snowflakeParticle,
                pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                -Math.cos(angle) * 0.03, -0.05, -Math.sin(angle) * 0.03);
        }
    }

    private static void spawnLightningParticles(Level level, Vec3 pos, int stackCount) {
        // Electric arcs spiraling upward
        ParticleOptions electricityParticle = IronParticleHelper.getIronParticle("electricity");
        int particleCount = 3;
        double height = 1.5 + (stackCount * 0.2);
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double) particleCount;
            double angle = t * Math.PI * 4.0; // 2 full rotations
            double spiralRadius = 0.4 * (1.0 - t * 0.5);
            double offsetX = Math.cos(angle) * spiralRadius;
            double offsetZ = Math.sin(angle) * spiralRadius;
            double offsetY = t * height;
            level.addParticle(electricityParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0.1, 0);
        }
    }

    private static void spawnNatureParticles(Level level, Vec3 pos, int stackCount) {
        // Floating fireflies orbiting
        ParticleOptions acidBubbleParticle = IronParticleHelper.getIronParticle("acid_bubble");
        int particleCount = 3;
        double radius = 0.8 + (stackCount * 0.1);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.08);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = 0.3 + Math.sin(level.getGameTime() * 0.1 + i) * 0.3;
            level.addParticle(acidBubbleParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                -Math.sin(angle) * 0.05, 0.02, Math.cos(angle) * 0.05);
        }
    }

    private static void spawnEnderParticles(Level level, Vec3 pos, int stackCount) {
        // Unstable void particles pulsing
        ParticleOptions unstableEnderParticle = IronParticleHelper.getIronParticle("unstable_ender");
        int particleCount = 7;
        double pulseRadius = 0.5 + Math.sin(level.getGameTime() * 0.15) * 0.3;
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double distance = level.random.nextDouble() * pulseRadius;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            double offsetY = level.random.nextDouble() * (1.0 + stackCount * 0.15);
            level.addParticle(unstableEnderParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                (level.random.nextDouble() - 0.5) * 0.15,
                (level.random.nextDouble() - 0.5) * 0.15,
                (level.random.nextDouble() - 0.5) * 0.15);
        }
    }
}
