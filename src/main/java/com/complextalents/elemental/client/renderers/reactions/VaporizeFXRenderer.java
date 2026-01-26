package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VaporizeFXRenderer {
    public static void renderParticles(Level level, Vec3 pos) {
        // Steam cloud in 3x3 region
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int i = 0; i < 8; i++) {
                    double offsetX = x + (level.random.nextDouble() - 0.5) * 0.8;
                    double offsetZ = z + (level.random.nextDouble() - 0.5) * 0.8;
                    double offsetY = level.random.nextDouble() * 1.0;
                    level.addParticle(ParticleTypes.CLOUD,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        (level.random.nextDouble() - 0.5) * 0.05,
                        0.05 + level.random.nextDouble() * 0.05,
                        (level.random.nextDouble() - 0.5) * 0.05);
                }
            }
        }

        // Steam hiss sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.FIRE_EXTINGUISH, 2, 1.0f, 0.8f);
    }
}