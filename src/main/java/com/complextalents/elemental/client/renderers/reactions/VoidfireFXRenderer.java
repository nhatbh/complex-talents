package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VoidfireFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Dragon breath flame burst
        ParticleOptions dragonFlameParticle = IronParticleHelper.getIronParticle("dragon_breath");
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI * 0.6;
            double speed = 0.3 + level.random.nextDouble() * 0.5;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.2;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            level.addParticle(dragonFlameParticle,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ);
        }

        // Ender particles
        for (int i = 0; i < 25; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
            double offsetY = level.random.nextDouble() * 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;

            level.addParticle(ParticleTypes.DRAGON_BREATH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                (level.random.nextDouble() - 0.5) * 0.1,
                0.05 + level.random.nextDouble() * 0.1,
                (level.random.nextDouble() - 0.5) * 0.1);
        }

        // Ender teleport sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.ENDERMAN_TELEPORT, 2, 0.8f, 0.7f);
    }
}
