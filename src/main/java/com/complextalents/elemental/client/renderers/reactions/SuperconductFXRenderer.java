package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SuperconductFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Bright flash
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.0;
            double offsetY = level.random.nextDouble() * 1.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.0;

            level.addParticle(ParticleTypes.FLASH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0);
        }

        // Shockwave particles spreading outward
        ParticleOptions zapParticle = IronParticleHelper.getIronParticle("zap");
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + (ring * 0.4);
            int particlesPerRing = 16 + ring * 8;

            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (double) i / particlesPerRing * Math.PI * 2;

                double offsetX = Math.cos(angle) * radius;
                double offsetY = 0.1 + (ring * 0.15);
                double offsetZ = Math.sin(angle) * radius;

                level.addParticle(zapParticle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    Math.cos(angle) * 0.2, 0.1, Math.sin(angle) * 0.2);
            }
        }

        // Electric spark sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.TRIDENT_THUNDER, 1, 1.0f, 1.2f);
    }
}
