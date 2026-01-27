package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnMeltReacionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Melt Reaction (Fire + Ice)
 * Deals 2.0 hearts (4.0 damage) of elemental damage
 */
public class MeltReaction implements IReactionStrategy {

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

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnMeltReacionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // Base damage: 2.0 hearts = 4.0 damage
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 4.0f * mastery * multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.MELT;
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
