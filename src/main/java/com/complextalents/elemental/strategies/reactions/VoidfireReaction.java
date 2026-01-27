package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnVoidfireReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Voidfire Reaction (Fire + Ender)
 * Deals 2.5 hearts damage and applies marked for death effect
 */
public class VoidfireReaction implements IReactionStrategy {

    private static final int MARKED_DURATION_TICKS = 80; // 4 seconds

    /**
     * NBT key for storing the attacker username who applied this effect.
     * This allows the marked for death damage to be attributed to the correct attacker.
     */
    private static final String NBT_ATTACKER_USERNAME = "VoidfireAttackerUsername";

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Check if target is an ally of the attacker - skip damage and effects if so
        if (context.getAttacker() != null && TeamHelper.isAlly(context.getAttacker(), target)) {
            return;
        }

        // Store attacker username in target's NBT for marked for death effect to use later
        if (context.getAttacker() != null) {
            CompoundTag data = target.getPersistentData();
            data.putString(NBT_ATTACKER_USERNAME, context.getAttacker().getName().getString());
        }

        // Apply damage with player attribution
        DamageSource damageSource;
        if (context.getAttacker() != null) {
            damageSource = target.level().damageSources().playerAttack(context.getAttacker());
        } else {
            damageSource = target.level().damageSources().magic();
        }
        target.hurt(damageSource, damage);

        // Apply marked for death effect
        MobEffectInstance markedEffect = new MobEffectInstance(
            ElementalEffects.MARKED_FOR_DEATH.get(),
            MARKED_DURATION_TICKS,
            0,
            false,
            true,
            true
        );
        target.addEffect(markedEffect);

        // Send particle effect packet to nearby clients
        PacketHandler.sendToNearby(
            new SpawnVoidfireReactionPacket(target.position()),
            context.getLevel(),
            target.position()
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 5.0f * mastery * multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.VOIDFIRE;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean consumesStacks() {
        return true;
    }

    /**
     * Gets the attacker username from the target's NBT data.
     *
     * @param target The entity with the Marked for Death effect
     * @return The attacker username, or null if not found
     */
    public static String getAttackerUsername(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (data.contains(NBT_ATTACKER_USERNAME)) {
            return data.getString(NBT_ATTACKER_USERNAME);
        }
        return null;
    }

    /**
     * Removes the attacker username from the target's NBT data.
     * Should be called when the Marked for Death effect expires.
     *
     * @param target The entity with the Marked for Death effect
     */
    public static void clearAttackerUsername(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(NBT_ATTACKER_USERNAME);
    }
}
