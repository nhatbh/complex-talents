package com.complextalents.elemental;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.superreaction.SuperReactionTier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Utility class for spawning elemental particles
 * Provides element-specific particle effects for stacks and reactions
 *
 * Note: This class uses registry lookups to access Iron's Spellbooks particles
 * to ensure compatibility in production environments where direct imports may fail
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
            case MELT, SUPERCONDUCT -> {
                // These reactions don't have special particle effects beyond the base particle
            }
            case VAPORIZE -> {
                // Rising steam clouds with fog
                ParticleOptions fogParticle = createFogParticle(new Vector3f(0.8f, 0.9f, 1.0f), 3.0f);
                if (fogParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
                        level.sendParticles(fogParticle,
                            pos.x + offsetX, pos.y, pos.z + offsetZ,
                            1, 0, 0.4, 0, 0.02);
                    }
                }
                // Add acid bubbles for water effect
                ParticleOptions acidBubble = getIronParticle("acid_bubble");
                if (acidBubble != null) {
                    for (int i = 0; i < 15; i++) {
                        level.sendParticles(acidBubble,
                            pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                            pos.y,
                            pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                            1, 0, 0.2, 0, 0.02);
                    }
                }
            }
            case OVERLOADED -> {
                // Massive explosion similar to Fireball spell
                // Central explosion emitter
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0.0);

                // Multiple explosion particles in a sphere
                for (int i = 0; i < 8; i++) {
                    double angle1 = level.random.nextDouble() * Math.PI * 2.0;
                    double angle2 = level.random.nextDouble() * Math.PI;
                    double distance = level.random.nextDouble() * 2.0;
                    double offsetX = Math.sin(angle2) * Math.cos(angle1) * distance;
                    double offsetY = Math.cos(angle2) * distance;
                    double offsetZ = Math.sin(angle2) * Math.sin(angle1) * distance;
                    level.sendParticles(ParticleTypes.EXPLOSION,
                        pos.x + offsetX, pos.y + 0.5 + offsetY, pos.z + offsetZ,
                        1, 0, 0, 0, 0.0);
                }

                // Fire particles bursting outward
                ParticleOptions fireParticle = getIronParticle("fire");
                if (fireParticle != null) {
                    for (int i = 0; i < 40; i++) {
                        double angle = (level.random.nextDouble() - 0.5) * Math.PI * 2.0;
                        double pitch = (level.random.nextDouble() - 0.3) * Math.PI;
                        double speed = 0.5 + level.random.nextDouble() * 0.8;
                        level.sendParticles(fireParticle,
                            pos.x, pos.y + 0.5, pos.z,
                            1,
                            Math.cos(pitch) * Math.cos(angle) * speed,
                            Math.sin(pitch) * speed,
                            Math.cos(pitch) * Math.sin(angle) * speed,
                            0.02);
                    }
                }

                // Lightning sparks radiating outward
                ParticleOptions sparkParticle = createSparkParticle(new Vector3f(1.0f, 0.9f, 0.3f));
                if (sparkParticle != null) {
                    for (int i = 0; i < 60; i++) {
                        double angle = (level.random.nextDouble() - 0.5) * Math.PI * 2.0;
                        double speed = 0.6 + level.random.nextDouble() * 0.7;
                        level.sendParticles(sparkParticle,
                            pos.x, pos.y + 0.5, pos.z,
                            1,
                            Math.cos(angle) * speed,
                            (level.random.nextDouble() - 0.2) * 0.5,
                            Math.sin(angle) * speed,
                            0.02);
                    }
                }

                // Add smoke clouds
                for (int i = 0; i < 25; i++) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.5,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        0.02);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts with electricity and zap particles
                ParticleOptions electricityParticle = getIronParticle("electricity");
                if (electricityParticle != null) {
                    for (int i = 0; i < 25; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                        level.sendParticles(electricityParticle,
                            pos.x + offsetX, pos.y + 2.5, pos.z + offsetZ,
                            1, 0, -0.6, 0, 0.02);
                    }
                }
                // Add zap particles (zap goes from particle spawn to a destination)
                for (int i = 0; i < 15; i++) {
                    double startX = pos.x + (level.random.nextDouble() - 0.5) * 1.5;
                    double startY = pos.y + level.random.nextDouble() * 2.0;
                    double startZ = pos.z + (level.random.nextDouble() - 0.5) * 1.5;
                    // Create destination offset from start position
                    Vec3 destination = new Vec3(
                        startX + (level.random.nextDouble() - 0.5) * 2.0,
                        startY + (level.random.nextDouble() - 0.5) * 2.0,
                        startZ + (level.random.nextDouble() - 0.5) * 2.0
                    );
                    ParticleOptions zapParticle = createZapParticle(destination);
                    if (zapParticle != null) {
                        level.sendParticles(zapParticle, startX, startY, startZ, 1, 0, 0, 0, 0.0);
                    }
                }
            }
            case FROZEN -> {
                // Ice crystal burst with snowflakes
                ParticleOptions snowflakeParticle = getIronParticle("snowflake");
                if (snowflakeParticle != null) {
                    for (int i = 0; i < 40; i++) {
                        double angle = (i * Math.PI * 2.0) / 40;
                        level.sendParticles(snowflakeParticle,
                            pos.x, pos.y + 1.0, pos.z,
                            1, Math.cos(angle) * 0.5, 0.15, Math.sin(angle) * 0.5, 0.02);
                    }
                }
                // Add snow dust
                ParticleOptions snowDustParticle = getIronParticle("snow_dust");
                if (snowDustParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        level.sendParticles(snowDustParticle,
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
            }
            case BURNING -> {
                // Fire spiral with dragon fire
                ParticleOptions dragonFireParticle = getIronParticle("dragon_fire");
                if (dragonFireParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        double angle = (i * Math.PI * 2.0) / 30;
                        double height = i * 0.12;
                        level.sendParticles(dragonFireParticle,
                            pos.x + Math.cos(angle) * 1.8, pos.y + height, pos.z + Math.sin(angle) * 1.8,
                            1, 0, 0.25, 0, 0.02);
                    }
                }
                // Add embers
                ParticleOptions emberParticle = getIronParticle("ember");
                if (emberParticle != null) {
                    for (int i = 0; i < 25; i++) {
                        level.sendParticles(emberParticle,
                            pos.x, pos.y + 0.2, pos.z,
                            1,
                            (level.random.nextDouble() - 0.5) * 0.5,
                            level.random.nextDouble() * 0.8,
                            (level.random.nextDouble() - 0.5) * 0.5,
                            0.02);
                    }
                }
            }
            case HYPERBLOOM -> {
                // Glowing wisps
                ParticleOptions wispParticle = getIronParticle("wisp");
                if (wispParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        level.sendParticles(wispParticle,
                            pos.x, pos.y + 1.0, pos.z,
                            1,
                            (level.random.nextDouble() - 0.5) * 0.7,
                            (level.random.nextDouble() - 0.5) * 0.7,
                            (level.random.nextDouble() - 0.5) * 0.7,
                            0.02);
                    }
                }
                // Add fireflies
                ParticleOptions fireflyParticle = getIronParticle("firefly");
                if (fireflyParticle != null) {
                    for (int i = 0; i < 20; i++) {
                        level.sendParticles(fireflyParticle,
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
            }
            case SINGULARITY -> {
                // Swirling void effect with siphon particles
                ParticleOptions siphonParticle = getIronParticle("siphon");
                if (siphonParticle != null) {
                    for (int ring = 0; ring < 6; ring++) {
                        for (int i = 0; i < 25; i++) {
                            double angle = (i * Math.PI * 2.0) / 25 + ring;
                            double ringRadius = 2.5 - (ring * 0.35);
                            level.sendParticles(siphonParticle,
                                pos.x + Math.cos(angle) * ringRadius,
                                pos.y + 0.5,
                                pos.z + Math.sin(angle) * ringRadius,
                                1, -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4, 0.02);
                        }
                    }
                }
                // Add unstable ender particles
                ParticleOptions unstableEnderParticle = getIronParticle("unstable_ender");
                if (unstableEnderParticle != null) {
                    for (int i = 0; i < 20; i++) {
                        level.sendParticles(unstableEnderParticle,
                            pos.x, pos.y + 0.5, pos.z,
                            1,
                            (level.random.nextDouble() - 0.5) * 0.3,
                            (level.random.nextDouble() - 0.5) * 0.3,
                            (level.random.nextDouble() - 0.5) * 0.3,
                            0.02);
                    }
                }
            }
            case FRACTURE -> {
                // Shattering glass/crystal effect
                // Ice/crystal shards bursting outward in all directions
                ParticleOptions snowflakeParticle = getIronParticle("snowflake");
                if (snowflakeParticle != null) {
                    for (int i = 0; i < 50; i++) {
                        double angle = level.random.nextDouble() * Math.PI * 2.0;
                        double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
                        double speed = 0.3 + level.random.nextDouble() * 0.9;

                        level.sendParticles(snowflakeParticle,
                            pos.x, pos.y + 1.0, pos.z,
                            1,
                            Math.cos(pitch) * Math.cos(angle) * speed,
                            Math.sin(pitch) * speed,
                            Math.cos(pitch) * Math.sin(angle) * speed,
                            0.02);
                    }
                }

                // Add ice shard effect with snow dust
                ParticleOptions snowDustParticle = getIronParticle("snow_dust");
                if (snowDustParticle != null) {
                    for (int i = 0; i < 70; i++) {
                        double angle = level.random.nextDouble() * Math.PI * 2.0;
                        double speed = 0.4 + level.random.nextDouble() * 0.8;
                        level.sendParticles(snowDustParticle,
                            pos.x, pos.y + 1.0, pos.z,
                            1,
                            Math.cos(angle) * speed,
                            (level.random.nextDouble() - 0.3) * 0.6,
                            Math.sin(angle) * speed,
                            0.02);
                    }
                }

                // White spark shards flying outward (like glass shards)
                ParticleOptions sparkParticle = createSparkParticle(new Vector3f(1.0f, 1.0f, 1.0f));
                if (sparkParticle != null) {
                    for (int i = 0; i < 80; i++) {
                        double angle = level.random.nextDouble() * Math.PI * 2.0;
                        double pitch = (level.random.nextDouble() - 0.4) * Math.PI * 0.8;
                        double speed = 0.5 + level.random.nextDouble() * 1.2;
                        level.sendParticles(sparkParticle,
                            pos.x, pos.y + 1.0, pos.z,
                            1,
                            Math.cos(pitch) * Math.cos(angle) * speed,
                            Math.sin(pitch) * speed,
                            Math.cos(pitch) * Math.sin(angle) * speed,
                            0.02);
                    }
                }

                // Central shattering point with crit particles
                for (int i = 0; i < 15; i++) {
                    level.sendParticles(ParticleTypes.CRIT,
                        pos.x, pos.y + 1.0, pos.z,
                        1,
                        (level.random.nextDouble() - 0.5) * 1.0,
                        (level.random.nextDouble() - 0.5) * 1.0,
                        (level.random.nextDouble() - 0.5) * 1.0,
                        0.02);
                }

                // Add enchantment glitter for the "magical shatter" effect
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleTypes.ENCHANT,
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + 1.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        1,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        0.02);
                }
            }
            case BLOOM -> {
                // Nature bloom with fireflies
                ParticleOptions fireflyParticle = getIronParticle("firefly");
                if (fireflyParticle != null) {
                    for (int i = 0; i < 40; i++) {
                        level.sendParticles(fireflyParticle,
                            pos.x, pos.y + 0.5, pos.z,
                            1,
                            (level.random.nextDouble() - 0.5) * 0.6,
                            level.random.nextDouble() * 0.5,
                            (level.random.nextDouble() - 0.5) * 0.6,
                            0.02);
                    }
                }
            }
            case BURGEON -> {
                // Explosive nature with embers
                ParticleOptions emberParticle = getIronParticle("ember");
                if (emberParticle != null) {
                    for (int i = 0; i < 35; i++) {
                        level.sendParticles(emberParticle,
                            pos.x, pos.y + 0.5, pos.z,
                            1,
                            (level.random.nextDouble() - 0.5) * 0.7,
                            level.random.nextDouble() * 0.8,
                            (level.random.nextDouble() - 0.5) * 0.7,
                            0.02);
                    }
                }
            }
            case UNSTABLE_WARD -> {
                // Portal frame effect
                ParticleOptions portalFrameParticle = getIronParticle("portal_frame");
                if (portalFrameParticle != null) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (i * Math.PI * 2.0) / 20;
                        level.sendParticles(portalFrameParticle,
                            pos.x + Math.cos(angle) * 1.5,
                            pos.y + 1.0,
                            pos.z + Math.sin(angle) * 1.5,
                            1, 0, 0.1, 0, 0.02);
                    }
                }
            }
            case RIFT_PULL -> {
                // Unstable ender vortex
                ParticleOptions unstableEnderParticle = getIronParticle("unstable_ender");
                if (unstableEnderParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        double angle = (i * Math.PI * 2.0) / 30;
                        level.sendParticles(unstableEnderParticle,
                            pos.x + Math.cos(angle) * 2.0,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * 2.0,
                            1, -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4, 0.02);
                    }
                }
            }
            case WITHERING_SEED -> {
                // Blood ground spread
                ParticleOptions bloodGroundParticle = getIronParticle("blood_ground");
                if (bloodGroundParticle != null) {
                    for (int i = 0; i < 25; i++) {
                        level.sendParticles(bloodGroundParticle,
                            pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                            pos.y + 0.1,
                            pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                            1, 0, 0.05, 0, 0.02);
                    }
                }
            }
            case DECREPIT_GRASP -> {
                // Blood particles
                ParticleOptions bloodParticle = getIronParticle("blood");
                if (bloodParticle != null) {
                    for (int i = 0; i < 30; i++) {
                        level.sendParticles(bloodParticle,
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
     * Uses registry lookups for Iron's Spellbooks particles
     */
    private static ParticleOptions getParticleForElement(ElementType element) {
        return switch (element) {
            case FIRE -> getIronParticle("fire");
            case AQUA -> getIronParticle("acid_bubble");
            case ICE -> getIronParticle("snowflake");
            case LIGHTNING -> getIronParticle("electricity");
            case NATURE -> getIronParticle("firefly");
            case ENDER -> getIronParticle("unstable_ender");
        };
    }

    /**
     * Maps reactions to their corresponding particle types from Iron's Spells
     * Uses registry lookups for cross-mod compatibility
     */
    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> createFogParticle(new Vector3f(0.8f, 0.9f, 1.0f), 2.5f);
            case MELT -> getIronParticle("acid");
            case OVERLOADED -> getIronParticle("fire"); // Explosive fire
            case ELECTRO_CHARGED -> getIronParticle("electricity");
            case FROZEN -> getIronParticle("snow_dust");
            case SUPERCONDUCT -> getIronParticle("ring_smoke");
            case BURNING -> getIronParticle("dragon_fire");
            case BLOOM -> getIronParticle("firefly");
            case HYPERBLOOM -> getIronParticle("wisp");
            case BURGEON -> getIronParticle("ember");
            case UNSTABLE_WARD -> getIronParticle("portal_frame");
            case RIFT_PULL -> getIronParticle("unstable_ender");
            case SINGULARITY -> getIronParticle("siphon");
            case FRACTURE -> getIronParticle("snowflake"); // Shattering ice/crystal
            case WITHERING_SEED -> getIronParticle("blood_ground");
            case DECREPIT_GRASP -> getIronParticle("blood");
        };
    }

    /**
     * Gets an Iron's Spellbooks particle from the registry
     * Returns null if the particle is not found (fail-safe)
     */
    private static ParticleOptions getIronParticle(String particleName) {
        try {
            ResourceLocation particleId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particleName);
            // Use reflection to access particle registry to avoid deprecation warnings
            var particleType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType == null) {
                return ParticleTypes.CRIT; // Fallback to vanilla particle
            }
            // For simple particle types without options, the type itself is a ParticleOptions
            if (particleType instanceof ParticleOptions options) {
                return options;
            }
            return ParticleTypes.CRIT;
        } catch (Exception e) {
            return ParticleTypes.CRIT; // Fallback to vanilla particle
        }
    }

    /**
     * Creates a FogParticleOptions using reflection to avoid direct dependency
     * This allows the code to work even if Iron's Spellbooks classes change
     */
    private static ParticleOptions createFogParticle(Vector3f color, float scale) {
        try {
            Class<?> fogClass = Class.forName("io.redspace.ironsspellbooks.particle.FogParticleOptions");
            var constructor = fogClass.getConstructor(Vector3f.class, float.class);
            return (ParticleOptions) constructor.newInstance(color, scale);
        } catch (Exception e) {
            return ParticleTypes.CLOUD; // Fallback
        }
    }

    /**
     * Creates a SparkParticleOptions using reflection
     */
    private static ParticleOptions createSparkParticle(Vector3f color) {
        try {
            Class<?> sparkClass = Class.forName("io.redspace.ironsspellbooks.particle.SparkParticleOptions");
            var constructor = sparkClass.getConstructor(Vector3f.class);
            return (ParticleOptions) constructor.newInstance(color);
        } catch (Exception e) {
            return ParticleTypes.FLAME; // Fallback
        }
    }

    /**
     * Creates a ZapParticleOption using reflection
     */
    private static ParticleOptions createZapParticle(Vec3 destination) {
        try {
            Class<?> zapClass = Class.forName("io.redspace.ironsspellbooks.particle.ZapParticleOption");
            var constructor = zapClass.getConstructor(Vec3.class);
            return (ParticleOptions) constructor.newInstance(destination);
        } catch (Exception e) {
            return getIronParticle("electricity"); // Fallback
        }
    }

    /**
     * Spawns particles for a Super-Reaction
     * Creates an absolutely MASSIVE and spectacular particle effect
     * worthy of the power unleashed
     */
    public static void spawnSuperReactionParticles(ServerLevel level, Vec3 pos, ElementType element, SuperReactionTier tier) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        ParticleOptions primaryParticle = getParticleForElement(element);
        if (primaryParticle == null) return;

        // Scale effects based on tier
        int baseParticleCount = 100;
        int particleCount = baseParticleCount * tier.ordinal();
        double radius = 3.0 + tier.ordinal() * 2.0;
        double height = 4.0 + tier.ordinal() * 1.5;

        // Create ascending vortex
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double) particleCount;
            double angle = t * Math.PI * 8; // Multiple spirals
            double currentRadius = radius * (1 - t * 0.5); // Narrows as it rises
            double currentHeight = t * height;

            double x = pos.x + Math.cos(angle) * currentRadius;
            double y = pos.y + currentHeight;
            double z = pos.z + Math.sin(angle) * currentRadius;

            // Outward and upward velocity
            double vx = Math.cos(angle) * 0.1;
            double vy = 0.3 + tier.ordinal() * 0.1;
            double vz = Math.sin(angle) * 0.1;

            level.sendParticles(primaryParticle, x, y, z, 1, vx, vy, vz, 0.02);
        }

        // Create expanding shockwave rings
        for (int ring = 0; ring < tier.ordinal() + 1; ring++) {
            double ringDelay = ring * 0.2; // Stagger the rings
            int ringParticles = 40;
            double ringRadius = (ring + 1) * 2.0;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (i * Math.PI * 2.0) / ringParticles;
                double x = pos.x + Math.cos(angle) * ringRadius;
                double z = pos.z + Math.sin(angle) * ringRadius;

                // Horizontal expansion
                double vx = Math.cos(angle) * 0.5;
                double vz = Math.sin(angle) * 0.5;

                level.sendParticles(ParticleTypes.EXPLOSION, x, pos.y + ring, z, 1, vx, 0.1, vz, 0.02);
            }
        }

        // Add tier-specific special effects
        switch (tier) {
            case TIER_2 -> {
                // Add electric sparks
                for (int i = 0; i < 20; i++) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + (level.random.nextDouble() - 0.5) * radius,
                        pos.y + level.random.nextDouble() * height,
                        pos.z + (level.random.nextDouble() - 0.5) * radius,
                        1, 0, 0, 0, 0.05);
                }
            }
            case TIER_3 -> {
                // Add end rod particles for mystical effect
                for (int i = 0; i < 40; i++) {
                    level.sendParticles(ParticleTypes.END_ROD,
                        pos.x + (level.random.nextDouble() - 0.5) * radius,
                        pos.y + level.random.nextDouble() * height,
                        pos.z + (level.random.nextDouble() - 0.5) * radius,
                        1, 0, 0.1, 0, 0.05);
                }
            }
            case TIER_4 -> {
                // Ultimate tier - Dragon breath and portal particles
                for (int i = 0; i < 60; i++) {
                    level.sendParticles(ParticleTypes.DRAGON_BREATH,
                        pos.x + (level.random.nextDouble() - 0.5) * radius * 1.5,
                        pos.y + level.random.nextDouble() * height * 1.5,
                        pos.z + (level.random.nextDouble() - 0.5) * radius * 1.5,
                        1, 0, 0, 0, 0.1);

                    if (i % 2 == 0) {
                        level.sendParticles(ParticleTypes.PORTAL,
                            pos.x + (level.random.nextDouble() - 0.5) * radius,
                            pos.y + level.random.nextDouble() * height,
                            pos.z + (level.random.nextDouble() - 0.5) * radius,
                            1, 0, 0, 0, 0.5);
                    }
                }

                // Central pillar of light
                for (int i = 0; i < 100; i++) {
                    level.sendParticles(ParticleTypes.GLOW,
                        pos.x, pos.y + i * 0.1, pos.z,
                        1, 0, 0.05, 0, 0);
                }
            }
            default -> {}
        }

        // Always add flash effect
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + height / 2, pos.z, 3, 0, 0, 0, 0);

        // Add sonic boom for tier 3+
        if (tier.ordinal() >= SuperReactionTier.TIER_3.ordinal()) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
        }
    }
}
