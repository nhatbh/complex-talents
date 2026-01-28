package com.complextalents.elemental.client.renderers.entities;

import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Particle effect renderer for Nature Core entity
 * Creates a gentle green particle burst effect every tick
 */
public class NatureCoreFXRenderer {

    /**
     * Renders a gentle nature particle burst around the Nature Core
     * Called every tick on the client side
     *
     * @param level The level
     * @param pos The position of the Nature Core
     */
    public static void render(Level level, Vec3 pos) {
        // Get nature particle
        ParticleOptions natureParticle = IronParticleHelper.getIronParticle("nature");

        // Small upward-rising particles (nature magic floating upward)
        for (int i = 0; i < 3; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = level.random.nextDouble() * 0.3;

            level.addParticle(natureParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                offsetX * 0.1, 0.05 + level.random.nextDouble() * 0.05, offsetZ * 0.1);
        }

        // Subtle glow effect using item particle (slime block particles)
        if (level.random.nextFloat() < 0.2f) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

            level.addParticle(ParticleTypes.ITEM_SLIME,
                pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ,
                0, 0.02, 0);
        }
    }
}
