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
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storm's Fury - Lightning Super-Reaction
 *
 * Tier 1 - Chain Lightning: Chain to 4 enemies with mastery-scaled damage
 * Tier 2 - Thunderclap: Apply Lightning Rod debuff, chain to 5 enemies every 0.5s
 * Tier 3 - Planar Storm: Spawn storm cloud entity, discharge to 5 enemies on next spell cast (37.5% spell damage)
 * Tier 4 - Superconductor: Pulsing lightning every 0.5s + 50% spell damage amplification debuff
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class LightningSuperReaction implements SuperReaction {

    // Tier 2 - Lightning Rod tracking (for chaining every 0.5s)
    private static final Map<UUID, LightningRodData> activeLightningRods = new ConcurrentHashMap<>();

    private static class LightningRodData {
        UUID targetUUID;
        UUID casterUUID;
        float baseDamage;
        int ticksRemaining;
        int chainCount;
        float chainRange;
    }

    // Tier 4 - Superconductor tracking (for pulsing lightning)
    private static final Map<UUID, SuperconductorData> activeSuperconductors = new ConcurrentHashMap<>();

    private static class SuperconductorData {
        UUID targetUUID;
        UUID casterUUID;
        float baseDamage;
        int ticksRemaining;
    }

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1ChainLightning(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2Thunderclap(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3PlanarStorm(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4Superconductor(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Lightning Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Chain Lightning
     * Chain to 4 enemies with mastery-scaled damage
     */
    private void executeTier1ChainLightning(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        int maxChains = 4; // Fixed to 4 enemies as per spec
        float chainRange = 10.0f;

        // Calculate mastery-scaled damage
        float chainDamage = calculateMasteryScaledDamage(caster, baseDamage);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, chainDamage);

        // Spawn lightning bolt visual
        spawnLightningBolt(serverLevel, pos);

        // Perform chain lightning
        Set<UUID> hitTargets = new HashSet<>();
        hitTargets.add(target.getUUID());
        performChainLightning(serverLevel, caster, target, chainDamage, maxChains - 1, chainRange, hitTargets);

        TalentsMod.LOGGER.debug("Tier 1 Chain Lightning: {} damage, chained to {} targets (mastery-scaled)",
                              chainDamage, hitTargets.size() - 1);
    }

    /**
     * Tier 2 - Thunderclap
     * Apply Lightning Rod debuff, chain to 5 enemies every 0.5s (mastery-scaled duration)
     */
    private void executeTier2Thunderclap(ServerPlayer caster, LivingEntity target,
                                         ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        int maxChains = 5;
        float chainRange = 12.0f;

        // Calculate mastery-scaled duration (5s base = 100 ticks)
        int baseDuration = 100;
        int rodDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 2.0f);

        // Spawn lightning bolt visual
        spawnLightningBolt(serverLevel, pos);

        // Apply Lightning Rod effect (visual marker)
        target.setGlowingTag(true);

        // Create Lightning Rod data for scheduled chaining
        LightningRodData rodData = new LightningRodData();
        rodData.targetUUID = target.getUUID();
        rodData.casterUUID = caster.getUUID();
        rodData.baseDamage = baseDamage * 0.5f; // Chain damage is 50% of base
        rodData.ticksRemaining = rodDuration;
        rodData.chainCount = maxChains;
        rodData.chainRange = chainRange;

        activeLightningRods.put(target.getUUID(), rodData);

        // Store rod data in persistent data
        target.getPersistentData().putBoolean("lightning_rod", true);
        target.getPersistentData().putLong("rod_end_time", serverLevel.getGameTime() + rodDuration);

        TalentsMod.LOGGER.info("Tier 2 Thunderclap: Applied Lightning Rod for {}s, will chain to {} enemies every 0.5s (mastery-scaled)",
                             rodDuration / 20f, maxChains);
    }

    /**
     * Tier 3 - Planar Storm
     * Spawn storm cloud entity above target, on next spell cast discharge to 5 enemies for 37.5% spell damage
     */
    private void executeTier3PlanarStorm(ServerPlayer caster, LivingEntity target,
                                         ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float cloudRadius = 8.0f;

        // Calculate mastery-scaled duration (30s base = 600 ticks)
        int baseDuration = 600;
        int cloudDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 3.0f);

        // Spawn storm cloud entity above target
        Vec3 cloudPos = pos.add(0, 8, 0); // 8 blocks above
        AreaEffectCloud stormCloud = new AreaEffectCloud(serverLevel, cloudPos.x, cloudPos.y, cloudPos.z);
        stormCloud.setRadius(cloudRadius);
        stormCloud.setDuration(cloudDuration);
        stormCloud.setRadiusPerTick(0f); // Don't shrink
        stormCloud.setWaitTime(0);
        stormCloud.setRadiusOnUse(0f);
        stormCloud.setParticle(ParticleTypes.CLOUD);

        // Store storm cloud data
        stormCloud.getPersistentData().putBoolean("planar_storm", true);
        stormCloud.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        stormCloud.getPersistentData().putFloat("discharge_damage_percent", 0.375f); // 37.5% of spell damage
        stormCloud.getPersistentData().putInt("discharge_targets", 5); // Chain to 5 enemies
        stormCloud.getPersistentData().putFloat("discharge_range", 15.0f);
        stormCloud.getPersistentData().putBoolean("ready_to_discharge", true);

        serverLevel.addFreshEntity(stormCloud);

        // Spawn storm cloud particles
        spawnStormCloudParticles(serverLevel, cloudPos, cloudRadius);

        TalentsMod.LOGGER.info("Tier 3 Planar Storm: spawned storm cloud with {}s duration, will discharge on next spell cast (mastery-scaled)",
                             cloudDuration / 20f);
    }

    /**
     * Tier 4 - Superconductor
     * Apply pulsing lightning (every 0.5s) + 50% spell damage amplification debuff
     */
    private void executeTier4Superconductor(ServerPlayer caster, LivingEntity target,
                                           ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();

        // Calculate mastery-scaled duration (10s base = 200 ticks)
        int baseDuration = 200;
        int conductorDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 5.0f);

        // Apply Superconductor effect (50% spell damage amplification)
        // TODO: Integrate with spell damage system when available
        // For now, apply a vulnerable effect as placeholder
        target.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), conductorDuration, 2));

        // Create Superconductor data for pulsing lightning
        SuperconductorData conductorData = new SuperconductorData();
        conductorData.targetUUID = target.getUUID();
        conductorData.casterUUID = caster.getUUID();
        conductorData.baseDamage = baseDamage * 0.1f; // Pulse damage is 10% of base
        conductorData.ticksRemaining = conductorDuration;

        activeSuperconductors.put(target.getUUID(), conductorData);

        // Store conductor data in persistent data
        target.getPersistentData().putBoolean("superconductor", true);
        target.getPersistentData().putLong("conductor_end_time", serverLevel.getGameTime() + conductorDuration);

        // Apply visual effects
        target.setGlowingTag(true);

        // Spawn initial burst particles
        spawnSuperconductorParticles(serverLevel, pos);

        TalentsMod.LOGGER.info("Tier 4 Superconductor applied to {}: {}s duration, pulsing lightning every 0.5s + 50% spell damage amp (mastery-scaled)",
                             target.getName().getString(), conductorDuration / 20f);
    }

    /**
     * Server tick event handler for Lightning Rod and Superconductor effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Process Lightning Rods (chain every 10 ticks = 0.5s)
        activeLightningRods.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            LightningRodData rod = entry.getValue();

            // Find target and caster
            LivingEntity target = findEntity(targetId, server);
            if (target == null || !target.isAlive()) {
                return true; // Remove rod
            }

            ServerPlayer caster = server.getPlayerList().getPlayer(rod.casterUUID);
            if (caster == null) {
                return true; // Remove rod if caster offline
            }

            rod.ticksRemaining--;

            // Chain every 10 ticks (0.5s)
            if (rod.ticksRemaining % 10 == 0 && target.level() instanceof ServerLevel serverLevel) {
                Set<UUID> hitTargets = new HashSet<>();
                hitTargets.add(target.getUUID());
                performChainLightning(serverLevel, caster, target, rod.baseDamage, rod.chainCount, rod.chainRange, hitTargets);

                // Spawn visual effect
                spawnLightningBolt(serverLevel, target.position());
            }

            // Remove rod when time is up
            if (rod.ticksRemaining <= 0) {
                target.setGlowingTag(false);
                target.getPersistentData().remove("lightning_rod");
                return true;
            }

            return false;
        });

        // Process Superconductors (pulse every 10 ticks = 0.5s)
        activeSuperconductors.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            SuperconductorData conductor = entry.getValue();

            // Find target and caster
            LivingEntity target = findEntity(targetId, server);
            if (target == null || !target.isAlive()) {
                return true; // Remove conductor
            }

            ServerPlayer caster = server.getPlayerList().getPlayer(conductor.casterUUID);
            if (caster == null) {
                return true; // Remove conductor if caster offline
            }

            conductor.ticksRemaining--;

            // Pulse every 10 ticks (0.5s)
            if (conductor.ticksRemaining % 10 == 0 && target.level() instanceof ServerLevel serverLevel) {
                // Deal pulse damage
                DamageSource damageSource = target.damageSources().playerAttack(caster);
                target.hurt(damageSource, conductor.baseDamage);

                // Spawn lightning particles
                spawnPulsingLightning(serverLevel, target.position());
            }

            // Remove conductor when time is up
            if (conductor.ticksRemaining <= 0) {
                target.setGlowingTag(false);
                target.getPersistentData().remove("superconductor");
                return true;
            }

            return false;
        });
    }

    /**
     * Helper method to find entity by UUID across all server levels
     */
    private static LivingEntity findEntity(UUID entityId, net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity.getUUID().equals(entityId) && entity instanceof LivingEntity living) {
                    return living;
                }
            }
        }
        return null;
    }

    /**
     * Perform chain lightning to nearby enemies
     */
    private static void performChainLightning(ServerLevel level, ServerPlayer caster, LivingEntity source,
                                             float damage, int remainingChains, float range, Set<UUID> hitTargets) {
        if (remainingChains <= 0) return;

        // Find nearby targets
        AABB searchArea = new AABB(
            source.position().add(-range, -range/2, -range),
            source.position().add(range, range/2, range)
        );

        List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, searchArea);

        // Sort by distance
        nearbyTargets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(source)));

        // Find next target that hasn't been hit
        LivingEntity nextTarget = null;
        for (LivingEntity candidate : nearbyTargets) {
            if (!hitTargets.contains(candidate.getUUID()) && candidate != caster && candidate != source) {
                nextTarget = candidate;
                break;
            }
        }

        if (nextTarget != null) {
            // Deal damage
            DamageSource damageSource = nextTarget.damageSources().playerAttack(caster);
            nextTarget.hurt(damageSource, damage * 0.8f); // 80% damage per chain

            // Mark as hit
            hitTargets.add(nextTarget.getUUID());

            // Visual effect
            createLightningArc(level, source.position(), nextTarget.position());

            // Continue chain
            performChainLightning(level, caster, nextTarget, damage * 0.8f, remainingChains - 1, range, hitTargets);
        }
    }

    /**
     * Calculate mastery-scaled damage
     */
    private float calculateMasteryScaledDamage(ServerPlayer caster, float baseDamage) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0);

        // Get Lightning-specific mastery
        double lightningMasteryAttr = caster.getAttributeValue(MasteryAttributes.LIGHTNING_MASTERY.get());
        float lightningMastery = (float)(lightningMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, lightningMastery);

        // Scale damage
        return baseDamage * effectiveMastery;
    }

    /**
     * Calculate mastery-scaled duration
     */
    private int calculateMasteryScaledDuration(ServerPlayer caster, int baseDuration) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0);

        // Get Lightning-specific mastery
        double lightningMasteryAttr = caster.getAttributeValue(MasteryAttributes.LIGHTNING_MASTERY.get());
        float lightningMastery = (float)(lightningMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, lightningMastery);

        // Scale duration
        return (int)(baseDuration * effectiveMastery);
    }

    /**
     * Spawn lightning bolt entity
     */
    private static void spawnLightningBolt(ServerLevel level, Vec3 position) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(position);
            bolt.setVisualOnly(true); // Visual only, don't cause fire
            level.addFreshEntity(bolt);
        }
    }

    /**
     * Create lightning arc particles between two points
     */
    private static void createLightningArc(ServerLevel level, Vec3 start, Vec3 end) {
        int particleCount = 15;
        for (int i = 0; i <= particleCount; i++) {
            double t = i / (double) particleCount;

            // Add arc variation
            double arcHeight = Math.sin(t * Math.PI) * 1.5;
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            Vec3 point = start.lerp(end, t).add(offsetX, arcHeight, offsetZ);

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                point.x, point.y, point.z,
                2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /**
     * Spawn storm cloud particles
     */
    private static void spawnStormCloudParticles(ServerLevel level, Vec3 center, float radius) {
        // Cloud particles
        for (int i = 0; i < 100; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double distance = level.random.nextDouble() * radius;

            double x = center.x + Math.cos(theta) * distance;
            double z = center.z + Math.sin(theta) * distance;
            double y = center.y + (level.random.nextDouble() - 0.5) * 2;

            level.sendParticles(ParticleTypes.CLOUD,
                x, y, z,
                1, 0.1, 0.1, 0.1, 0.01);

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
                    1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawn superconductor particles
     */
    private static void spawnSuperconductorParticles(ServerLevel level, Vec3 pos) {
        // Explosive burst of electricity
        for (int i = 0; i < 60; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 1.0;
            double vy = level.random.nextDouble() * 0.5;
            double vz = (level.random.nextDouble() - 0.5) * 1.0;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1, pos.z,
                3, vx, vy, vz, 0.2);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.FLASH,
                    pos.x, pos.y + 1, pos.z,
                    1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawn pulsing lightning particles
     */
    private static void spawnPulsingLightning(ServerLevel level, Vec3 pos) {
        // Vertical lightning surge
        for (int i = 0; i < 20; i++) {
            double height = i * 0.3;
            double radius = 0.5;

            double angle = level.random.nextDouble() * Math.PI * 2;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x, pos.y + height, z,
                2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public String getName() {
        return "Storm's Fury";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Chain Lightning: Chain to 4 enemies with mastery-scaled damage";
            case TIER_2 -> "Thunderclap: Lightning Rod that chains to 5 enemies every 0.5s";
            case TIER_3 -> "Planar Storm: Storm cloud that discharges on next spell cast (37.5% damage to 5 enemies)";
            case TIER_4 -> "Superconductor: Pulsing lightning every 0.5s + 50% spell damage amplification";
            case NONE -> "Unknown tier";
        };
    }
}
