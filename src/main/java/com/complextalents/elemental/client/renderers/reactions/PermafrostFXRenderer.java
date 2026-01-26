package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PermafrostFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Ice particles in circle on ground
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");

        // Create circular frost pattern on ground
        int circles = 3;
        for (int circle = 0; circle < circles; circle++) {
            double radius = 0.6 + (circle * 0.4);
            int particlesPerCircle = 20 + circle * 8;

            for (int i = 0; i < particlesPerCircle; i++) {
                double angle = (double) i / particlesPerCircle * Math.PI * 2;

                double offsetX = Math.cos(angle) * radius;
                double offsetY = 0.05;
                double offsetZ = Math.sin(angle) * radius;

                ParticleOptions particle = level.random.nextBoolean() ? iceParticle : snowflakeParticle;
                level.addParticle(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0, 0.05, 0);
            }
        }

        // Ice cracking sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.GLASS_BREAK, 1, 0.8f, 0.9f);
    }
}
