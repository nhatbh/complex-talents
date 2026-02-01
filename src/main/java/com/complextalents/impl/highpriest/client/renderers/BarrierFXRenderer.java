package com.complextalents.impl.highpriest.client.renderers;

import com.complextalents.impl.highpriest.sound.HighPriestSounds;
import com.complextalents.network.highpriest.SpawnBarrierFXPacket.EffectType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Renderer for Sanctuary Barrier particle and sound effects.
 * Handles four effect types: creation, destruction, entity/projectile hit, and ambient.
 */
public class BarrierFXRenderer {

    /**
     * Main render method that routes to the appropriate effect handler.
     *
     * @param level     The level to spawn particles in
     * @param x         The x position of the effect
     * @param y         The y position of the effect
     * @param z         The z position of the effect
     * @param effectType The type of effect to render
     * @param radius    The barrier radius (used for destruction effect)
     */
    public static void render(Level level, double x, double y, double z, EffectType effectType, float radius) {
        switch (effectType) {
            case CREATED -> renderCreated(level, x, y, z);
            case DESTROYED -> renderDestroyed(level, x, y, z, radius);
            case ENTITY_HIT -> renderEntityHit(level, x, y, z);
            case AMBIENT -> renderAmbient(level, x, y, z);
        }
    }

    /**
     * Renders the barrier creation effect - explosion of END_ROD particles with charge sound.
     *
     * @param level The level to spawn particles in
     * @param x     The x position of the barrier
     * @param y     The y position of the barrier
     * @param z     The z position of the barrier
     */
    private static void renderCreated(Level level, double x, double y, double z) {
        // Explosion of END_ROD particles in a sphere
        int particleCount = 25 + level.random.nextInt(6); // 25-30 particles
        for (int i = 0; i < particleCount; i++) {
            // Spherical distribution
            double theta = level.random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double distance = 1.5 + level.random.nextDouble() * 0.5;

            double px = x + distance * Math.sin(phi) * Math.cos(theta);
            double py = y + 1.0 + distance * Math.sin(phi) * Math.sin(theta);
            double pz = z + distance * Math.cos(phi);

            // Velocity outward from center
            double vx = (px - x) * 0.1;
            double vy = (py - (y + 1.0)) * 0.1;
            double vz = (pz - z) * 0.1;

            level.addParticle(
                ParticleTypes.END_ROD,
                px, py, pz, vx, vy, vz
            );
        }

        // Play respawn anchor charge sound
        level.playLocalSound(
            x, y, z,
            SoundEvents.RESPAWN_ANCHOR_CHARGE,
            SoundSource.BLOCKS,
            1.0f, 0.8f, false
        );
    }

    /**
     * Renders the barrier destruction effect - END_ROD particles falling from sky with deplete sound.
     *
     * @param level  The level to spawn particles in
     * @param x      The x position of the barrier
     * @param y      The y position of the barrier
     * @param z      The z position of the barrier
     * @param radius The barrier radius for particle spread
     */
    private static void renderDestroyed(Level level, double x, double y, double z, float radius) {
        // END_ROD particles falling from sky within barrier radius (rain effect)
        int particleCount = (int) (radius * 15);

        for (int i = 0; i < particleCount; i++) {
            // Random position within barrier circle
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = level.random.nextDouble() * radius;
            double px = x + Math.cos(angle) * distance;
            double pz = z + Math.sin(angle) * distance;

            // Start from above and fall down
            double py = y + 2.5 + level.random.nextDouble();

            level.addParticle(
                ParticleTypes.END_ROD,
                px, py, pz,
                0, -0.05, 0 // Falling velocity
            );
        }

        // Play respawn anchor deplete sound
        level.playLocalSound(
            x, y, z,
            SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
            SoundSource.BLOCKS,
            1.0f, 0.8f, false
        );
    }

    /**
     * Renders the entity/projectile hit effect - flash particle with clang sound.
     *
     * @param level The level to spawn particles in
     * @param x     The x position of the hit
     * @param y     The y position of the hit
     * @param z     The z position of the hit
     */
    private static void renderEntityHit(Level level, double x, double y, double z) {
        // Spawn flash particle for impact
        level.addParticle(
            ParticleTypes.FLASH,
            x, y, z,
            0, 0, 0
        );

        // Spawn small burst of END_ROD particles
        for (int i = 0; i < 5; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.3;
            double oy = (level.random.nextDouble() - 0.5) * 0.3;
            double oz = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(
                ParticleTypes.END_ROD,
                x, y, z, ox, oy, oz
            );
        }

        // Play sword clang sound
        level.playLocalSound(
            x, y, z,
            HighPriestSounds.SWORD_CLANG.get(),
            SoundSource.NEUTRAL,
            0.5f, 1.0f, false
        );
    }

    /**
     * Renders the ambient sound effect while barrier exists.
     *
     * @param level The level to play sound in
     * @param x     The x position of the barrier
     * @param y     The y position of the barrier
     * @param z     The z position of the barrier
     */
    private static void renderAmbient(Level level, double x, double y, double z) {
        // Play respawn anchor ambient sound
        level.playLocalSound(
            x, y, z,
            SoundEvents.RESPAWN_ANCHOR_AMBIENT,
            SoundSource.BLOCKS,
            0.3f, 1.0f, false
        );
    }
}
