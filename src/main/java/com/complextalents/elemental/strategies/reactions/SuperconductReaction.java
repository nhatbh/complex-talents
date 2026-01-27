package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnSuperconductReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Superconduct Reaction (Ice + Lightning)
 * Deals 0.5 hearts damage and reduces target's armor by 50% (Armor Shred).
 */
public class SuperconductReaction implements IReactionStrategy {

    private static final int ARMOR_SHRED_DURATION_TICKS = 120; // 6 seconds

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

        // Apply Superconduct effect for armor shred
        // The effect handles the -50% armor modifier via attribute modifiers
        MobEffectInstance superconductEffect = new MobEffectInstance(
            ElementalEffects.SUPERCONDUCT.get(),
            ARMOR_SHRED_DURATION_TICKS,
            0,
            false,
            true,
            true
        );
        target.addEffect(superconductEffect);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnSuperconductReactionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // Base damage: 0.5 hearts = 1.0 damage
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
        return ElementalReaction.SUPERCONDUCT;
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