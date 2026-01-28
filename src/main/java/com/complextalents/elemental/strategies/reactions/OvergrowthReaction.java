package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.effects.UnstableBioEnergyEffect;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnOvergrowthReactionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Overgrowth Reaction (Lightning + Nature)
 * Afflicts the target with Unstable Bio-energy effect
 * If the target dies while the effect is active, they explode with bio-energy
 * The explosion deals 3*mastery magic damage to all enemies in 5 blocks
 * Visual effect is similar to totem of undying with ember sparks
 */
public class OvergrowthReaction implements IReactionStrategy {

    // Effect duration in seconds (20 ticks per second)
    private static final int EFFECT_DURATION_TICKS = 60; // 3 seconds

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        Vec3 pos = target.position();

        // Apply the Unstable Bio-energy effect to the target
        // The effect will trigger an explosion when the target dies
        MobEffectInstance effect = new MobEffectInstance(
            ElementalEffects.UNSTABLE_BIO_ENERGY.get(),
            EFFECT_DURATION_TICKS,
            0, // Amplifier 0 (base duration and damage)
            false, // No ambient particles
            true, // Show icon
            true  // Show particles
        );

        target.addEffect(effect);

        // Store the attacker username for damage attribution
        if (context.getAttacker() != null) {
            UnstableBioEnergyEffect.setAttackerUsername(target, context.getAttacker().getName().getString());
            UnstableBioEnergyEffect.setMastery(target, context.getElementalMastery());
        }

        // Send visual effect packet
        PacketHandler.sendToNearby(
            new SpawnOvergrowthReactionPacket(pos),
            context.getLevel(),
            pos
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // No direct damage - damage comes from the death explosion
        return 0.0f;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.OVERGROWTH;
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
