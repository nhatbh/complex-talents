package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnVaporizeReactionPacket;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vaporize Reaction (Fire + Aqua)
 * Deals 1.5 hearts (3.0 damage) of elemental damage
 */
public class VaporizeReaction implements IReactionStrategy {

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply damage
        DamageSource damageSource = target.level().damageSources().magic();
        target.hurt(damageSource, damage);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnVaporizeReactionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 3.0f * mastery * multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.VAPORIZE;
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