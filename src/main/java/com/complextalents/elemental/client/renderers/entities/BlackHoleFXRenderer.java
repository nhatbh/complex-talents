package com.complextalents.elemental.client.renderers.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Particle effect renderer for Black Hole entity
 * Creates a circular suction effect using ender particles
 * Ender particles spawn in a circle and move toward the center
 * Renders every tick the entity exists
 */
public class BlackHoleFXRenderer {


    /**
     * Renders the black hole suction particle effect
     * Creates ender particles in a circle that move toward the center
     *
     * @param level The level
     * @param pos The position of the Black Hole
     * @param isImploding Whether the black hole is imploding (affects particle behavior)
     */
    public static void render(Level level, Vec3 center, boolean isImploding) {

          if (!level.isClientSide) return;

    RandomSource random = level.getRandom();
    int count = 20;

    for (int i = 0; i < count; i++) {

        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.6 + random.nextDouble();

        Vec3 pos = center.add(
                Math.cos(angle) * radius,
                random.nextGaussian() * 0.1,
                Math.sin(angle) * radius
        );

        // Direction toward center
        Vec3 toCenter = center.subtract(pos).normalize();

        // Tangential (perpendicular) vector
        Vec3 tangent = new Vec3(-toCenter.z, 0, toCenter.x);

        double inwardSpeed = -2;
        double swirlSpeed = 3;

        Vec3 velocity = toCenter.scale(inwardSpeed)
                .add(tangent.scale(swirlSpeed));

        level.addParticle(
                ParticleTypes.PORTAL,
                pos.x, pos.y, pos.z,
                velocity.x, velocity.y, velocity.z
        );
    }
    }
}
