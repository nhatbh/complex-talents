package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementFX;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.util.IronParticleHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Burning Reaction (Fire + Nature)
 * Deals immediate damage and applies a damage over time (DoT) burning effect.
 * The DoT damage scales with the effect amplifier.
 */
public class BurningReaction implements IReactionStrategy {

    /**
     * Duration of the burning DoT effect in seconds.
     */
    private static final int BASE_BURN_DURATION = 5;

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply immediate damage
        DamageSource damageSource = target.level().damageSources().onFire();
        target.hurt(damageSource, damage);

        // Calculate amplifier based on reaction damage
        // Every 2.0 damage = 1 amplifier level (min 0, max 4)
        int amplifier = Math.min(200, Math.max(0, (int) (damage / 2.0f)));

        // Apply burning DoT effect
        int durationTicks = BASE_BURN_DURATION * 20;
        target.addEffect(new MobEffectInstance(
            ElementalEffects.BURNING.get(),
            durationTicks,
            amplifier,
            false, // isAmbient
            true,  // visible
            true   // showIcon
        ));

        // Spawn particles
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        spawnReactionParticles(context.getLevel(), pos);
    }

    private void spawnReactionParticles(net.minecraft.server.level.ServerLevel level, Vec3 pos) {
        // Use shared explosion burst effect with dragon fire
        ParticleOptions dragonFire = IronParticleHelper.getIronParticle("dragon_fire");
        ElementFX.spawnExplosionBurst(level, pos, dragonFire, ParticleTypes.FLAME);

        // Add burning-specific effects
        ElementFX.spawnSpiral(level, pos, "dragon_fire", ParticleTypes.FLAME, 3.6, 1.8);
        ElementFX.spawnEmbers(level, pos, 25);
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // Base damage: 0.5 hearts = 1.0 damage
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 1.0f*mastery*multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        // Can trigger if target is a valid living entity
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.BURNING;
    }

    @Override
    public int getPriority() {
        return 10; // Standard priority
    }

    @Override
    public boolean consumesStacks() {
        return true; // Consumes elemental stacks when triggered
    }
}