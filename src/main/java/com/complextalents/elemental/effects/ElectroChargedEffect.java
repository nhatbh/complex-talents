package com.complextalents.elemental.effects;

import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnElectroChargedReactionPacket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Electro-Charged Effect - Chain lightning AoE damage from Electro-Charged Reaction
 * Periodically zaps 3 nearby enemies in a 3-block radius for 1 damage per second.
 * Each zap sends a packet to render chain lightning particles from target to enemies.
 *
 * Damage is calculated based on the effect amplifier.
 * Amplifier 0 = 1.0 damage/second, Amplifier 1 = 2.0 damage/second, etc.
 */
public class ElectroChargedEffect extends MobEffect {

    /**
     * Base damage per second at amplifier 0.
     * 1.0 damage = 0.5 hearts per second
     */
    public static final float BASE_DAMAGE_PER_SECOND = 1.0f;

    /**
     * Radius to search for nearby enemies to zap
     */
    private static final double ZAP_RADIUS = 3.0;

    /**
     * Number of nearby enemies to zap each tick
     */
    private static final int ZAP_TARGET_COUNT = 3;

    public ElectroChargedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Only apply damage on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Calculate damage: base * (amplifier + 1)
        // Amplifier 0 = 1.0 damage, Amplifier 1 = 2.0 damage, Amplifier 2 = 3.0 damage, etc.
        float damage = BASE_DAMAGE_PER_SECOND * (amplifier + 1);

        // Find nearby entities to zap
        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(ZAP_RADIUS, ZAP_RADIUS, ZAP_RADIUS),
            nearby -> nearby != entity && nearby.isAlive()
        );

        // Zap up to ZAP_TARGET_COUNT nearby entities
        int targetsZapped = 0;
        for (LivingEntity target : nearbyEntities) {
            if (targetsZapped >= ZAP_TARGET_COUNT) {
                break;
            }

            // Deal lightning damage to the target
            target.hurt(entity.level().damageSources().lightningBolt(), damage);

            if (target.level() instanceof ServerLevel serverLevel) {
                // Send packet to render chain lightning from entity to target
                PacketHandler.sendToNearby(
                    new SpawnElectroChargedReactionPacket(entity.position(), target.position()),
                    serverLevel,
                    entity.position()
                );
            }
         

            targetsZapped++;
        }

        // Also deal damage to the entity itself
        entity.hurt(entity.level().damageSources().lightningBolt(), damage);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply damage every second (20 ticks)
        return duration % 20 == 0;
    }
}
