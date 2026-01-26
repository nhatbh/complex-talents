package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementFX;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.util.IronParticleHelper;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Melt Reaction (Fire + Ice)
 * Deals 1.5 hearts (3.0 damage) of elemental damage
 */
public class MeltReaction implements IReactionStrategy {

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply damage
        DamageSource damageSource = target.level().damageSources().magic();
        target.hurt(damageSource, damage);

        // Spawn particles
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        spawnReactionParticles(context.getLevel(), pos);
    }

    private void spawnReactionParticles(net.minecraft.server.level.ServerLevel level, Vec3 pos) {
        // Use shared explosion burst effect
        ParticleOptions particle = IronParticleHelper.getIronParticle("acid");
        ElementFX.spawnExplosionBurst(level, pos, particle, ParticleTypes.DRIPPING_WATER);
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // Base damage: 1.5 hearts = 3.0 damage
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 3.0f*mastery*multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        // Can trigger if target is a valid living entity
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.MELT;
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