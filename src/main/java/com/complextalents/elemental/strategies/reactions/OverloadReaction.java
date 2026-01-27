package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnOverloadReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Overload Reaction (Fire + Lightning)
 * Deals 1.5 hearts (3.0 damage) and creates an electrical explosion
 * Applies strong knockback to the target
 */
public class OverloadReaction implements IReactionStrategy {

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

        // Apply strong knockback away from attacker
        Vec3 targetPos = target.position();
        Vec3 direction;

        if (context.getAttacker() != null) {
            // Knockback away from attacker
            direction = targetPos.subtract(context.getAttacker().position()).normalize();
        } else {
            // If no attacker, knockback upward and outward randomly
            direction = new Vec3(target.getX() - target.xo, 0.5, target.getZ() - target.zo).normalize();
        }

        double knockbackStrength = 1.5; // Strong knockback

        target.setDeltaMovement(
            target.getDeltaMovement().x + direction.x * knockbackStrength,
            target.getDeltaMovement().y + knockbackStrength * 0.6, // Add upward lift
            target.getDeltaMovement().z + direction.z * knockbackStrength
        );
        target.hurtMarked = true; // Sync movement to client

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnOverloadReactionPacket(target.position()),
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
        return ElementalReaction.OVERLOADED;
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
