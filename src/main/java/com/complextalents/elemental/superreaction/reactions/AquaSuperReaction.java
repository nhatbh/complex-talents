package com.complextalents.elemental.superreaction.reactions;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.superreaction.SuperReaction;
import com.complextalents.elemental.superreaction.SuperReactionTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Primordial Tide - Aqua Super-Reaction
 *
 * Tier 1 - Tidal Surge: Wave particles + knockback + 20% slow (2.5s, mastery-scaled)
 * Tier 2 - Tsunami: Full-screen wave effect, strip potion effects, spawn slow field (17% slow, 2.67s, mastery-scaled)
 * Tier 3 - Aegis of Leviathan: Transformation buff to player (8s), grant 12.5% speed, apply 5% damage taken debuff to nearby enemies
 * Tier 4 - The Great Flood: Persistent arena flood zone (60s), 14% slow to enemies, 30% speed to player
 */
public class AquaSuperReaction implements SuperReaction {

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1TidalSurge(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2Tsunami(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3AegisOfLeviathan(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4TheGreatFlood(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Aqua Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Tidal Surge
     * Wave particles + knockback + 20% slow (2.5s, mastery-scaled)
     */
    private void executeTier1TidalSurge(ServerPlayer caster, LivingEntity target,
                                        ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 5.0f;
        float knockbackStrength = 1.5f;

        // Calculate mastery-scaled duration (2.5s base = 50 ticks)
        int baseDuration = 50;
        int slowDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Find all entities in radius
        AABB area = new AABB(pos.add(-radius, -radius/2, -radius), pos.add(radius, radius/2, radius));
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : targets) {
            if (entity == caster) continue;

            double distance = entity.position().distanceTo(pos);
            if (distance <= radius) {
                // Calculate knockback direction (radial outward)
                Vec3 knockbackDirection = entity.position().subtract(pos).normalize();

                // Apply knockback
                entity.push(
                    knockbackDirection.x * knockbackStrength,
                    0.5 * knockbackStrength,
                    knockbackDirection.z * knockbackStrength
                );

                // Deal wave damage (reduced based on distance)
                float waveDamage = baseDamage * 0.5f * (1.0f - (float)(distance / radius) * 0.3f);
                entity.hurt(damageSource, waveDamage);

                // Apply 20% slow (Slowness I gives ~15%, Slowness II gives ~30%, so use amplifier 0)
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, 0));
            }
        }

        // Spawn tidal wave particles
        spawnTidalWaveParticles(serverLevel, pos, radius);

        TalentsMod.LOGGER.debug("Tier 1 Tidal Surge: {} damage, {}s slow (mastery-scaled)",
                              baseDamage, slowDuration / 20f);
    }

    /**
     * Tier 2 - Tsunami
     * Full-screen wave effect, strip potion effects, spawn slow field (17% slow, 2.67s, mastery-scaled)
     */
    private void executeTier2Tsunami(ServerPlayer caster, LivingEntity target,
                                     ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float waveRadius = 20.0f; // Full-screen radius
        float fieldRadius = 6.0f;

        // Calculate mastery-scaled duration (2.67s base = 53 ticks, rounded to 55)
        int baseDuration = 55;
        int slowDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 2.0f);

        // Create massive wave effect
        AABB waveArea = new AABB(pos.add(-waveRadius, -5, -waveRadius), pos.add(waveRadius, 5, waveRadius));
        List<LivingEntity> waveTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, waveArea);

        int hitCount = 0;
        for (LivingEntity entity : waveTargets) {
            if (entity == caster) continue;

            double distance = entity.position().distanceTo(pos);
            if (distance <= waveRadius) {
                // Strip all potion effects
                entity.removeAllEffects();

                // Deal wave damage
                float waveDamage = baseDamage * (1.0f - (float)(distance / waveRadius) * 0.5f);
                entity.hurt(damageSource, waveDamage);

                hitCount++;
            }
        }

        // Spawn persistent slow field at impact location
        spawnSlowField(serverLevel, pos, fieldRadius, caster, slowDuration);

        // Spawn full-screen tsunami particles
        spawnTsunamiParticles(serverLevel, pos, waveRadius);

