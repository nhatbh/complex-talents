package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FreezeFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Ice particle ring around the target
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");

        // Create horizontal ring effect at different heights
        int rings = 4;
        for (int ring = 0; ring < rings; ring++) {
            double radius = 0.8;
            double yOffset = 0.2 + (ring * 0.3);
            int particlesPerRing = 20;

            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (double) i / particlesPerRing * Math.PI * 2;

                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                ParticleOptions particle = level.random.nextBoolean() ? iceParticle : snowflakeParticle;
                level.addParticle(particle,
                    pos.x + offsetX, pos.y + yOffset, pos.z + offsetZ,
                    0, 0, 0);
            }
        }

        // Ice freeze sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.GLASS_BREAK, 1, 1.0f, 0.8f);
    }
}
