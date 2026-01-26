package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FractureFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Bright flash
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
            double offsetY = level.random.nextDouble() * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;

            level.addParticle(ParticleTypes.FLASH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0);
        }

        // Ice and ender particle explosion
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        for (int i = 0; i < 35; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.3 + level.random.nextDouble() * 0.5;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.3;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            // Alternate between ice particles and ender particles
            if (level.random.nextBoolean()) {
                level.addParticle(iceParticle,
                    pos.x, pos.y, pos.z,
                    offsetX, offsetY, offsetZ);
            } else {
                level.addParticle(ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z,
                    offsetX * 0.5, offsetY * 0.5, offsetZ * 0.5);
            }
        }

        // Glass shattering sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.GLASS_BREAK, 2, 1.2f, 1.0f);
    }
}
