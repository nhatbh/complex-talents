package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnFreezeReactionPacket;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Freeze Reaction (Ice + Aqua)
 * Encases target in ice, preventing movement.
 * Physical hits deal 2.5x damage and break the ice instantly.
 */
public class FreezeReaction implements IReactionStrategy {

    private static final int FREEZE_DURATION_TICKS = 40; // 2 seconds

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();

        // Apply freeze effect - no initial damage
        MobEffectInstance freezeEffect = new MobEffectInstance(
            ElementalEffects.FREEZE.get(),
            FREEZE_DURATION_TICKS,
            0,
            false,
            true,
            true
        );
        target.addEffect(freezeEffect);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnFreezeReactionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        return 0.0f;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.FREEZE;
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
