package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PermafrostFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Ice particles in 2x2 circle on ground
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");

        // Create 2x2 circular frost pattern on ground
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                double centerX = pos.x + (x * 1.0);
                double centerZ = pos.z + (z * 1.0);

                for (int i = 0; i < 15; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double radius = 0.3 + level.random.nextDouble() * 0.4;

                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    double offsetY = 0.05;

                    ParticleOptions particle = level.random.nextBoolean() ? iceParticle : snowflakeParticle;
                    level.addParticle(particle,
                        centerX + offsetX, pos.y + offsetY, centerZ + offsetZ,
                        0, 0.05, 0);
                }
            }
        }

        // Ice cracking sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.GLASS_BREAK, 1, 0.8f, 0.9f);
    }
}
