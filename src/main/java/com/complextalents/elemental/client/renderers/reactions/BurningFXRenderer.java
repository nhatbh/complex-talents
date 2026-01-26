package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BurningFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Fire spark explosion
        ParticleOptions fireParticle = IronParticleHelper.getIronParticle("fire");
        ParticleOptions emberParticle = IronParticleHelper.getIronParticle("ember");
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.3 + level.random.nextDouble() * 0.5;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.3;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            ParticleOptions particle = level.random.nextBoolean() ? fireParticle : emberParticle;
            level.addParticle(particle,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ);
        }

        // Add some flames lingering
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.FLAME,
                pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ,
                0, 0.05 + level.random.nextDouble() * 0.1, 0);
        }

        // Explosion sound
        SoundHelper.playStackedExplosionSound(level, pos, 2, 0.9f);
    }
}
