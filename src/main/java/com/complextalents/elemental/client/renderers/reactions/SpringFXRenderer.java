package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for Spring reaction particle effects
 * Creates a magical spring geyser effect with water and ender particles bursting upward
 */
public class SpringFXRenderer {

    public static void render(Level level, Vec3 pos) {
        // Water splash particles (Aqua element) - geyser burst from single point
        ParticleOptions aquasphere = IronParticleHelper.getIronParticle("aquasphere");

        // Cone burst - all particles from center point, faster in middle
        for (int i = 0; i < 50; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            // Bias toward center of cone for faster particles in middle
            double distanceFromCenter = level.random.nextDouble();
            distanceFromCenter = distanceFromCenter * distanceFromCenter; // Square to bias toward center

            // Narrower cone angle for center (faster), wider for edges (slower)
            double coneAngle = (Math.PI / 6) + (distanceFromCenter * Math.PI / 4); // 30-75 degrees from vertical

            // Vertical speed - faster in center of cone
            double verticalSpeed = 1.2 - (distanceFromCenter * 0.6); // 1.2 to 0.6
            // Horizontal speed based on cone angle
            double horizontalSpeed = Math.tan(coneAngle) * verticalSpeed;

            level.addParticle(aquasphere,
                pos.x, pos.y, pos.z, // Single point source
                Math.cos(angle) * horizontalSpeed, verticalSpeed, Math.sin(angle) * horizontalSpeed);
        }

        // Secondary splash ring - water bursting outward at base
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + level.random.nextDouble() * 0.4;

            level.addParticle(aquasphere,
                pos.x, pos.y + 0.1, pos.z,
                Math.cos(angle) * speed, 0.15, Math.sin(angle) * speed);
        }

        // Spring geyser sound
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.PLAYER_SPLASH,
            net.minecraft.sounds.SoundSource.PLAYERS,
            0.8f, 1.2f, false);

        // Magical chime sound for the ender element
        level.playLocalSound(pos.x, pos.y + 0.5, pos.z,
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
            net.minecraft.sounds.SoundSource.PLAYERS,
            0.5f, 1.5f, false);
    }
}
