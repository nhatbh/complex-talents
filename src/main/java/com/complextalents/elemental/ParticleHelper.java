package com.complextalents.elemental;

import com.complextalents.config.ElementalReactionConfig;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for spawning elemental particles
 * Provides element-specific particle effects for stacks and reactions
 */
public class ParticleHelper {

    /**
     * Spawns particles for an element stack being applied
     * Creates a spectacular particle burst around the entity
     */
    public static void spawnStackParticles(ServerLevel level, Vec3 pos, ElementType element, int stackCount) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = getParticleForElement(element);
        if (particle == null) return;

        // Scale effects based on stack count
        int particleCount = 5 + (stackCount * 4); // More particles per stack
        double radius = 0.6 + (stackCount * 0.1); // Larger radius for higher stacks
        double height = 1.2 + (stackCount * 0.15); // Taller effect for more stacks

        // Main particle spiral effect
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount;
            double spiralHeight = (i / (double) particleCount) * height;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = spiralHeight;

            // Spiral upward velocity
            double velocityX = Math.cos(angle) * 0.05;
            double velocityY = 0.15 + (stackCount * 0.02);
            double velocityZ = Math.sin(angle) * 0.05;

            level.sendParticles(particle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, velocityX, velocityY, velocityZ, 0.02);
        }

        // Add burst particles for high stacks
        if (stackCount >= 3) {
            for (int i = 0; i < stackCount * 3; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * radius * 1.5;
                double offsetY = level.random.nextDouble() * height;
                double offsetZ = (level.random.nextDouble() - 0.5) * radius * 1.5;

                double velocityX = (level.random.nextDouble() - 0.5) * 0.2;
                double velocityY = level.random.nextDouble() * 0.25;
                double velocityZ = (level.random.nextDouble() - 0.5) * 0.2;

                level.sendParticles(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, velocityX, velocityY, velocityZ, 0.02);
            }
        }

        // Add glow effect for max stacks
        if (stackCount >= 5) {
            level.sendParticles(ParticleTypes.GLOW,
                pos.x, pos.y + 0.5, pos.z,
                3, 0, 0.1, 0, 0.02);
        }
    }

    /**
     * Spawns particles for an elemental reaction
     * Creates an extremely dramatic and spectacular particle effect
     */
    public static void spawnReactionParticles(ServerLevel level, Vec3 pos, ElementalReaction reaction) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions particle = getParticleForReaction(reaction);
        if (particle == null) return;

        // MASSIVE explosion burst
        int particleCount = 60; // Tripled particle count
        double radius = 2.0; // Doubled radius

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

                level.sendParticles(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, velocityX, velocityY, velocityZ, 0.05);
            }
        }

        // Central explosion core
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            double velocityX = (level.random.nextDouble() - 0.5) * 0.6;
            double velocityY = level.random.nextDouble() * 0.8;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.6;

            level.sendParticles(particle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, velocityX, velocityY, velocityZ, 0.05);
        }

        // Multiple flash effects
        for (int i = 0; i < 5; i++) {
            double flashOffset = i * 0.3;
            level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y + 0.5 + flashOffset, pos.z,
                1, 0, 0, 0, 0.0);
        }

        // Add reaction-specific effects
        spawnReactionSpecificEffects(level, pos, reaction);
    }

    /**
     * Adds unique particle effects for specific reactions using Iron's Spells particles
     */
    private static void spawnReactionSpecificEffects(ServerLevel level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                // Rising steam clouds with fog
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
                    level.sendParticles(ParticleRegistry.FOG_PARTICLE.get(),
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        1, 0, 0.4, 0, 0.02);
                }
                // Add acid bubbles for water effect
                for (int i = 0; i < 15; i++) {
                    level.sendParticles(ParticleRegistry.ACID_BUBBLE_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1, 0, 0.2, 0, 0.02);
                }
            }
            case OVERLOADED -> {
                // Explosion shockwave with blastwave particles
                for (int i = 0; i < 3; i++) {
                    level.sendParticles(ParticleRegistry.BLASTWAVE_PARTICLE.get(),
                        pos.x, pos.y + (i * 0.3), pos.z, 1, 0, 0, 0, 0.0);
                }
                // Add spark ring
                for (int i = 0; i < 50; i++) {
                    double angle = (i * Math.PI * 2.0) / 50;
                    level.sendParticles(ParticleRegistry.SPARK_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 2.5, pos.y, pos.z + Math.sin(angle) * 2.5,
                        1, Math.cos(angle) * 0.6, 0.1, Math.sin(angle) * 0.6, 0.02);
                }
                // Add smoke
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleRegistry.RING_SMOKE_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        level.random.nextDouble() * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        0.02);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts with electricity and zap particles
                for (int i = 0; i < 25; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.sendParticles(ParticleRegistry.ELECTRICITY_PARTICLE.get(),
                        pos.x + offsetX, pos.y + 2.5, pos.z + offsetZ,
                        1, 0, -0.6, 0, 0.02);
                }
                // Add zap particles
                for (int i = 0; i < 15; i++) {
                    level.sendParticles(ParticleRegistry.ZAP_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        0.02);
                }
            }
            case FROZEN -> {
                // Ice crystal burst with snowflakes
                for (int i = 0; i < 40; i++) {
                    double angle = (i * Math.PI * 2.0) / 40;
                    level.sendParticles(ParticleRegistry.SNOWFLAKE_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        1, Math.cos(angle) * 0.5, 0.15, Math.sin(angle) * 0.5, 0.02);
                }
                // Add snow dust
                for (int i = 0; i < 30; i++) {
                    level.sendParticles(ParticleRegistry.SNOW_DUST.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        0.02);
                }
            }
            case BURNING -> {
                // Fire spiral with dragon fire
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    double height = i * 0.12;
                    level.sendParticles(ParticleRegistry.DRAGON_FIRE_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 1.8, pos.y + height, pos.z + Math.sin(angle) * 1.8,
                        1, 0, 0.25, 0, 0.02);
                }
                // Add embers
                for (int i = 0; i < 25; i++) {
                    level.sendParticles(ParticleRegistry.EMBER_PARTICLE.get(),
                        pos.x, pos.y + 0.2, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        0.02);
                }
            }
            case HYPERBLOOM -> {
                // Glowing wisps
                for (int i = 0; i < 30; i++) {
                    level.sendParticles(ParticleRegistry.WISP_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        0.02);
                }
                // Add fireflies
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleRegistry.FIREFLY_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        0.02);
                }
            }
            case SINGULARITY -> {
                // Swirling void effect with siphon particles
                for (int ring = 0; ring < 6; ring++) {
                    for (int i = 0; i < 25; i++) {
                        double angle = (i * Math.PI * 2.0) / 25 + ring;
                        double ringRadius = 2.5 - (ring * 0.35);
                        level.sendParticles(ParticleRegistry.SIPHON_PARTICLE.get(),
                            pos.x + Math.cos(angle) * ringRadius,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * ringRadius,
                            1, -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4, 0.02);
                    }
                }
                // Add unstable ender particles
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        0.02);
                }
            }
            case FRACTURE -> {
                // Shattering effect with shockwave
                for (int i = 0; i < 5; i++) {
                    level.sendParticles(ParticleRegistry.SHOCKWAVE_PARTICLE.get(),
                        pos.x, pos.y + 1.0 + (i * 0.2), pos.z,
                        1, 0, 0, 0, 0.0);
                }
                // Add spark explosion
                for (int i = 0; i < 60; i++) {
                    level.sendParticles(ParticleRegistry.SPARK_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.9,
                        (level.random.nextDouble() - 0.5) * 0.9,
                        (level.random.nextDouble() - 0.5) * 0.9,
                        0.02);
                }
            }
            case BLOOM -> {
                // Nature bloom with fireflies
                for (int i = 0; i < 40; i++) {
                    level.sendParticles(ParticleRegistry.FIREFLY_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        level.random.nextDouble() * 0.5,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        0.02);
                }
            }
            case BURGEON -> {
                // Explosive nature with embers
                for (int i = 0; i < 35; i++) {
                    level.sendParticles(ParticleRegistry.EMBER_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        0.02);
                }
            }
            case UNSTABLE_WARD -> {
                // Portal frame effect
                for (int i = 0; i < 20; i++) {
                    double angle = (i * Math.PI * 2.0) / 20;
                    level.sendParticles(ParticleRegistry.PORTAL_FRAME_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 1.5,
                        pos.y + 1.0,
                        pos.z + Math.sin(angle) * 1.5,
                        1, 0, 0.1, 0, 0.02);
                }
            }
            case RIFT_PULL -> {
                // Unstable ender vortex
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    level.sendParticles(ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 2.0,
                        pos.y + 0.5,
                        pos.z + Math.sin(angle) * 2.0,
                        1, -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4, 0.02);
                }
            }
            case WITHERING_SEED -> {
                // Blood ground spread
                for (int i = 0; i < 25; i++) {
                    level.sendParticles(ParticleRegistry.BLOOD_GROUND_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.1,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1, 0, 0.05, 0, 0.02);
                }
            }
            case DECREPIT_GRASP -> {
                // Blood particles
                for (int i = 0; i < 30; i++) {
                    level.sendParticles(ParticleRegistry.BLOOD_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        -0.1,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        0.02);
                }
            }
        }
    }

    /**
     * Spawns AoE particles in a circular area
     * Used for reactions with area effects
     */
    public static void spawnAoeParticles(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int density) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        int particleCount = (int) (radius * density);

        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = level.random.nextDouble() * radius;

            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            double offsetY = level.random.nextDouble() * 0.5;

            level.sendParticles(particle,
                center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                1, 0, 0.1, 0, 0.02);
        }
    }

    /**
     * Maps element types to their corresponding particle types
     */
    private static ParticleOptions getParticleForElement(ElementType element) {
        return switch (element) {
            case FIRE -> ParticleRegistry.FIRE_PARTICLE.get();
            case AQUA -> ParticleRegistry.ACID_BUBBLE_PARTICLE.get();
            case ICE -> ParticleRegistry.SNOWFLAKE_PARTICLE.get();
            case LIGHTNING -> ParticleRegistry.ELECTRICITY_PARTICLE.get();
            case NATURE -> ParticleRegistry.FIREFLY_PARTICLE.get();
            case ENDER -> ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get();
        };
    }

    /**
     * Maps reactions to their corresponding particle types from Iron's Spells
     */
    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> ParticleRegistry.FOG_PARTICLE.get();
            case MELT -> ParticleRegistry.ACID_PARTICLE.get();
            case OVERLOADED -> ParticleRegistry.BLASTWAVE_PARTICLE.get();
            case ELECTRO_CHARGED -> ParticleRegistry.ZAP_PARTICLE.get();
            case FROZEN -> ParticleRegistry.SNOW_DUST.get();
            case SUPERCONDUCT -> ParticleRegistry.RING_SMOKE_PARTICLE.get();
            case BURNING -> ParticleRegistry.DRAGON_FIRE_PARTICLE.get();
            case BLOOM -> ParticleRegistry.FIREFLY_PARTICLE.get();
            case HYPERBLOOM -> ParticleRegistry.WISP_PARTICLE.get();
            case BURGEON -> ParticleRegistry.EMBER_PARTICLE.get();
            case UNSTABLE_WARD -> ParticleRegistry.PORTAL_FRAME_PARTICLE.get();
            case RIFT_PULL -> ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get();
            case SINGULARITY -> ParticleRegistry.SIPHON_PARTICLE.get();
            case FRACTURE -> ParticleRegistry.SHOCKWAVE_PARTICLE.get();
            case WITHERING_SEED -> ParticleRegistry.BLOOD_GROUND_PARTICLE.get();
            case DECREPIT_GRASP -> ParticleRegistry.BLOOD_PARTICLE.get();
        };
    }
}
