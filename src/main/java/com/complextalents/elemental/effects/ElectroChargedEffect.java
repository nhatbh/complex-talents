package com.complextalents.elemental.effects;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.events.ElementalDamageEvent;
import com.complextalents.elemental.strategies.reactions.ElectroChargedReaction;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnElectroChargedReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * Electro-Charged Effect - Chain lightning AoE damage from Electro-Charged Reaction
 * Periodically zaps nearby enemies in a 3-block radius for lightning damage per second.
 * The number of targets scales with the amplifier: base 3 targets + 1 per amplifier level.
 * Each zap sends a packet to render chain lightning particles from target to enemies.
 *
 * Damage and target count are calculated based on the effect amplifier:
 * - Amplifier 0: 1.0 damage/second to 3 targets
 * - Amplifier 1: 2.0 damage/second to 4 targets
 * - Amplifier 2: 3.0 damage/second to 5 targets
 * - etc.
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
     * Base number of nearby enemies to zap each tick at amplifier 0.
     * Each amplifier level adds 1 additional target.
     */
    private static final int BASE_ZAP_TARGET_COUNT = 3;

    public ElectroChargedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Only apply damage on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Get the attacker username from NBT
        String attackerUsername = ElectroChargedReaction.getAttackerUsername(entity);
        ServerPlayer attacker = null;

        // Retrieve the attacker from the level if username exists and server is available
        if (attackerUsername != null && entity.getServer() != null) {
            attacker = entity.getServer().getPlayerList().getPlayerByName(attackerUsername);
        }

        // Calculate damage: base * (amplifier + 1)
        // Amplifier 0 = 1.0 damage, Amplifier 1 = 2.0 damage, Amplifier 2 = 3.0 damage, etc.
        float damage = BASE_DAMAGE_PER_SECOND * (amplifier + 1);

        // Calculate zap target count: base + 1 per amplifier level
        // Amplifier 0 = 3 targets, Amplifier 1 = 4 targets, Amplifier 2 = 5 targets, etc.
        int maxTargets = BASE_ZAP_TARGET_COUNT + amplifier;

        // Find nearby entities to zap
        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(ZAP_RADIUS, ZAP_RADIUS, ZAP_RADIUS),
            nearby -> nearby != entity && nearby.isAlive()
        );

        // Zap up to maxTargets nearby entities
        int targetsZapped = 0;
        for (LivingEntity target : nearbyEntities) {
            if (targetsZapped >= maxTargets) {
                break;
            }

            // Skip allies - don't zap friendly targets
            if (attacker != null && TeamHelper.isAlly(attacker, target)) {
                continue;
            }

            // Fire ElementalDamageEvent to apply lightning stack
            // Use the stored attacker as source, or fall back to the entity with the effect
            LivingEntity eventSource = attacker != null ? attacker : entity;
            ElementalDamageEvent elementalEvent = new ElementalDamageEvent(
                target,
                eventSource,
                ElementType.LIGHTNING,
                damage
            );
            MinecraftForge.EVENT_BUS.post(elementalEvent);

            // Deal lightning damage to the target with player attribution
            DamageSource damageSource;
            if (attacker != null) {
                damageSource = target.level().damageSources().playerAttack(attacker);
            } else {
                damageSource = target.level().damageSources().lightningBolt();
            }
            target.hurt(damageSource, damage);

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

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up NBT data when effect is removed
        ElectroChargedReaction.clearAttackerUsername(entity);
    }
}
