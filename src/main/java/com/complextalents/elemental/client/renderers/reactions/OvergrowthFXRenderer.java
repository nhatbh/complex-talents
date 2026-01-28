package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Visual effects renderer for the Overgrowth reaction (Lightning + Nature)
 * Creates a bio-energy effect similar to totem of undying with ember sparks
 * Combines green nature particles with yellow/lava particles for the "ember sparks"
 */
public class OvergrowthFXRenderer {

    public static void render(Level level, Vec3 pos) {
        // Nature/green particles representing the bio-energy
        ParticleOptions natureParticle = IronParticleHelper.getIronParticle("nature");

        // Create a swirling vortex of nature particles (totem-like effect)
        for (int i = 0; i < 50; i++) {
            double angle = (i / 50.0) * Math.PI * 2;
            double radius = 0.5 + (level.random.nextDouble() * 0.5);
            double height = level.random.nextDouble() * 2.5;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Upward spiral motion
            level.addParticle(natureParticle,
                pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                Math.cos(angle + Math.PI/2) * 0.1, 0.3, Math.sin(angle + Math.PI/2) * 0.1);
        }

        // Ember sparks - lava/fire particles rising up
        ParticleOptions lavaParticle = net.minecraft.core.particles.ParticleTypes.LAVA;
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            level.addParticle(lavaParticle,
                pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                0, 0.5 + level.random.nextDouble() * 0.3, 0);
        }

        // Flame particles for additional ember effect
        ParticleOptions flameParticle = net.minecraft.core.particles.ParticleTypes.FLAME;
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;

            level.addParticle(flameParticle,
                pos.x + offsetX, pos.y + 0.3, pos.z + offsetZ,
                (level.random.nextDouble() - 0.5) * 0.1, 0.4 + level.random.nextDouble() * 0.2, (level.random.nextDouble() - 0.5) * 0.1);
        }

        // Lightning sparks to show the lightning element
        ParticleOptions electricParticle = IronParticleHelper.getIronParticle("electric");
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            level.addParticle(electricParticle,
                pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                0, 0.2, 0);
        }

        // Play magical sound for the bio-energy infusing
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL,
            net.minecraft.sounds.SoundSource.HOSTILE,
            0.8f, 1.2f, false);

        // Secondary crackling sound for the lightning
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.HOSTILE,
            0.3f, 1.5f, false);
    }

    /**
     * Renders the explosion effect when the target dies with Unstable Bio-energy
     * Creates a burst effect combining totem visuals with ember explosion
     */
    public static void renderExplosion(Level level, Vec3 pos) {
        // Large burst of nature particles (totem effect)
        ParticleOptions natureParticle = IronParticleHelper.getIronParticle("nature");

        for (int i = 0; i < 60; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + level.random.nextDouble() * 0.5;

            level.addParticle(natureParticle,
                pos.x, pos.y + 1.0, pos.z,
                Math.cos(angle) * speed, 0.2, Math.sin(angle) * speed);
        }

        // Explosion of ember sparks (lava)
        ParticleOptions lavaParticle = net.minecraft.core.particles.ParticleTypes.LAVA;
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.4 + level.random.nextDouble() * 0.6;

            level.addParticle(lavaParticle,
                pos.x, pos.y + 0.5, pos.z,
                Math.cos(angle) * speed, 0.5 + level.random.nextDouble() * 0.5, Math.sin(angle) * speed);
        }

        // Flame burst
        ParticleOptions flameParticle = net.minecraft.core.particles.ParticleTypes.FLAME;
        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + level.random.nextDouble() * 0.4;

            level.addParticle(flameParticle,
                pos.x, pos.y + 0.5, pos.z,
                Math.cos(angle) * speed, 0.4 + level.random.nextDouble() * 0.4, Math.sin(angle) * speed);
        }

        // Electric burst from lightning
        ParticleOptions electricParticle = IronParticleHelper.getIronParticle("electric");
        for (int i = 0; i < 25; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.5 + level.random.nextDouble() * 0.5;

            level.addParticle(electricParticle,
                pos.x, pos.y + 0.5, pos.z,
                Math.cos(angle) * speed, 0.3, Math.sin(angle) * speed);
        }

        // Large smoke particles for explosion effect
        ParticleOptions smokeParticle = net.minecraft.core.particles.ParticleTypes.POOF;
        for (int i = 0; i < 10; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.0;

            level.addParticle(smokeParticle,
                pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                0, 0.1, 0);
        }
    }
}
