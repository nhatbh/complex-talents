package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BloomFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Nature/green particles blooming outward
        ParticleOptions natureParticle = IronParticleHelper.getIronParticle("nature");


        // Expanding nature ring
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.2 + level.random.nextDouble() * 0.3;

            level.addParticle(natureParticle,
                pos.x, pos.y + 0.3, pos.z,
                Math.cos(angle) * speed, 0.1, Math.sin(angle) * speed);
        }

        // Rising water droplets (Aqua element)
        ParticleOptions aquasphere = IronParticleHelper.getIronParticle("aquasphere");
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.0;

            level.addParticle(aquasphere,
                pos.x + offsetX, pos.y, pos.z + offsetZ,
                0, 0.3, 0);
        }

        // Blooming sound
        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.PLAYERS,
            0.5f, 1.2f, false);
    }
}
