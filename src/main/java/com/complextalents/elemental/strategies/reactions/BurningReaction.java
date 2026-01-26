package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnBurningReactionPacket;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Burning Reaction (Fire + Nature)
 * Deals 0.5 hearts damage and applies burning effect
 */
public class BurningReaction implements IReactionStrategy {

    private static final int BURNING_DURATION_TICKS = 60; // 3 seconds

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply damage
        DamageSource damageSource = target.level().damageSources().magic();
        target.hurt(damageSource, damage);

        // Apply burning effect
        MobEffectInstance burningEffect = new MobEffectInstance(
            ElementalEffects.BURNING.get(),
            BURNING_DURATION_TICKS,
            0,
            false,
            true,
            true
        );
        target.addEffect(burningEffect);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnBurningReactionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 1.0f * mastery * multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.BURNING;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean consumesStacks() {
        return true;
    }
}
