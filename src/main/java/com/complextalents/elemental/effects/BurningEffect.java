package com.complextalents.elemental.effects;

import com.complextalents.elemental.strategies.reactions.BurningReaction;
import com.complextalents.util.TeamHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Burning Effect - Damage over Time from Burning Reaction
 * Applies periodic fire damage with visual particle effects.
 *
 * Damage is calculated based on the effect amplifier.
 * Amplifier 0 = 0.5 damage/tick, Amplifier 1 = 1.0 damage/tick, etc.
 */
public class BurningEffect extends MobEffect {

    /**
     * Base damage per tick at amplifier 0.
     * 0.5 damage = 0.25 hearts per tick
     */
    public static final float BASE_DAMAGE_PER_TICK = 0.5f;

    public BurningEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Only apply damage on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Get the attacker username from NBT
        String attackerUsername = BurningReaction.getAttackerUsername(entity);
        ServerPlayer attacker = null;

        // Retrieve the attacker from the level if username exists and server is available
        if (attackerUsername != null && entity.getServer() != null) {
            attacker = entity.getServer().getPlayerList().getPlayerByName(attackerUsername);
        }

        // Skip if the entity with the effect is an ally of the attacker
        if (attacker != null && TeamHelper.isAlly(attacker, entity)) {
            return;
        }

        // Calculate damage: base * (amplifier + 1)
        // Amplifier 0 = 0.5 damage, Amplifier 1 = 1.0 damage, Amplifier 2 = 1.5 damage, etc.
        float damage = BASE_DAMAGE_PER_TICK * (amplifier + 1);

        // Apply fire damage with player attribution
        DamageSource damageSource;
        if (attacker != null) {
            damageSource = entity.level().damageSources().playerAttack(attacker);
        } else {
            damageSource = entity.level().damageSources().onFire();
        }
        entity.hurt(damageSource, damage);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply damage every second (20 ticks)
        // This makes the damage more readable and less spammy than every tick
        return duration % 20 == 0;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up NBT data when effect is removed
        BurningReaction.clearAttackerUsername(entity);
    }
}
