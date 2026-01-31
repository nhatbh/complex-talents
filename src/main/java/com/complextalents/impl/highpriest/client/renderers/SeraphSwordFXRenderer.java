package com.complextalents.impl.highpriest.client.renderers;

import com.complextalents.impl.highpriest.sound.HighPriestSounds;
import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for Seraph's Bouncing Sword particle and sound effects.
 * Handles three effect types: flight trail, terrain impact, and entity hit.
 */
public class SeraphSwordFXRenderer {

    // Effect type constants
    public static final int EFFECT_FLIGHT_TRAIL = 0;
    public static final int EFFECT_TERRAIN_HIT = 1;
    public static final int EFFECT_ENTITY_HIT = 2;

    /**
     * Main render method that routes to the appropriate effect handler.
     *
     * @param level The level to spawn particles in
     * @param pos The position of the effect
     * @param velocity The velocity (used for flight trail direction, null for collisions)
     * @param effectType The type of effect to render
     */
    public static void render(Level level, Vec3 pos, Vec3 velocity, int effectType) {
        switch (effectType) {
            case EFFECT_FLIGHT_TRAIL -> renderFlightTrail(level, pos, velocity);
            case EFFECT_TERRAIN_HIT -> renderTerrainImpact(level, pos);
            case EFFECT_ENTITY_HIT -> renderEntityHit(level, pos);
        }
    }

    /**
     * Renders the holy/divine flight trail behind the sword.
     * Uses END_ROD particles for a holy divine trail effect.
     *
     * @param level The level to spawn particles in
     * @param pos The current position of the sword
     * @param velocity The velocity of the sword (for trail direction)
     */
    private static void renderFlightTrail(Level level, Vec3 pos, Vec3 velocity) {
        if (velocity == null) {
            return;
        }

        // Calculate opposite direction for trail (behind the sword)
        Vec3 trailDir = velocity.normalize().scale(-0.15);

        // Spawn main END_ROD trail particle behind sword
        level.addParticle(
            ParticleTypes.END_ROD,
            pos.x, pos.y, pos.z,
            trailDir.x, trailDir.y, trailDir.z
        );

        // Occasionally spawn extra particles for more visual flair
        if (level.random.nextFloat() < 0.3f) {
            // Add slight randomness to position
            double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

            level.addParticle(
                ParticleTypes.END_ROD,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                trailDir.x * 0.5, trailDir.y * 0.5, trailDir.z * 0.5
            );
        }
    }

    /**
     * Renders the terrain collision effect - small ember burst with clang sound.
     *
     * @param level The level to spawn particles in
     * @param pos The position of the collision
     */
    private static void renderTerrainImpact(Level level, Vec3 pos) {
        // Get ember particle with fallback
        ParticleOptions emberParticle = IronParticleHelper.getIronParticle("ember");
        if (emberParticle == null) {
            emberParticle = ParticleTypes.FLAME;
        }

        // Spawn 5-7 ember particles in a small burst
        int particleCount = 5 + level.random.nextInt(3); // 5-7 particles
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + level.random.nextDouble() * 0.15;

            double velX = Math.cos(angle) * speed;
            double velY = 0.1 + level.random.nextDouble() * 0.2; // Slight upward bias
            double velZ = Math.sin(angle) * speed;

            level.addParticle(
                emberParticle,
                pos.x, pos.y, pos.z,
                velX, velY, velZ
            );
        }

        // Play clang sound
        level.playLocalSound(
            pos.x, pos.y, pos.z,
            HighPriestSounds.SWORD_CLANG.get(),
            SoundSource.HOSTILE,
            1.5f, 1.5f, false
        );
    }

    /**
     * Renders the entity hit effect - flash particle with hit sound.
     *
     * @param level The level to spawn particles in
     * @param pos The position of the hit
     */
    private static void renderEntityHit(Level level, Vec3 pos) {
        // Spawn flash particle for impact
        level.addParticle(
            ParticleTypes.FLASH,
            pos.x, pos.y, pos.z,
            0, 0, 0
        );

        // Spawn a few extra particles for impact effect
        for (int i = 0; i < 3; i++) {
            double spread = 0.2;
            double velX = (level.random.nextDouble() - 0.5) * spread;
            double velY = (level.random.nextDouble() - 0.5) * spread;
            double velZ = (level.random.nextDouble() - 0.5) * spread;

            level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z,
                velX, velY, velZ
            );
        }

        // Play hit sound
        level.playLocalSound(
            pos.x, pos.y, pos.z,
            HighPriestSounds.SWORD_HIT.get(),
            SoundSource.HOSTILE,
            1.5f, 1.0f, false
        );
    }
}
