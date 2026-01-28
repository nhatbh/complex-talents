package com.complextalents.elemental.client.renderers.reactions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for Flux reaction particle effects
 * Creates a singularity burst effect with lightning and ender particles
 */
public class FluxFXRenderer {

    public static void render(Level level, Vec3 pos) {
        // Lightning particles burst from center (electricity crackling)
        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;

            // Radial burst pattern
            double horizontalSpeed = Math.cos(angle) * (0.3 + level.random.nextDouble() * 0.4);
            double verticalSpeed = 0.5 + level.random.nextDouble() * 0.8;
            double depthSpeed = Math.sin(angle) * (0.3 + level.random.nextDouble() * 0.4);

            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z,
                horizontalSpeed, verticalSpeed, depthSpeed);
        }

        // Ender particles swirling inward (singularity forming)
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double radius = 2.0 + level.random.nextDouble() * 1.0;

            double px = pos.x + Math.cos(angle) * radius;
            double pz = pos.z + Math.sin(angle) * radius;
            double py = pos.y + (level.random.nextDouble() - 0.5) * 1.0;

            // Velocity toward center (pulling in effect)
            Vec3 toCenter = pos.subtract(new Vec3(px, py, pz)).normalize();
            double speed = 0.2 + level.random.nextDouble() * 0.15;

            level.addParticle(ParticleTypes.DRAGON_BREATH,
                px, py, pz,
                toCenter.x * speed, toCenter.y * speed * 0.3, toCenter.z * speed);
        }

        // Portal particles for extra ender effect
        for (int i = 0; i < 15; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 1.5 + level.random.nextDouble() * 0.5;

            double px = pos.x + Math.cos(angle) * radius;
            double pz = pos.z + Math.sin(angle) * radius;
            double py = pos.y + (level.random.nextDouble() - 0.5) * 0.5;

            level.addParticle(ParticleTypes.PORTAL,
                px, py, pz,
                (pos.x - px) * 0.1, 0.1, (pos.z - pz) * 0.1);
        }

        // Play sound for the flux reaction
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
            net.minecraft.sounds.SoundSource.HOSTILE,
            0.8f, 0.8f, false);

        // Secondary electric crackle sound
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.HOSTILE,
            0.5f, 1.5f, false);
    }
}
