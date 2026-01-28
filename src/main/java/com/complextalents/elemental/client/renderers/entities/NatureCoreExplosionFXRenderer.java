package com.complextalents.elemental.client.renderers.entities;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Explosion effect renderer for Nature Core entity
 * Creates a burst of nature-themed explosion particles with ember effects
 */
public class NatureCoreExplosionFXRenderer {

    /**
     * Renders a nature-themed explosion effect
     * Features:
     * - Central burst of nature particles
     * - Outward ember explosion
     * - Expanding ring effect
     * - Vertical nature energy release
     * - Explosion sound
     *
     * @param level The level
     * @param pos The position of the explosion
     */
  public static void render(Level level, Vec3 pos) {
        // Explosion flash
        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;
            double distance = level.random.nextDouble() * 1.5;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * distance;
            double offsetY = Math.sin(verticalAngle) * distance + 0.5;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * distance;

            level.addParticle(ParticleTypes.FLASH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0);
        }

        // Fire sparks explosion
        ParticleOptions fireParticle = IronParticleHelper.getIronParticle("fire");
        ParticleOptions emberParticle = IronParticleHelper.getIronParticle("ember");
        for (int i = 0; i < 50; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.4 + level.random.nextDouble() * 0.6;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.4;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            ParticleOptions particle = level.random.nextBoolean() ? fireParticle : emberParticle;
            level.addParticle(particle,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ);
        }
        // Explosion sound
        SoundHelper.playStackedExplosionSound(level, pos, 3, 1.0f);
    }
}
