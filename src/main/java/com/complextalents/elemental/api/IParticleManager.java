package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Interface for managing particle effects for elemental reactions.
 * Separates visual effects from game logic.
 */
public interface IParticleManager {

    /**
     * Spawns particles for an elemental reaction.
     *
     * @param level The server level
     * @param position The position to spawn particles
     * @param reaction The reaction type
     */
    void spawnReactionParticles(ServerLevel level, Vec3 position, ElementalReaction reaction);

    /**
     * Spawns particles for an element application.
     *
     * @param level The server level
     * @param position The position to spawn particles
     * @param element The element type
     * @param stackCount The number of stacks
     */
    void spawnElementParticles(ServerLevel level, Vec3 position, ElementType element, int stackCount);

    /**
     * Spawns continuous particles for an ongoing effect.
     *
     * @param level The server level
     * @param entity The entity to spawn particles around
     * @param reaction The reaction causing the effect
     * @param intensity The intensity of the effect (affects particle count)
     */
    void spawnContinuousParticles(ServerLevel level, LivingEntity entity,
                                 ElementalReaction reaction, float intensity);

    /**
     * Spawns area effect particles.
     *
     * @param level The server level
     * @param center The center of the area
     * @param radius The radius of the area
     * @param reaction The reaction type
     */
    void spawnAreaParticles(ServerLevel level, Vec3 center, double radius,
                          ElementalReaction reaction);

    /**
     * Spawns custom particles.
     *
     * @param level The server level
     * @param particleType The particle type
     * @param position The position
     * @param count Number of particles
     * @param offset Position offset for spread
     * @param speed Particle speed
     */
    void spawnCustomParticles(ServerLevel level, ParticleOptions particleType,
                            Vec3 position, int count, Vec3 offset, double speed);

    /**
     * Spawns a burst of particles for impact effects.
     *
     * @param level The server level
     * @param position The impact position
     * @param element The element type
     * @param power The power of the burst (affects particle count and spread)
     */
    void spawnBurstParticles(ServerLevel level, Vec3 position,
                           ElementType element, float power);

    /**
     * Spawns trail particles between two points.
     *
     * @param level The server level
     * @param start The start position
     * @param end The end position
     * @param element The element type
     * @param density Particle density (particles per block)
     */
    void spawnTrailParticles(ServerLevel level, Vec3 start, Vec3 end,
                           ElementType element, float density);

    /**
     * Spawns spiral particles for vortex effects.
     *
     * @param level The server level
     * @param center The center of the spiral
     * @param radius The radius of the spiral
     * @param height The height of the spiral
     * @param element The element type
     * @param rotationSpeed The rotation speed
     */
    void spawnSpiralParticles(ServerLevel level, Vec3 center, double radius,
                            double height, ElementType element, float rotationSpeed);

    /**
     * Displays floating text for a reaction.
     *
     * @param level The server level
     * @param entity The entity to display text above
     * @param reaction The reaction type
     * @param damage The damage dealt (for display)
     */
    void displayReactionText(ServerLevel level, LivingEntity entity,
                           ElementalReaction reaction, float damage);

    /**
     * Displays floating text for element application.
     *
     * @param level The server level
     * @param entity The entity to display text above
     * @param element The element type
     * @param stackCount The stack count
     */
    void displayElementText(ServerLevel level, LivingEntity entity,
                          ElementType element, int stackCount);
}