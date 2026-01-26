package com.complextalents.elemental.strategies.reactions;

import com.complextalents.config.ElementalReactionConfig;
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
 * Overload Reaction (Fire + Lightning)
 * Deals 1.0 heart (2.0 damage) of elemental damage with knockback
 */
public class OverloadReaction implements IReactionStrategy {

    private static final double KNOCKBACK_STRENGTH = 1.5; // Strong knockback
    private static final double VERTICAL_BOOST = 0.4; // Upward component for explosion effect

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply damage
        DamageSource damageSource = target.level().damageSources().magic();
        target.hurt(damageSource, damage);

        // Apply knockback effect
        applyKnockback(context);

        // Spawn particles
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        spawnReactionParticles(context.getLevel(), pos);
    }

    /**
     * Applies knockback to the target, simulating an explosion
     */
    private void applyKnockback(ReactionContext context) {
        LivingEntity target = context.getTarget();
        LivingEntity attacker = context.getAttacker();

        if (attacker != null) {
            // Calculate knockback direction (away from attacker)
            Vec3 attackerPos = attacker.position();
            Vec3 targetPos = target.position();
            Vec3 knockbackDirection = targetPos.subtract(attackerPos).normalize();

            // Apply horizontal knockback
            double horizontalStrength = KNOCKBACK_STRENGTH;
            target.setDeltaMovement(
                target.getDeltaMovement().add(
                    knockbackDirection.x * horizontalStrength,
                    VERTICAL_BOOST, // Add upward boost for explosion effect
                    knockbackDirection.z * horizontalStrength
                )
            );

            // Mark entity as needing velocity update
            target.hurtMarked = true;

            // Push the entity to ensure knockback is applied
            target.push(knockbackDirection.x * 0.5, 0.1, knockbackDirection.z * 0.5);
        } else {
            // If no attacker, apply random knockback (explosion at location)
            double angle = Math.random() * Math.PI * 2;
            double horizontalStrength = KNOCKBACK_STRENGTH * 0.75; // Slightly weaker without attacker

            target.setDeltaMovement(
                target.getDeltaMovement().add(
                    Math.cos(angle) * horizontalStrength,
                    VERTICAL_BOOST,
                    Math.sin(angle) * horizontalStrength
                )
            );

            target.hurtMarked = true;
        }
    }

    private void spawnReactionParticles(net.minecraft.server.level.ServerLevel level, Vec3 pos) {
        if (!ElementalReactionConfig.enableParticleEffects.get()) return;

        // Use shared explosion burst effect
        ParticleOptions fireParticle = IronParticleHelper.getIronParticle("fire");
        ElementFX.spawnExplosionBurst(level, pos, fireParticle, ParticleTypes.FLAME);

        // Add explosion emitter
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0.0);

        // Add overload-specific effects with quality scaling
        var quality = ElementalReactionConfig.particleQuality.get();

        // Fire burst
        ElementFX.spawnRandomBurst(level, pos, "fire", ParticleTypes.FLAME, 40, 1.5);

        // Add smoke effect
        int smokeCount = quality.scale(25);
        for (int i = 0; i < smokeCount; i++) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                pos.y + 0.5,
                pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                1,
                (level.random.nextDouble() - 0.5) * 0.4,
                level.random.nextDouble() * 0.3,
                (level.random.nextDouble() - 0.5) * 0.4,
                0.02);
        }
    }


    @Override
    public float calculateDamage(ReactionContext context) {
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 2.0f*mastery*multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        // Can trigger if target is a valid living entity
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.OVERLOADED;
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