package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnBurningReactionPacket;
import com.complextalents.util.TeamHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Burning Reaction (Fire + Nature)
 * Deals 0.5 hearts damage and applies burning effect
 */
public class BurningReaction implements IReactionStrategy {

    private static final int BURNING_DURATION_TICKS = 60; // 3 seconds

    /**
     * NBT key for storing the attacker username who applied this effect.
     * This allows the burning damage to be attributed to the correct attacker.
     */
    private static final String NBT_ATTACKER_USERNAME = "BurningAttackerUsername";

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Check if target is an ally of the attacker - skip damage and effects if so
        if (context.getAttacker() != null && TeamHelper.isAlly(context.getAttacker(), target)) {
            return;
        }

        // Store attacker username in target's NBT for burning effect to use later
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

    /**
     * Gets the attacker username from the target's NBT data.
     *
     * @param target The entity with the Burning effect
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
     * Should be called when the Burning effect expires.
     *
     * @param target The entity with the Burning effect
     */
    public static void clearAttackerUsername(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(NBT_ATTACKER_USERNAME);
    }
}
