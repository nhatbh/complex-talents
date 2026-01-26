package com.complextalents.elemental.strategies.reactions;

import com.complextalents.TalentsMod;
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
 * Voidfire Reaction (Fire + Ender)
 * Type: Vulnerability Amplifier
 * Effect: The void destabilizes the target's form
 * Damage: Base 2.0 Hearts (4.0 damage)
 * Amplifier Effect: Marked for Death - Target takes +30% increased damage from all sources for 4 seconds
 * Visual: Purple/black aura pulses around the mob
 */
public class VoidfireReaction implements IReactionStrategy {
    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        float damage = calculateDamage(context);

        // Apply high burst damage
        DamageSource damageSource = target.level().damageSources().magic();
        target.hurt(damageSource, damage);

        // Apply "Marked for Death" vulnerability effect (4 seconds = 80 ticks)
        target.addEffect(new MobEffectInstance(
            ElementalEffects.MARKED_FOR_DEATH.get(),
            80, 0, false, true, true));

        // Spawn particles
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        spawnReactionParticles(context.getLevel(), pos);

        // Log for debugging
        TalentsMod.LOGGER.info("Voidfire reaction triggered on {} - {} damage dealt, marked for death applied",
            target.getName().getString(), damage);
    }

    private void spawnReactionParticles(net.minecraft.server.level.ServerLevel level, Vec3 pos) {
        // Use shared explosion burst effect with unstable ender
        ParticleOptions unstableEnder = IronParticleHelper.getIronParticle("unstable_ender");
        ElementFX.spawnExplosionBurst(level, pos, unstableEnder, ParticleTypes.PORTAL);

        // Add voidfire-specific effects
        ElementFX.spawnVortex(level, pos, "unstable_ender", ParticleTypes.PORTAL, 3.0, 1.5);
        ElementFX.spawnRandomBurst(level, pos, "dragon_fire", ParticleTypes.DRAGON_BREATH, 40, 1.5);
        ElementFX.spawnRandomBurst(level, pos, "portal", ParticleTypes.PORTAL, 30, 2.5);
        ElementFX.spawnRandomBurst(level, pos, "smoke", ParticleTypes.SMOKE, 20, 2.0);
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // Base damage: 2.0 hearts = 4.0 damage (High Burst)
        float mastery = context.getElementalMastery();
        float multiplier = context.getDamageMultiplier();
        return 4.0f*mastery*multiplier;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        // Can trigger if target is a valid living entity
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.VOIDFIRE;
    }

    @Override
    public int getPriority() {
        return 15; // Higher priority due to powerful effect
    }

    @Override
    public boolean consumesStacks() {
        return true; // Consumes elemental stacks when triggered
    }
}