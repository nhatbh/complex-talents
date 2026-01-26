package com.complextalents.elemental.client.renderers.reactions;

import com.complextalents.util.IronParticleHelper;
import com.complextalents.util.SoundHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ElectroChargedFXRenderer {
    public static void render(Level level, Vec3 pos) {
        // Bright flash at center
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
            double offsetY = level.random.nextDouble() * 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;

            level.addParticle(ParticleTypes.FLASH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0);
        }

        // Electricity particles radiating outward
        ParticleOptions zapParticle = IronParticleHelper.getIronParticle("zap");
        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI * 0.8;
            double speed = 0.4 + level.random.nextDouble() * 0.6;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed + 0.2;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            level.addParticle(zapParticle,
                pos.x, pos.y, pos.z,
                offsetX, offsetY, offsetZ);
        }

        // Electric spark sound
        SoundHelper.playStackedSound(level, pos, SoundEvents.TRIDENT_THUNDER, 2, 1.0f, 1.3f);
    }

    /**
     * Renders a chain lightning effect from source to target position
     */
    public static void renderChain(Level level, Vec3 source, Vec3 target) {
        ParticleOptions zapParticle = IronParticleHelper.getIronParticle("electricity");

        // Create lightning bolt path
        Vec3 direction = target.subtract(source);
        double distance = direction.length();
        direction = direction.normalize();

        int segments = (int) (distance * 5); // More segments for longer distances

        for (int i = 0; i < segments; i++) {
            // Move along the main direction with random offset
            double t = (double) i / segments;
            double progress = t * distance;

            // Add jagged randomness
            double offsetMagnitude = 0.15 * (1.0 - Math.abs(t - 0.5) * 2); // More offset in middle
            double offsetX = (level.random.nextDouble() - 0.5) * offsetMagnitude;
            double offsetY = (level.random.nextDouble() - 0.5) * offsetMagnitude;
            double offsetZ = (level.random.nextDouble() - 0.5) * offsetMagnitude;

            Vec3 segmentPos = source.add(
                direction.x * progress + offsetX,
                direction.y * progress + offsetY,
                direction.z * progress + offsetZ
            );

            // Spawn particles along the path
            level.addParticle(ParticleTypes.FLASH, segmentPos.x, segmentPos.y, segmentPos.z, 0, 0, 0);
            level.addParticle(zapParticle, segmentPos.x, segmentPos.y, segmentPos.z, 0, 0, 0);
        }

        // Play sound at both positions
        SoundHelper.playStackedSound(level, source, SoundEvents.TRIDENT_THUNDER, 1, 0.8f, 1.2f);
    }
}
