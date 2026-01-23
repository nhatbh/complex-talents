package com.complextalents.elemental.superreaction.reactions;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.effects.ModEffects;
import com.complextalents.elemental.superreaction.SuperReaction;
import com.complextalents.elemental.superreaction.SuperReactionTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Glacial Tomb - Ice Super-Reaction
 *
 * Tier 1 - Frostburst: Cold wave + freeze with mastery-scaled duration (1.5s base)
 * Tier 2 - Shattering Prism: Crystal transform + on next hit shatter for 150% AoE damage
 * Tier 3 - Stasis Field: Time-stop effect (Slowness 255, Weakness 255) in massive radius, mastery-scaled
 * Tier 4 - Cryo-Shatter: Apply debuff converting damage to 125% bonus Poise damage for 10s
 */
public class IceSuperReaction implements SuperReaction {

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1Frostburst(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2ShatteringPrism(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3StasisField(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4CryoShatter(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Ice Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Frostburst
     * Cold wave particle effect + freeze with mastery-scaled duration (1.5s base)
     */
    private void executeTier1Frostburst(ServerPlayer caster, LivingEntity target,
                                        ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 4.0f;

        // Calculate mastery-scaled freeze duration (1.5s base = 30 ticks)
        int baseDuration = 30;
        int freezeDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Apply freeze (Slowness 2, Mining Fatigue 2)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeDuration, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, freezeDuration, 1));
        target.setTicksFrozen(Math.min(target.getTicksFrozen() + freezeDuration, target.getTicksRequiredToFreeze() + 200));

        // Spawn cold wave particles
        spawnColdWaveParticles(serverLevel, pos, radius);

        TalentsMod.LOGGER.debug("Tier 1 Frostburst: {} damage, {}s freeze (mastery-scaled)",
                              baseDamage, freezeDuration / 20f);
    }

    /**
     * Tier 2 - Shattering Prism
     * Transform target visual (crystal overlay), on next hit shatter for 150% AoE damage
     */
    private void executeTier2ShatteringPrism(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();

        // Calculate mastery-scaled duration (5s base = 100 ticks)
        int baseDuration = 100;
        int prismDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Apply Shattering Prism effect
        // When target takes damage while this effect is active, it shatters for 150% AoE damage
        target.addEffect(new MobEffectInstance(ModEffects.SHATTERING_PRISM.get(), prismDuration, 0));

        // Store shatter damage in persistent data for damage event handler
        target.getPersistentData().putFloat("shatter_damage", baseDamage * 1.5f);
        target.getPersistentData().putUUID("shatter_caster", caster.getUUID());

        // Apply crystal visual (glowing effect)
        target.setGlowingTag(true);

        // Spawn crystal transformation particles
        spawnCrystalTransformParticles(serverLevel, pos);

        TalentsMod.LOGGER.debug("Tier 2 Shattering Prism applied: will shatter for {} damage on next hit",
                              baseDamage * 1.5f);
    }

    /**
     * Tier 3 - Stasis Field
     * Apply time-stop effect (Slowness 255, Weakness 255) in massive radius, mastery-scaled duration (1.5s base)
     */
    private void executeTier3StasisField(ServerPlayer caster, LivingEntity target,
                                        ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 12.0f; // Massive radius

        // Calculate mastery-scaled duration (1.5s base = 30 ticks)
        int baseDuration = 30;
        int stasisDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 3.0f);

        // Find all entities in radius
        AABB area = new AABB(pos.add(-radius, -radius, -radius), pos.add(radius, radius, radius));
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);

        int frozenCount = 0;
        for (LivingEntity entity : targets) {
            if (entity == caster) continue;

            double distance = entity.position().distanceTo(pos);
            if (distance <= radius) {
                // Apply time-stop effect (Slowness 255, Weakness 255)
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stasisDuration, 255));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stasisDuration, 255));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, stasisDuration, 255));

                // Set to maximum frozen
                entity.setTicksFrozen(entity.getTicksRequiredToFreeze() + stasisDuration);

                frozenCount++;
            }
        }

        // Spawn stasis field particles
        spawnStasisFieldParticles(serverLevel, pos, radius);

        TalentsMod.LOGGER.info("Tier 3 Stasis Field: froze {} entities for {}s (mastery-scaled)",
                             frozenCount, stasisDuration / 20f);
    }

    /**
     * Tier 4 - Cryo-Shatter
     * Apply debuff converting damage to 125% bonus Poise damage for 10s
     */
    private void executeTier4CryoShatter(ServerPlayer caster, LivingEntity target,
                                        ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 5.0f);

        // Apply Cryo-Shatter effect for 10 seconds (200 ticks)
        // All damage taken is converted to 125% Poise damage
        target.addEffect(new MobEffectInstance(ModEffects.CRYO_SHATTER.get(), 200, 0));

        // TODO: Implement Poise system integration for damage conversion
        // For now, apply vulnerability debuff as placeholder
        target.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), 200, 1));

        // Apply extreme freeze
        target.setTicksFrozen(target.getTicksRequiredToFreeze() + 200);

        // Apply movement and attack debuffs
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));

        // Spawn cryo-shatter particles
        spawnCryoShatterParticles(serverLevel, pos);

        TalentsMod.LOGGER.info("Tier 4 Cryo-Shatter applied to {}: 10s Poise damage conversion",
                             target.getName().getString());
    }

    /**
     * Calculate mastery-scaled duration
     * Formula: baseDuration * (1 + generalMastery - 1 + specificMastery - 1)
     */
    private int calculateMasteryScaledDuration(ServerPlayer caster, int baseDuration) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0); // Base is 1

        // Get Ice-specific mastery
        double iceMasteryAttr = caster.getAttributeValue(MasteryAttributes.ICE_MASTERY.get());
        float iceMastery = (float)(iceMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, iceMastery);

        // Scale duration
        return (int)(baseDuration * effectiveMastery);
    }

    /**
     * Spawn cold wave particles radiating outward
     */
    private void spawnColdWaveParticles(ServerLevel level, Vec3 center, float radius) {
        // Radiating wave effect
        for (double r = 0; r <= radius; r += 0.5) {
            int particlesAtRadius = (int)(r * 8);
            for (int i = 0; i < particlesAtRadius; i++) {
                double angle = (Math.PI * 2 * i) / particlesAtRadius;
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;

                level.sendParticles(ParticleTypes.SNOWFLAKE,
                    x, center.y + 0.5, z,
                    3, 0.1, 0.2, 0.1, 0.02);

                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        x, center.y + 0.5, z,
                        1, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * Spawn crystal transformation particles
     */
    private void spawnCrystalTransformParticles(ServerLevel level, Vec3 pos) {
        // Upward spiral of ice particles
        for (int i = 0; i < 50; i++) {
            double height = (i / 50.0) * 3.0;
            double angle = (i / 50.0) * Math.PI * 4; // 2 full rotations
            double radius = 1.0 - (i / 50.0) * 0.5; // Shrinking spiral

            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.SNOWFLAKE,
                x, pos.y + height, z,
                2, 0.05, 0.05, 0.05, 0.01);

            level.sendParticles(ParticleTypes.END_ROD,
                x, pos.y + height, z,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn stasis field particles (massive sphere of frozen time)
     */
    private void spawnStasisFieldParticles(ServerLevel level, Vec3 center, float radius) {
        // Sphere of particles
        for (int i = 0; i < 150; i++) {
            // Random point on sphere surface
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);

            double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.y + radius * Math.cos(phi);
            double z = center.z + radius * Math.sin(phi) * Math.sin(theta);

            level.sendParticles(ParticleTypes.SNOWFLAKE,
                x, y, z,
                1, 0, 0, 0, 0);

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.SOUL,
                    x, y, z,
                    1, 0, 0, 0, 0);
            }
        }

        // Center concentration
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2;
            double offsetY = (level.random.nextDouble() - 0.5) * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2;

            level.sendParticles(ParticleTypes.END_ROD,
                center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn cryo-shatter particles (extreme cold explosion)
     */
    private void spawnCryoShatterParticles(ServerLevel level, Vec3 pos) {
        // Explosive burst
        for (int i = 0; i < 100; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 1.5;
            double vy = level.random.nextDouble() * 1.0;
            double vz = (level.random.nextDouble() - 0.5) * 1.5;

            level.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.x, pos.y + 1, pos.z,
                5, vx, vy, vz, 0.3);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SOUL,
                    pos.x, pos.y + 1, pos.z,
                    2, vx * 0.5, vy * 0.5, vz * 0.5, 0.1);
            }

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 1, pos.z,
                    1, vx * 0.3, vy * 0.3, vz * 0.3, 0.05);
            }
        }
    }

    @Override
    public String getName() {
        return "Glacial Tomb";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Frostburst: Cold wave with mastery-scaled freeze duration";
            case TIER_2 -> "Shattering Prism: Crystal transform, shatters for 150% AoE on next hit";
            case TIER_3 -> "Stasis Field: Massive time-stop effect (Slowness 255, Weakness 255)";
            case TIER_4 -> "Cryo-Shatter: 10s debuff converting damage to 125% Poise damage";
            case NONE -> "Unknown tier";
        };
    }
}
