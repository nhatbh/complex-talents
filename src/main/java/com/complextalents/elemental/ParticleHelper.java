package com.complextalents.elemental;

import com.complextalents.config.ElementalReactionConfig;
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
     * Adds unique particle effects for specific reactions
     */
    private static void spawnReactionSpecificEffects(ServerLevel level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                // Rising steam clouds
                for (int i = 0; i < 20; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.sendParticles(ParticleTypes.CLOUD,
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        1, 0, 0.3, 0, 0.02);
                }
            }
            case OVERLOADED -> {
                // Explosion shockwave
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.0);
                for (int i = 0; i < 40; i++) {
                    double angle = (i * Math.PI * 2.0) / 40;
                    level.sendParticles(ParticleTypes.SMOKE,
                        pos.x + Math.cos(angle) * 2.0, pos.y, pos.z + Math.sin(angle) * 2.0,
                        1, Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5, 0.02);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts
                for (int i = 0; i < 15; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + offsetX, pos.y + 2.0, pos.z + offsetZ,
                        1, 0, -0.5, 0, 0.02);
                }
            }
            case FROZEN -> {
                // Ice crystal burst
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                        pos.x, pos.y + 1.0, pos.z,
                        1, Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4, 0.02);
                }
                // Add ice shards
                for (int i = 0; i < 10; i++) {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        0.02);
                }
            }
            case BURNING -> {
                // Fire spiral
                for (int i = 0; i < 25; i++) {
                    double angle = (i * Math.PI * 2.0) / 25;
                    double height = i * 0.1;
                    level.sendParticles(ParticleTypes.FLAME,
                        pos.x + Math.cos(angle) * 1.5, pos.y + height, pos.z + Math.sin(angle) * 1.5,
                        1, 0, 0.2, 0, 0.02);
                }
                level.sendParticles(ParticleTypes.LAVA,
                    pos.x, pos.y, pos.z, 3, 0, 0, 0, 0.02);
            }
            case HYPERBLOOM -> {
                // Glowing orbs
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        0.02);
                }
            }
            case SINGULARITY -> {
                // Swirling void effect
                for (int ring = 0; ring < 5; ring++) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (i * Math.PI * 2.0) / 20 + ring;
                        double ringRadius = 2.0 - (ring * 0.3);
                        level.sendParticles(ParticleTypes.WITCH,
                            pos.x + Math.cos(angle) * ringRadius,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * ringRadius,
                            1, -Math.cos(angle) * 0.3, 0, -Math.sin(angle) * 0.3, 0.02);
                    }
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.x, pos.y, pos.z, 5, 0, 0, 0, 0.02);
            }
            case FRACTURE -> {
                // Shattering effect with fireworks
                for (int i = 0; i < 50; i++) {
                    level.sendParticles(ParticleTypes.FIREWORK,
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.8,
                        0.02);
                }
                level.sendParticles(ParticleTypes.EXPLOSION,
                    pos.x, pos.y + 1.0, pos.z, 3, 0, 0, 0, 0.0);
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
            case FIRE -> ParticleTypes.FLAME;
            case AQUA -> ParticleTypes.DRIPPING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case ENDER -> ParticleTypes.PORTAL;
        };
    }

    /**
     * Maps reactions to their corresponding particle types
     */
    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> ParticleTypes.CLOUD;
            case MELT -> ParticleTypes.DRIPPING_DRIPSTONE_WATER;
            case OVERLOADED -> ParticleTypes.EXPLOSION;
            case ELECTRO_CHARGED -> ParticleTypes.ELECTRIC_SPARK;
            case FROZEN -> ParticleTypes.SNOWFLAKE;
            case SUPERCONDUCT -> ParticleTypes.SOUL_FIRE_FLAME;
            case BURNING -> ParticleTypes.LAVA;
            case BLOOM -> ParticleTypes.CHERRY_LEAVES;
            case HYPERBLOOM -> ParticleTypes.GLOW;
            case BURGEON -> ParticleTypes.FLAME;
            case UNSTABLE_WARD -> ParticleTypes.REVERSE_PORTAL;
            case RIFT_PULL -> ParticleTypes.PORTAL;
            case SINGULARITY -> ParticleTypes.WITCH;
            case FRACTURE -> ParticleTypes.FIREWORK;
            case WITHERING_SEED -> ParticleTypes.SCULK_SOUL;
            case DECREPIT_GRASP -> ParticleTypes.SOUL;
        };
    }
}
