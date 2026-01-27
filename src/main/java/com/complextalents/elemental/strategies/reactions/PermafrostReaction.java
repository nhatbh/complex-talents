package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnPermafrostReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Permafrost Reaction (Ice + Nature)
 * Deals 0.5 hearts damage and roots the target.
 */
public class PermafrostReaction implements IReactionStrategy {

    private static final int PERMAFROST_DURATION_TICKS = 40; // 2 seconds

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Check if target is an ally of the attacker - skip damage and effects if so
        if (context.getAttacker() != null && TeamHelper.isAlly(context.getAttacker(), target)) {
            return;
        }

        // Apply damage with player attribution
        DamageSource damageSource;
        if (context.getAttacker() != null) {
            damageSource = target.level().damageSources().playerAttack(context.getAttacker());
        } else {
            damageSource = target.level().damageSources().magic();
        }
        target.hurt(damageSource, damage);

        MobEffectInstance permafrostEffect = new MobEffectInstance(
            ElementalEffects.PERMAFROST.get(),
            PERMAFROST_DURATION_TICKS,
            0,
            false,
            true,
            true
        );
        target.addEffect(permafrostEffect);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnPermafrostReactionPacket(target.position()),
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
        return ElementalReaction.PERMAFROST;
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
