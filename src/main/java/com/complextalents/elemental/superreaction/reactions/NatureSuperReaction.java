package com.complextalents.elemental.superreaction.reactions;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.DamageOverTimeManager;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World's Wrath - Nature Super-Reaction
 *
 * Tier 1 - Grasping Thorns: Thorns particles + 1.5s root + bleed DOT, mastery-scaled
 * Tier 2 - Jungle's Embrace: Spawn jungle zone (5s), apply damage + 1s root + silence effect, mastery-scaled durations
 * Tier 3 - Avatar of the Wild: Spawn Heart of the Wild entity at location, pulse damage + root every 1s for 5s, mastery-scaled
 * Tier 4 - Verdant Crucible: Apply spore emission debuff (20s), target emits damaging spores in radius, apply bleed to hit enemies, mastery-scaled
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class NatureSuperReaction implements SuperReaction {

    // Tier 3 - Heart of the Wild tracking (pulse damage + root)
    private static final Map<UUID, HeartOfTheWildData> activeHearts = new ConcurrentHashMap<>();

    private static class HeartOfTheWildData {
        UUID heartUUID;
        UUID casterUUID;
        Vec3 position;
        float baseDamage;
        float radius;
        int ticksRemaining;
    }

    // Tier 4 - Verdant Crucible tracking (spore emission)
    private static final Map<UUID, VerdantCrucibleData> activeSporeEmitters = new ConcurrentHashMap<>();

    private static class VerdantCrucibleData {
        UUID targetUUID;
        UUID casterUUID;
        float sporeDamage;
        float sporeRadius;
        int ticksRemaining;
    }

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1GraspingThorns(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2JunglesEmbrace(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3AvatarOfTheWild(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4VerdantCrucible(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Nature Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Grasping Thorns
     * Thorns particles + 1.5s root + bleed DOT (mastery-scaled)
     */
    private void executeTier1GraspingThorns(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float thornRadius = 3.0f;

        // Calculate mastery-scaled duration (1.5s base = 30 ticks)
        int baseDuration = 30;
        int rootDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Apply root effect (Slowness IV + Jump boost -10 to prevent jumping)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootDuration, 3));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, rootDuration, -10));

        // Apply bleed DOT using Wither effect (3s duration, 10 ticks per damage)
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));

        // Spawn thorns particles in circle around target
        spawnThornsParticles(serverLevel, pos, thornRadius);

        TalentsMod.LOGGER.debug("Tier 1 Grasping Thorns: {} damage, {}s root + bleed DOT (mastery-scaled)",
                              baseDamage, rootDuration / 20f);
    }

    /**
     * Tier 2 - Jungle's Embrace
     * Spawn jungle zone (5s), apply damage + 1s root + silence effect (mastery-scaled durations)
     */
    private void executeTier2JunglesEmbrace(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float zoneRadius = 8.0f;

        // Calculate mastery-scaled durations
        int baseZoneDuration = 100; // 5s
        int zoneDuration = calculateMasteryScaledDuration(caster, baseZoneDuration);

        int baseRootDuration = 20; // 1s
        int rootDuration = calculateMasteryScaledDuration(caster, baseRootDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 2.0f);

        // Spawn jungle zone
        AreaEffectCloud jungleZone = new AreaEffectCloud(serverLevel, pos.x, pos.y, pos.z);
        jungleZone.setRadius(zoneRadius);
        jungleZone.setDuration(zoneDuration);
        jungleZone.setRadiusPerTick(0f); // Don't shrink
        jungleZone.setWaitTime(0);
        jungleZone.setRadiusOnUse(0f);
        jungleZone.setParticle(ParticleTypes.SPORE_BLOSSOM_AIR);

        // Store jungle zone data
        jungleZone.getPersistentData().putBoolean("jungle_zone", true);
        jungleZone.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        jungleZone.getPersistentData().putFloat("zone_damage", baseDamage * 0.15f); // 15% damage per tick
        jungleZone.getPersistentData().putInt("effect_interval", 20); // Apply effects every 1s
        jungleZone.getPersistentData().putInt("root_duration", rootDuration);
        jungleZone.getPersistentData().putLong("last_effect_time", serverLevel.getGameTime());

        serverLevel.addFreshEntity(jungleZone);

        // Spawn jungle particles
        spawnJungleParticles(serverLevel, pos, zoneRadius);

        TalentsMod.LOGGER.info("Tier 2 Jungle's Embrace: spawned jungle zone for {}s, {}s root per tick (mastery-scaled)",
                             zoneDuration / 20f, rootDuration / 20f);
    }

    /**
     * Tier 3 - Avatar of the Wild
     * Spawn Heart of the Wild entity at location, pulse damage + root every 1s for 5s (mastery-scaled)
     */
    private void executeTier3AvatarOfTheWild(ServerPlayer caster, LivingEntity target,
                                             ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float heartRadius = 10.0f;

        // Calculate mastery-scaled duration (5s base = 100 ticks)
        int baseDuration = 100;
        int heartDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 3.0f);

        // Create Heart of the Wild data
        HeartOfTheWildData heartData = new HeartOfTheWildData();
        heartData.heartUUID = UUID.randomUUID();
        heartData.casterUUID = caster.getUUID();
        heartData.position = pos;
        heartData.baseDamage = baseDamage * 0.3f; // 30% damage per pulse
        heartData.radius = heartRadius;
        heartData.ticksRemaining = heartDuration;

        activeHearts.put(heartData.heartUUID, heartData);

        // Spawn heart particles (nature core)
        spawnHeartOfTheWildParticles(serverLevel, pos, heartRadius);

        TalentsMod.LOGGER.info("Tier 3 Avatar of the Wild: spawned Heart of the Wild for {}s, {}% pulse damage every 1s (mastery-scaled)",
                             heartDuration / 20f, 30);
    }

    /**
     * Tier 4 - Verdant Crucible
     * Apply spore emission debuff (20s), target emits damaging spores in radius, apply bleed to hit enemies (mastery-scaled)
     */
    private void executeTier4VerdantCrucible(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float sporeRadius = 6.0f;

        // Calculate mastery-scaled duration (20s base = 400 ticks)
        int baseDuration = 400;
        int crucibleDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 5.0f);

        // Create Verdant Crucible data for spore emission
        VerdantCrucibleData crucibleData = new VerdantCrucibleData();
        crucibleData.targetUUID = target.getUUID();
        crucibleData.casterUUID = caster.getUUID();
        crucibleData.sporeDamage = baseDamage * 0.2f; // 20% damage per spore pulse
        crucibleData.sporeRadius = sporeRadius;
        crucibleData.ticksRemaining = crucibleDuration;

        activeSporeEmitters.put(target.getUUID(), crucibleData);

        // Store crucible data in persistent data
        target.getPersistentData().putBoolean("verdant_crucible", true);
        target.getPersistentData().putLong("crucible_end_time", serverLevel.getGameTime() + crucibleDuration);

        // Apply visual effects
        target.setGlowingTag(true);

        // Spawn initial burst particles
        spawnVerdantCrucibleParticles(serverLevel, pos, sporeRadius);

        TalentsMod.LOGGER.info("Tier 4 Verdant Crucible applied to {}: {}s duration, emits spores in {} radius every 1s (mastery-scaled)",
                             target.getName().getString(), crucibleDuration / 20f, sporeRadius);
    }

    /**
     * Server tick event handler for Heart of the Wild and Verdant Crucible effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Process Hearts of the Wild (pulse every 20 ticks = 1s)
        activeHearts.entrySet().removeIf(entry -> {
            UUID heartId = entry.getKey();
            HeartOfTheWildData heart = entry.getValue();

            ServerPlayer caster = server.getPlayerList().getPlayer(heart.casterUUID);
            if (caster == null) {
                return true; // Remove heart if caster offline
            }

            ServerLevel level = (ServerLevel) caster.level();
            heart.ticksRemaining--;

            // Pulse every 20 ticks (1s)
            if (heart.ticksRemaining % 20 == 0) {
                // Find all entities in radius
                AABB pulseArea = new AABB(
                    heart.position.add(-heart.radius, -3, -heart.radius),
                    heart.position.add(heart.radius, 3, heart.radius)
                );
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pulseArea);

                for (LivingEntity entity : targets) {
                    if (entity == caster) continue;

                    double distance = entity.position().distanceTo(heart.position);
                    if (distance <= heart.radius) {
                        // Deal pulse damage
                        DamageSource damageSource = entity.damageSources().playerAttack(caster);
                        entity.hurt(damageSource, heart.baseDamage);

                        // Apply 1s root
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, -10));
                    }
                }

                // Spawn pulse particles
                spawnPulseParticles(level, heart.position, heart.radius);
            }

            // Remove heart when time is up
            return heart.ticksRemaining <= 0;
        });

        // Process Verdant Crucible spore emitters (emit every 20 ticks = 1s)
        activeSporeEmitters.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            VerdantCrucibleData crucible = entry.getValue();

            // Find target and caster
            LivingEntity target = findEntity(targetId, server);
            if (target == null || !target.isAlive()) {
                return true; // Remove emitter
            }

            ServerPlayer caster = server.getPlayerList().getPlayer(crucible.casterUUID);
            if (caster == null) {
                return true; // Remove emitter if caster offline
            }

            ServerLevel level = (ServerLevel) target.level();
            crucible.ticksRemaining--;

            // Emit spores every 20 ticks (1s)
            if (crucible.ticksRemaining % 20 == 0) {
                Vec3 targetPos = target.position();

                // Find all entities in spore radius
                AABB sporeArea = new AABB(
                    targetPos.add(-crucible.sporeRadius, -2, -crucible.sporeRadius),
                    targetPos.add(crucible.sporeRadius, 2, crucible.sporeRadius)
                );
                List<LivingEntity> sporeTargets = level.getEntitiesOfClass(LivingEntity.class, sporeArea);

                for (LivingEntity entity : sporeTargets) {
                    if (entity == caster || entity == target) continue;

                    double distance = entity.position().distanceTo(targetPos);
                    if (distance <= crucible.sporeRadius) {
                        // Deal spore damage
                        DamageSource damageSource = entity.damageSources().playerAttack(caster);
                        entity.hurt(damageSource, crucible.sporeDamage);

                        // Apply bleed (Wither effect)
                        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    }
                }

                // Spawn spore particles
                spawnSporeEmissionParticles(level, targetPos, crucible.sporeRadius);
            }

            // Remove emitter when time is up
            if (crucible.ticksRemaining <= 0) {
                target.setGlowingTag(false);
                target.getPersistentData().remove("verdant_crucible");
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
     * Calculate mastery-scaled duration
     */
    private int calculateMasteryScaledDuration(ServerPlayer caster, int baseDuration) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0);

        // Get Nature-specific mastery
        double natureMasteryAttr = caster.getAttributeValue(MasteryAttributes.NATURE_MASTERY.get());
        float natureMastery = (float)(natureMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, natureMastery);

        // Scale duration
        return (int)(baseDuration * effectiveMastery);
    }

    /**
     * Spawn thorns particles in circle
     */
    private static void spawnThornsParticles(ServerLevel level, Vec3 center, float radius) {
        // Circle of thorns
        int thornCount = 30;
        for (int i = 0; i < thornCount; i++) {
            double angle = (Math.PI * 2 * i) / thornCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            // Vertical line of particles (thorns growing from ground)
            for (int j = 0; j < 10; j++) {
                double y = center.y + j * 0.3;
                level.sendParticles(ParticleTypes.COMPOSTER,
                    x, y, z,
                    1, 0.05, 0, 0.05, 0);
            }
        }
    }

    /**
     * Spawn jungle particles
     */
    private static void spawnJungleParticles(ServerLevel level, Vec3 center, float radius) {
        // Dense vegetation particles
        for (int i = 0; i < 150; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double distance = level.random.nextDouble() * radius;

            double x = center.x + Math.cos(theta) * distance;
            double z = center.z + Math.sin(theta) * distance;
            double y = center.y + level.random.nextDouble() * 3;

            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                x, y, z,
                2, 0.1, 0.1, 0.1, 0.02);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.COMPOSTER,
                    x, y, z,
                    1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawn Heart of the Wild particles (nature core)
     */
    private static void spawnHeartOfTheWildParticles(ServerLevel level, Vec3 center, float radius) {
        // Pulsing nature core
        for (int i = 0; i < 100; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double distance = level.random.nextDouble() * radius * 0.5;

            double x = center.x + distance * Math.sin(phi) * Math.cos(theta);
            double y = center.y + 1 + distance * Math.cos(phi);
            double z = center.z + distance * Math.sin(phi) * Math.sin(theta);

            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                x, y, z,
                1, 0, 0, 0, 0);

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    1, 0, 0, 0, 0);
            }
        }

        // Center marker
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2;
            double offsetY = (level.random.nextDouble() - 0.5) * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2;

            level.sendParticles(ParticleTypes.GLOW,
                center.x + offsetX, center.y + 1 + offsetY, center.z + offsetZ,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn pulse particles from Heart of the Wild
     */
    private static void spawnPulseParticles(ServerLevel level, Vec3 center, float radius) {
        // Expanding ring
        int particleCount = 40;
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2 * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.COMPOSTER,
                x, center.y + 0.5, z,
                2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /**
     * Spawn Verdant Crucible particles
     */
    private static void spawnVerdantCrucibleParticles(ServerLevel level, Vec3 pos, float radius) {
        // Explosive burst of spores
        for (int i = 0; i < 80; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 1.5;
            double vy = level.random.nextDouble() * 1.0;
            double vz = (level.random.nextDouble() - 0.5) * 1.5;

            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                pos.x, pos.y + 1, pos.z,
                3, vx, vy, vz, 0.2);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.COMPOSTER,
                    pos.x, pos.y + 1, pos.z,
                    1, vx * 0.5, vy * 0.5, vz * 0.5, 0.1);
            }
        }
    }

    /**
     * Spawn spore emission particles
     */
    private static void spawnSporeEmissionParticles(ServerLevel level, Vec3 center, float radius) {
        // Radiating spores
        int sporeCount = 30;
        for (int i = 0; i < sporeCount; i++) {
            double angle = (Math.PI * 2 * i) / sporeCount;
            double distance = radius * level.random.nextDouble();

            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = center.y + level.random.nextDouble() * 2;

            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                x, y, z,
                2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public String getName() {
        return "World's Wrath";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Grasping Thorns: Root + bleed DOT with thorns particles";
            case TIER_2 -> "Jungle's Embrace: Jungle zone with damage, root, and silence";
            case TIER_3 -> "Avatar of the Wild: Heart of the Wild pulses damage + root every 1s";
            case TIER_4 -> "Verdant Crucible: Spore emission aura with bleed damage to nearby enemies";
            case NONE -> "Unknown tier";
        };
    }
}