        TalentsMod.LOGGER.info("Tier 2 Tsunami: hit {} targets, spawned slow field for {}s (mastery-scaled)",
                             hitCount, slowDuration / 20f);
    }

    /**
     * Tier 3 - Aegis of Leviathan
     * Apply transformation buff to player (8s), grant 12.5% speed, apply 5% damage taken debuff to nearby enemies
     */
    private void executeTier3AegisOfLeviathan(ServerPlayer caster, LivingEntity target,
                                              ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float debuffRadius = 8.0f;

        // Calculate mastery-scaled duration (8s base = 160 ticks)
        int baseDuration = 160;
        int buffDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 3.0f);

        // Apply transformation buff to player
        // Speed II gives +40% speed, Speed I gives +20% speed
        // For 12.5% speed, we'll use Speed I with reduced duration scaling
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, buffDuration, 0));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffDuration, 0));
        caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, buffDuration, 0));

        // Store transformation state in persistent data
        caster.getPersistentData().putBoolean("aegis_of_leviathan", true);
        caster.getPersistentData().putLong("aegis_end_time", serverLevel.getGameTime() + buffDuration);

        // Apply 5% damage taken debuff to nearby enemies
        AABB debuffArea = new AABB(pos.add(-debuffRadius, -debuffRadius, -debuffRadius),
                                   pos.add(debuffRadius, debuffRadius, debuffRadius));
        List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, debuffArea);

        int debuffedCount = 0;
        for (LivingEntity entity : enemies) {
            if (entity == caster) continue;

            double distance = entity.position().distanceTo(pos);
            if (distance <= debuffRadius) {
                // Apply Weakness effect for damage reduction
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, buffDuration, 0));
                debuffedCount++;
            }
        }

        // Spawn transformation particles around player
        spawnTransformationParticles(serverLevel, caster.position());

        TalentsMod.LOGGER.info("Tier 3 Aegis of Leviathan: transformed player for {}s, debuffed {} enemies (mastery-scaled)",
                             buffDuration / 20f, debuffedCount);
    }

    /**
     * Tier 4 - The Great Flood
     * Spawn persistent arena flood zone (60s), applies 14% slow to enemies, 30% speed to player
     */
    private void executeTier4TheGreatFlood(ServerPlayer caster, LivingEntity target,
                                          ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float floodRadius = 25.0f; // Massive arena-wide radius

        // Calculate mastery-scaled duration (60s base = 1200 ticks)
        int baseDuration = 1200;
        int floodDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 5.0f);

        // Spawn persistent flood zone
        AreaEffectCloud flood = new AreaEffectCloud(serverLevel, pos.x, pos.y, pos.z);
        flood.setRadius(floodRadius);
        flood.setDuration(floodDuration);
        flood.setRadiusPerTick(0f); // Don't shrink
        flood.setWaitTime(0);
        flood.setRadiusOnUse(0f);
        flood.setParticle(ParticleTypes.DRIPPING_WATER);

        // Store custom data for flood zone
        flood.getPersistentData().putBoolean("great_flood", true);
        flood.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        flood.getPersistentData().putInt("effect_interval", 10); // Apply effects every 0.5s
        flood.getPersistentData().putLong("last_effect_time", serverLevel.getGameTime());

        serverLevel.addFreshEntity(flood);

        // Spawn flood particles
        spawnFloodParticles(serverLevel, pos, floodRadius);

        TalentsMod.LOGGER.info("Tier 4 The Great Flood: spawned flood zone with {}s duration (mastery-scaled), radius {}",
                             floodDuration / 20f, floodRadius);
    }

    /**
     * Calculate mastery-scaled duration
     * Formula: baseDuration * (1 + generalMastery - 1 + specificMastery - 1)
     */
    private int calculateMasteryScaledDuration(ServerPlayer caster, int baseDuration) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0); // Base is 1

        // Get Aqua-specific mastery
        double aquaMasteryAttr = caster.getAttributeValue(MasteryAttributes.AQUA_MASTERY.get());
        float aquaMastery = (float)(aquaMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, aquaMastery);

        // Scale duration
        return (int)(baseDuration * effectiveMastery);
    }

    /**
     * Spawn tidal wave particles (radiating outward)
     */
    private void spawnTidalWaveParticles(ServerLevel level, Vec3 center, float radius) {
        // Radiating wave effect
        for (double r = 0; r <= radius; r += 0.5) {
            int particlesAtRadius = (int)(r * 8);
            for (int i = 0; i < particlesAtRadius; i++) {
                double angle = (Math.PI * 2 * i) / particlesAtRadius;
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;

                level.sendParticles(ParticleTypes.SPLASH,
                    x, center.y + 0.5, z,
                    3, 0.1, 0.2, 0.1, 0.02);

                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE,
                        x, center.y + 0.5, z,
                        1, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * Spawn full-screen tsunami particles
     */
    private void spawnTsunamiParticles(ServerLevel level, Vec3 center, float radius) {
        // Massive wave particle effect
        for (int i = 0; i < 200; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double distance = level.random.nextDouble() * radius;

            double x = center.x + Math.cos(theta) * distance;
            double z = center.z + Math.sin(theta) * distance;
            double y = center.y + level.random.nextDouble() * 5;

            level.sendParticles(ParticleTypes.SPLASH,
                x, y, z,
                5, 0.3, 0.3, 0.3, 0.1);

            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE,
                    x, y, z,
                    2, 0.1, 0.1, 0.1, 0.05);
            }

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.FALLING_WATER,
                    x, y + 3, z,
                    1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawn transformation particles around player
     */
    private void spawnTransformationParticles(ServerLevel level, Vec3 pos) {
        // Upward spiral of water particles
        for (int i = 0; i < 60; i++) {
            double height = (i / 60.0) * 3.0;
            double angle = (i / 60.0) * Math.PI * 6; // 3 full rotations
            double radius = 1.5 - (i / 60.0) * 0.5; // Shrinking spiral

            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.SPLASH,
                x, pos.y + height, z,
                2, 0.05, 0.05, 0.05, 0.01);

            level.sendParticles(ParticleTypes.BUBBLE,
                x, pos.y + height, z,
                1, 0, 0, 0, 0);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.FALLING_WATER,
                    x, pos.y + height + 1, z,
                    1, 0, 0, 0, 0);
            }
        }

        // Central burst
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2;
            double offsetY = (level.random.nextDouble() - 0.5) * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2;

            level.sendParticles(ParticleTypes.DOLPHIN,
                pos.x + offsetX, pos.y + 1 + offsetY, pos.z + offsetZ,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn persistent slow field
     */
    private void spawnSlowField(ServerLevel level, Vec3 pos, float radius, ServerPlayer caster, int duration) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(0f); // Don't shrink
        cloud.setWaitTime(0);
        cloud.setRadiusOnUse(0f);
        cloud.setParticle(ParticleTypes.DRIPPING_WATER);

        // Store custom data
        cloud.getPersistentData().putBoolean("slow_field", true);
        cloud.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        cloud.getPersistentData().putInt("effect_interval", 10); // Apply slow every 0.5s
        cloud.getPersistentData().putLong("last_effect_time", level.getGameTime());

        level.addFreshEntity(cloud);
    }

    /**
     * Spawn flood zone particles
     */
    private void spawnFloodParticles(ServerLevel level, Vec3 center, float radius) {
        // Ground-level water particles
        for (int i = 0; i < 300; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double distance = level.random.nextDouble() * radius;

            double x = center.x + Math.cos(theta) * distance;
            double z = center.z + Math.sin(theta) * distance;

            level.sendParticles(ParticleTypes.SPLASH,
                x, center.y + 0.5, z,
                3, 0.2, 0.1, 0.2, 0.02);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE,
                    x, center.y + 0.5, z,
                    2, 0.1, 0.1, 0.1, 0.01);
            }

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.FALLING_WATER,
                    x, center.y + 2, z,
                    1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public String getName() {
        return "Primordial Tide";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Tidal Surge: Wave with knockback and mastery-scaled slow";
            case TIER_2 -> "Tsunami: Full-screen wave that strips effects and spawns slow field";
            case TIER_3 -> "Aegis of Leviathan: Transform player with speed buff and debuff enemies";
            case TIER_4 -> "The Great Flood: Persistent 60s arena flood with enemy slow and player speed";
            case NONE -> "Unknown tier";
        };
    }
}
