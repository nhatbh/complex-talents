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
        // Portal effect explosion
        ParticleOptions portalParticle = IronParticleHelper.getIronParticle("portal");
        for (int i = 0; i < 50; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.4 + level.random.nextDouble() * 0.6;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.3;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            level.addParticle(portalParticle,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ);
        }

        // Additional vanilla portal particles for extra effect
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 1.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            level.addParticle(ParticleTypes.PORTAL,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                (level.random.nextDouble() - 0.5) * 0.15,
                0.05 + level.random.nextDouble() * 0.1,
                (level.random.nextDouble() - 0.5) * 0.15);
        }

        // Ender teleport sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.ENDERMAN_TELEPORT, 2, 0.8f, 0.7f);
    }
}
