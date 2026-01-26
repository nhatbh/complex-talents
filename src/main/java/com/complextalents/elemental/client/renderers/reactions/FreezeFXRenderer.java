package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FreezeFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Spherical ice shell formation
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");

        // Create spherical shell effect
        int rings = 5;
        for (int ring = 0; ring < rings; ring++) {
            double radius = 0.8 + (ring * 0.15);
            int particlesPerRing = 12 + ring * 4;

            for (int i = 0; i < particlesPerRing; i++) {
                double phi = Math.acos(1 - 2.0 * i / particlesPerRing);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;

                double offsetX = Math.cos(theta) * Math.sin(phi) * radius;
                double offsetY = Math.sin(theta) * Math.sin(phi) * radius + 0.5;
                double offsetZ = Math.cos(phi) * radius;

                ParticleOptions particle = level.random.nextBoolean() ? iceParticle : snowflakeParticle;
                level.addParticle(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0, 0, 0);
            }
        }

        // Ice freeze sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.GLASS_BREAK, 1, 1.0f, 0.8f);
    }
}
