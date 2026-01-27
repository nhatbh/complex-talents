package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.util.TeamHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Electro-Charged Reaction (Aqua + Lightning)
 * Deals immediate damage and applies a chain lightning AoE effect.
 * The effect zaps 3 nearby enemies every second for damage over time.
 * The DoT damage scales with the effect amplifier.
 */
public class ElectroChargedReaction implements IReactionStrategy {

    /**
     * Duration of the electro-charged effect in seconds.
     */
    private static final int BASE_EFFECT_DURATION = 5;

    /**
     * NBT key for storing the attacker username who applied this effect.
     * This allows the chain lightning to apply lightning stacks from the correct attacker.
     */
    private static final String NBT_ATTACKER_USERNAME = "ElectroChargedAttackerUsername";

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Check if target is an ally of the attacker - skip damage and effects if so
        if (context.getAttacker() != null && TeamHelper.isAlly(context.getAttacker(), target)) {
            return;
        }

        // Store attacker username in target's NBT for chain lightning to use later
        if (context.getAttacker() != null) {
            CompoundTag data = target.getPersistentData();
            data.putString(NBT_ATTACKER_USERNAME, context.getAttacker().getName().getString());
        }

        // Apply immediate damage with player attribution
        DamageSource damageSource;
        if (context.getAttacker() != null) {
            damageSource = target.level().damageSources().playerAttack(context.getAttacker());
        } else {
            damageSource = target.level().damageSources().lightningBolt();
        }
        target.hurt(damageSource, damage);

        // Calculate amplifier based on reaction damage
        // Every 2.0 damage = 1 amplifier level (min 0, max 4)
        int amplifier = Math.min(200, Math.max(0, (int) (damage / 2.0f)));

        // Apply electro-charged effect
        int durationTicks = BASE_EFFECT_DURATION * 20;
        target.addEffect(new MobEffectInstance(
            ElementalEffects.ELECTRO_CHARGED.get(),
            durationTicks,
            amplifier,
            false, // isAmbient
            true,  // visible
            true   // showIcon
        ));

        // Send particle effect packet to nearby clients
        // Note: Visual effect is handled by the ElectroChargedEffect when it zaps targets
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
        // Can trigger if target is a valid living entity
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.ELECTRO_CHARGED;
    }

    @Override
    public int getPriority() {
        return 10; // Standard priority
    }

    @Override
    public boolean consumesStacks() {
        return true; // Consumes elemental stacks when triggered
    }

    /**
     * Gets the attacker username from the target's NBT data.
     *
     * @param target The entity with the Electro Charged effect
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
     * Should be called when the Electro Charged effect expires.
     *
     * @param target The entity with the Electro Charged effect
     */
    public static void clearAttackerUsername(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(NBT_ATTACKER_USERNAME);
    }
}