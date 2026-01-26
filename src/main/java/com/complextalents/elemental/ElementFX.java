package com.complextalents.elemental;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.util.IronParticleHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ElementFX {

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
        float volume = 0.3f + (stackCount * 0.1f); // Louder for higher stacks
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
        int particleCount = 8 + (stackCount * 3);
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
        ParticleOptions acidBubbleParticle = IronParticleHelper.getIronParticle("acid_bubble");
        int particleCount = 6 + (stackCount * 2);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.7;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.7;
            level.addParticle(acidBubbleParticle,
                pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ,
                0, 0.12 + (stackCount * 0.015), 0);
        }
    }

    private static void spawnIceParticles(Level level, Vec3 pos, int stackCount) {
        // Swirling snowflakes
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");
        int particleCount = 10 + (stackCount * 3);
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
        int particleCount = 12 + (stackCount * 4);
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
        ParticleOptions fireflyParticle = IronParticleHelper.getIronParticle("firefly");
        int particleCount = 8 + (stackCount * 3);
        double radius = 0.8 + (stackCount * 0.1);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.08);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = 0.3 + Math.sin(level.getGameTime() * 0.1 + i) * 0.3;
            level.addParticle(fireflyParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                -Math.sin(angle) * 0.05, 0.02, Math.cos(angle) * 0.05);
        }
    }

    private static void spawnEnderParticles(Level level, Vec3 pos, int stackCount) {
        // Unstable void particles pulsing
        ParticleOptions unstableEnderParticle = IronParticleHelper.getIronParticle("unstable_ender");
        int particleCount = 10 + (stackCount * 4);
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

    // ===== Shared Reaction Particle Effects =====

    /**
     * Spawns a massive explosion burst effect with expanding rings.
     * This is the standard "reaction triggered" visual effect.
     *
     * @param level The server level
     * @param pos The center position
     * @param particle The particle type to spawn (or null for fallback)
     * @param fallbackParticle The fallback particle if primary is null
     */
    public static void spawnExplosionBurst(ServerLevel level, Vec3 pos,
                                          @Nullable ParticleOptions particle,
                                          ParticleOptions fallbackParticle) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions effectiveParticle = particle != null ? particle : fallbackParticle;
        var quality = ElementalReactionConfig.particleQuality.get();

        // Base particle count scaled by quality
        int baseParticleCount = 60;
        int particleCount = quality.scale(baseParticleCount);
        double radius = quality.scale(2.0);

        // Expanding ring effect
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = radius * (ring + 1) / 3.0;
            int ringParticles = particleCount / 3;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (i * Math.PI * 2.0) / ringParticles;
                double offsetX = Math.cos(angle) * ringRadius;
                double offsetZ = Math.sin(angle) * ringRadius;
                double offsetY = level.random.nextDouble() * 2.5;

                double velocityX = Math.cos(angle) * (0.4 + ring * 0.1);
                double velocityY = (level.random.nextDouble() - 0.3) * 0.4;
                double velocityZ = Math.sin(angle) * (0.4 + ring * 0.1);

                level.sendParticles(effectiveParticle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, velocityX, velocityY, velocityZ, 0.05);
            }
        }

        // Central explosion core
        int coreParticles = quality.scale(30);
        for (int i = 0; i < coreParticles; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            double velocityX = (level.random.nextDouble() - 0.5) * 0.6;
            double velocityY = level.random.nextDouble() * 0.8;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.6;

            level.sendParticles(effectiveParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, velocityX, velocityY, velocityZ, 0.05);
        }

        // Multiple flash effects
        int flashCount = quality.scale(5);
        for (int i = 0; i < flashCount; i++) {
            double flashOffset = i * 0.3;
            level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y + 0.5 + flashOffset, pos.z,
                1, 0, 0, 0, 0.0);
        }
    }

    /**
     * Spawns a spiral effect with the given particle.
     *
     * @param level The server level
     * @param pos The center position
     * @param particleName The Iron's Spellbooks particle name
     * @param fallbackParticle Fallback particle if Iron's particle not found
     * @param spiralHeight Total height of the spiral
     * @param spiralRadius Base radius of the spiral
     */
    public static void spawnSpiral(ServerLevel level, Vec3 pos,
                                  String particleName, ParticleOptions fallbackParticle,
                                  double spiralHeight, double spiralRadius) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = IronParticleHelper.getIronParticle(particleName);
        if (particle == null) particle = fallbackParticle;

        var quality = ElementalReactionConfig.particleQuality.get();
        int spiralParticles = quality.scale(30);

        for (int i = 0; i < spiralParticles; i++) {
            double angle = (i * Math.PI * 2.0) / spiralParticles;
            double height = i * (spiralHeight / spiralParticles);
            double currentRadius = spiralRadius * (1 - height / (spiralHeight * 1.5));

            level.sendParticles(particle,
                pos.x + Math.cos(angle) * currentRadius,
                pos.y + height,
                pos.z + Math.sin(angle) * currentRadius,
                1, 0, 0.25, 0, 0.02);
        }
    }

    /**
     * Spawns an upward ember effect.
     *
     * @param level The server level
     * @param pos The center position
     * @param count Base number of embers
     */
    public static void spawnEmbers(ServerLevel level, Vec3 pos, int count) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = IronParticleHelper.getIronParticle("ember");
        if (particle == null) particle = ParticleTypes.FLAME;

        var quality = ElementalReactionConfig.particleQuality.get();
        int scaledCount = quality.scale(count);

        for (int i = 0; i < scaledCount; i++) {
            level.sendParticles(particle,
                pos.x, pos.y + 0.2, pos.z,
                1,
                (level.random.nextDouble() - 0.5) * 0.5,
                level.random.nextDouble() * 0.8,
                (level.random.nextDouble() - 0.5) * 0.5,
                0.02);
        }
    }

    /**
     * Spawns a vortex/swirl effect.
     *
     * @param level The server level
     * @param pos The center position
     * @param particleName The Iron's Spellbooks particle name
     * @param fallbackParticle Fallback particle if Iron's particle not found
     * @param vortexHeight Total height of the vortex
     * @param baseRadius Starting radius at the bottom
     */
    public static void spawnVortex(ServerLevel level, Vec3 pos,
                                  String particleName, ParticleOptions fallbackParticle,
                                  double vortexHeight, double baseRadius) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = IronParticleHelper.getIronParticle(particleName);
        if (particle == null) particle = fallbackParticle;

        var quality = ElementalReactionConfig.particleQuality.get();
        int vortexParticles = quality.scale(50);

        for (int i = 0; i < vortexParticles; i++) {
            double angle = (i * Math.PI * 2.0) / vortexParticles;
            double height = i * (vortexHeight / vortexParticles);
            double currentRadius = baseRadius * (1 - height / vortexHeight);

            level.sendParticles(particle,
                pos.x + Math.cos(angle) * currentRadius,
                pos.y + height,
                pos.z + Math.sin(angle) * currentRadius,
                1, 0, 0.15, 0, 0.02);
        }
    }

    /**
     * Spawns a random burst of particles in a spread pattern.
     *
     * @param level The server level
     * @param pos The center position
     * @param particleName The Iron's Spellbooks particle name
     * @param fallbackParticle Fallback particle if Iron's particle not found
     * @param count Base number of particles
     * @param spread The spread radius
     */
    public static void spawnRandomBurst(ServerLevel level, Vec3 pos,
                                       String particleName, ParticleOptions fallbackParticle,
                                       int count, double spread) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = IronParticleHelper.getIronParticle(particleName);
        if (particle == null) particle = fallbackParticle;

        var quality = ElementalReactionConfig.particleQuality.get();
        int scaledCount = quality.scale(count);

        for (int i = 0; i < scaledCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double distance = level.random.nextDouble() * spread;

            level.sendParticles(particle,
                pos.x + Math.cos(angle) * distance,
                pos.y + level.random.nextDouble() * 2.0,
                pos.z + Math.sin(angle) * distance,
                1,
                (level.random.nextDouble() - 0.5) * 0.3,
                level.random.nextDouble() * 0.4,
                (level.random.nextDouble() - 0.5) * 0.3,
                0.02);
        }
    }
}
