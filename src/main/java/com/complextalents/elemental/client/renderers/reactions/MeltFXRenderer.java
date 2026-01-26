package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MeltFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Flash effect
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = level.random.nextDouble() * 1.5;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            level.addParticle(ParticleTypes.FLASH,
                pos.x + offsetX, pos.y + level.random.nextDouble() * 1.5, pos.z + offsetZ,
                0, 0, 0);
        }

        // Ice explosion
        ParticleOptions iceParticle = IronParticleHelper.getIronParticle("ice");
        ParticleOptions snowflakeParticle = IronParticleHelper.getIronParticle("snowflake");
        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + level.random.nextDouble() * 0.4;
            double offsetX = Math.cos(angle) * speed;
            double offsetZ = Math.sin(angle) * speed;
            double offsetY = level.random.nextDouble() * 0.5;
            ParticleOptions particle = level.random.nextBoolean() ? iceParticle : snowflakeParticle;
            level.addParticle(particle,
                pos.x, pos.y + offsetY, pos.z,
                offsetX, 0.1 + level.random.nextDouble() * 0.2, offsetZ);
        }

        // Stacked explosion sound
        SoundHelper.playStackedExplosionSound(level, pos, 2, 1.0f);
    }
}
