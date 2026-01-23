package com.complextalents.elemental.superreaction.reactions;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.DamageOverTimeManager;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.superreaction.SuperReaction;
import com.complextalents.elemental.superreaction.SuperReactionTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inferno's Heart - Fire Super-Reaction
 *
 * Tier 1 - Conflagration: Explosion + 2.5s burn DOT
 * Tier 2 - Incinerating Maw: Vortex pull + explosion + lava pool (4s)
 * Tier 3 - Solar Judgment: Meteor trail + missing HP damage + Scorched Earth zone (3.75s DOT)
 * Tier 4 - Ignition: 8-second fuse with glowing + countdown + execute (5% max HP + flat)
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class FireSuperReaction implements SuperReaction {

    // Ignition fuse tracking
    private static final Map<UUID, IgnitionFuse> activeFuses = new ConcurrentHashMap<>();

    private static class IgnitionFuse {
        UUID targetUUID;
        UUID casterUUID;
        float baseDamage;
        long startTime;
        int ticksRemaining = 160; // 8 seconds

        IgnitionFuse(UUID targetUUID, UUID casterUUID, float baseDamage, long startTime) {
            this.targetUUID = targetUUID;
            this.casterUUID = casterUUID;
            this.baseDamage = baseDamage;
            this.startTime = startTime;
        }
    }

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1Conflagration(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2IncineratingMaw(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3SolarJudgment(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4Ignition(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Fire Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Conflagration
     * Explosion + 2.5s burn DOT effect
     */
    private void executeTier1Conflagration(ServerPlayer caster, LivingEntity target,
                                           ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 3.0f;

        // Deal primary explosion damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Create visual explosion (no terrain destruction)
        serverLevel.explode(caster, pos.x, pos.y, pos.z, radius, false,
                          net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        // Apply area damage to nearby enemies
        applyAreaDamage(serverLevel, caster, pos, radius, baseDamage * 0.5f);

        // Apply 2.5s burn DOT effect (50 ticks)
        DamageOverTimeManager.addDoT(target, caster, ElementalReaction.CONFLAGRATION_BURN,
                                    baseDamage * 0.4f, 50, 10); // Total 40% damage over 2.5s, tick every 0.5s

        // Spawn fire particles
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.FLAME, 50);

        TalentsMod.LOGGER.debug("Tier 1 Conflagration: {} damage with 2.5s burn DOT", baseDamage);
    }

    /**
     * Tier 2 - Incinerating Maw
     * Vortex pull towards target + explosion + lava pool entity (4s duration)
     */
    private void executeTier2IncineratingMaw(ServerPlayer caster, LivingEntity target,
                                            ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 4.5f;
        float pullRadius = 8.0f;

        // Pull nearby entities towards target (vortex effect)
        pullEntitiesToward(serverLevel, pos, pullRadius, caster);

        // Wait a tick for entities to move, then explode
        serverLevel.getServer().execute(() -> {
            // Deal primary explosion damage
            DamageSource damageSource = target.damageSources().playerAttack(caster);
            target.hurt(damageSource, baseDamage * 1.8f);

            // Create explosion
            serverLevel.explode(caster, pos.x, pos.y, pos.z, radius, false,
                              net.minecraft.world.level.Level.ExplosionInteraction.NONE);

            // Apply area damage
            applyAreaDamage(serverLevel, caster, pos, radius, baseDamage * 0.9f);

            // Spawn lava pool zone (4 seconds = 80 ticks)
            spawnLavaPool(serverLevel, pos, caster, baseDamage * 0.3f, 80);

            // Spawn particles
            spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.FLAME, 75);
            spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.LAVA, 30);
        });

        TalentsMod.LOGGER.debug("Tier 2 Incinerating Maw: {} damage with vortex + lava pool", baseDamage * 1.8f);
    }

    /**
     * Tier 3 - Solar Judgment
     * Meteor particle trail + missing HP damage component + Scorched Earth zone (3.75s DOT)
     */
    private void executeTier3SolarJudgment(ServerPlayer caster, LivingEntity target,
                                          ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float radius = 6.0f;

        // Spawn meteor trail particles from above
        spawnMeteorTrail(serverLevel, pos);

        // Calculate missing HP damage component
        float currentHP = target.getHealth();
        float maxHP = target.getMaxHealth();
        float missingHP = maxHP - currentHP;
        float missingHPDamage = missingHP * 0.15f; // 15% of missing HP

        float totalDamage = baseDamage * 3.0f + missingHPDamage;

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, totalDamage);

        // Create explosion
        serverLevel.explode(caster, pos.x, pos.y, pos.z, radius, false,
                          net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        // Apply area damage
        applyAreaDamage(serverLevel, caster, pos, radius, totalDamage * 0.5f);

        // Spawn Scorched Earth zone (3.75s = 75 ticks, DOT to entities inside)
        spawnScorchedEarthZone(serverLevel, pos, caster, baseDamage * 0.3f, 75);

        // Spawn impact particles
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.SOUL_FIRE_FLAME, 100);
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.FLAME, 50);

        TalentsMod.LOGGER.debug("Tier 3 Solar Judgment: {} damage ({} base + {} missing HP)",
                              totalDamage, baseDamage * 3.0f, missingHPDamage);
    }

    /**
     * Tier 4 - Ignition
     * Apply 8-second fuse with glowing effect, countdown particles, then detonate for 5% max HP + flat damage
     */
    private void executeTier4Ignition(ServerPlayer caster, LivingEntity target,
                                     ServerLevel serverLevel, float baseDamage) {
        UUID targetId = target.getUUID();

        // Check if target already has an active fuse
        if (activeFuses.containsKey(targetId)) {
            TalentsMod.LOGGER.debug("Target {} already has an Ignition fuse - refreshing",
                                  target.getName().getString());
            activeFuses.remove(targetId);
        }

        // Apply glowing effect
        target.setGlowingTag(true);

        // Create fuse instance
        IgnitionFuse fuse = new IgnitionFuse(targetId, caster.getUUID(), baseDamage,
                                            serverLevel.getGameTime());
        activeFuses.put(targetId, fuse);

        // Store fuse data in target's persistent data for visual feedback
        target.getPersistentData().putBoolean("ignition_fuse_active", true);
        target.getPersistentData().putLong("ignition_fuse_start", serverLevel.getGameTime());
        target.getPersistentData().putInt("ignition_fuse_duration", 160);

        TalentsMod.LOGGER.info("Ignition fuse applied to {} - detonation in 8 seconds",
                             target.getName().getString());
    }

    /**
     * Process active Ignition fuses
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Process each active fuse
        activeFuses.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            IgnitionFuse fuse = entry.getValue();

            // Find target entity
            LivingEntity target = findEntity(targetId, server);
            if (target == null || !target.isAlive()) {
                return true; // Remove fuse
            }

            // Find caster
            ServerPlayer caster = server.getPlayerList().getPlayer(fuse.casterUUID);

            fuse.ticksRemaining--;

            // Spawn countdown particles every 20 ticks (1 second)
            if (fuse.ticksRemaining % 20 == 0 && target.level() instanceof ServerLevel serverLevel) {
                int secondsLeft = fuse.ticksRemaining / 20;
                spawnCountdownParticles(serverLevel, target.position(), secondsLeft);
            }

            // Detonate when time is up
            if (fuse.ticksRemaining <= 0) {
                detonateIgnitionFuse(target, caster, fuse.baseDamage);
                return true; // Remove fuse
            }

            return false; // Keep fuse
        });
    }

    /**
     * Detonate an Ignition fuse
     */
    private static void detonateIgnitionFuse(LivingEntity target, ServerPlayer caster, float baseDamage) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = target.position();
        float radius = 8.0f;

        // Calculate execute damage: 5% max HP + base damage * 5.0
        float maxHP = target.getMaxHealth();
        float executeDamage = (maxHP * 0.05f) + (baseDamage * 5.0f);

        // Deal detonation damage
        DamageSource damageSource = caster != null ?
            target.damageSources().playerAttack(caster) :
            target.damageSources().magic();
        target.hurt(damageSource, executeDamage);

        // Create massive explosion (can destroy blocks)
        serverLevel.explode(caster, pos.x, pos.y, pos.z, radius, true,
                          net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);

        // Apply area damage
        AABB area = new AABB(pos.add(-radius, -radius, -radius), pos.add(radius, radius, radius));
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : targets) {
            if (entity == target || (caster != null && entity == caster)) continue;

            double distance = entity.position().distanceTo(pos);
            if (distance <= radius) {
                float falloff = 1.0f - (float)(distance / radius) * 0.5f;
                float areaDamage = executeDamage * 0.5f * falloff;

                DamageSource aoeSource = caster != null ?
                    entity.damageSources().playerAttack(caster) :
                    entity.damageSources().magic();
                entity.hurt(aoeSource, areaDamage);
            }
        }

        // Remove glowing effect
        target.setGlowingTag(false);
        target.getPersistentData().remove("ignition_fuse_active");
        target.getPersistentData().remove("ignition_fuse_start");
        target.getPersistentData().remove("ignition_fuse_duration");

        // Spawn detonation particles
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.SOUL_FIRE_FLAME, 150);
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.FLAME, 100);
        spawnExplosionParticles(serverLevel, pos, radius, ParticleTypes.LAVA, 50);

        TalentsMod.LOGGER.info("Ignition detonated on {} for {} damage (5% max HP + base)",
                             target.getName().getString(), executeDamage);
    }

    /**
     * Pull entities towards a position (vortex effect)
     */
    private void pullEntitiesToward(ServerLevel level, Vec3 center, float radius, ServerPlayer caster) {
        AABB area = new AABB(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : targets) {
            if (entity == caster) continue;

            Vec3 direction = center.subtract(entity.position()).normalize();
            double distance = entity.position().distanceTo(center);

            // Stronger pull when closer to edge
            float pullStrength = (float)(distance / radius) * 0.5f;
            Vec3 velocity = direction.scale(pullStrength);

            entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
            entity.hurtMarked = true;
        }
    }

    /**
     * Spawn lava pool zone entity
     */
    private void spawnLavaPool(ServerLevel level, Vec3 pos, ServerPlayer caster, float dotDamage, int duration) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius(3.0f);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(0f); // Don't shrink
        cloud.setWaitTime(0);
        cloud.setRadiusOnUse(0f);
        cloud.setParticle(ParticleTypes.LAVA);

        // Store custom data
        cloud.getPersistentData().putBoolean("lava_pool", true);
        cloud.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        cloud.getPersistentData().putFloat("dot_damage", dotDamage);
        cloud.getPersistentData().putInt("damage_interval", 10); // Damage every 0.5s
        cloud.getPersistentData().putLong("last_damage_time", level.getGameTime());

        level.addFreshEntity(cloud);

        TalentsMod.LOGGER.debug("Lava pool spawned for {}s", duration / 20);
    }

    /**
     * Spawn Scorched Earth zone entity
     */
    private void spawnScorchedEarthZone(ServerLevel level, Vec3 pos, ServerPlayer caster, float dotDamage, int duration) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius(5.0f);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-0.01f); // Slowly shrink
        cloud.setWaitTime(0);
        cloud.setRadiusOnUse(0f);
        cloud.setParticle(ParticleTypes.SOUL_FIRE_FLAME);

        // Store custom data
        cloud.getPersistentData().putBoolean("scorched_earth", true);
        cloud.getPersistentData().putUUID("caster_uuid", caster.getUUID());
        cloud.getPersistentData().putFloat("dot_damage", dotDamage);
        cloud.getPersistentData().putInt("damage_interval", 10); // Damage every 0.5s
        cloud.getPersistentData().putLong("last_damage_time", level.getGameTime());

        level.addFreshEntity(cloud);

        TalentsMod.LOGGER.debug("Scorched Earth zone spawned for {}s", duration / 20);
    }

    /**
     * Spawn meteor trail particles from above target
     */
    private void spawnMeteorTrail(ServerLevel level, Vec3 targetPos) {
        // Spawn particles from high above down to target
        for (double y = 30; y > 0; y -= 0.5) {
            Vec3 pos = new Vec3(targetPos.x, targetPos.y + y, targetPos.z);

            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y, pos.z,
                2, 0.1, 0, 0.1, 0.02);

            level.sendParticles(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                1, 0.1, 0, 0.1, 0.05);
        }
    }

    /**
     * Spawn countdown particles showing seconds remaining
     */
    private static void spawnCountdownParticles(ServerLevel level, Vec3 pos, int secondsLeft) {
        // Spawn particles in increasing intensity as time runs out
        int particleCount = (8 - secondsLeft + 1) * 10;

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            level.sendParticles(ParticleTypes.FLAME,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, 0, 0.1, 0, 0.05);

            if (secondsLeft <= 3) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0.1, 0, 0.05);
            }
        }
    }

    /**
     * Apply area damage to nearby enemies
     */
    private void applyAreaDamage(ServerLevel level, ServerPlayer caster, Vec3 center, float radius, float damage) {
        AABB area = new AABB(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : targets) {
            if (entity == caster) continue;

            double distance = entity.position().distanceTo(center);
            if (distance <= radius) {
                // Damage falls off with distance
                float falloff = 1.0f - (float)(distance / radius) * 0.5f;
                float areaDamage = damage * falloff;

                DamageSource damageSource = entity.damageSources().playerAttack(caster);
                entity.hurt(damageSource, areaDamage);
            }
        }
    }

    /**
     * Spawn explosion particles
     */
    private static void spawnExplosionParticles(ServerLevel level, Vec3 center, float radius,
                                        net.minecraft.core.particles.ParticleOptions particleType, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * radius;
            double offsetY = level.random.nextDouble() * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * radius;

            level.sendParticles(particleType,
                center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                1, 0, 0.1, 0, 0.05);
        }
    }

    /**
     * Find an entity by UUID across all levels
     */
    private static LivingEntity findEntity(UUID uuid, net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof LivingEntity entity) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Inferno's Heart";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Conflagration: Explosion with 2.5s burn DOT";
            case TIER_2 -> "Incinerating Maw: Vortex pull + explosion + 4s lava pool";
            case TIER_3 -> "Solar Judgment: Meteor strike with missing HP damage + Scorched Earth zone";
            case TIER_4 -> "Ignition: 8-second fuse countdown then massive execute detonation";
            case NONE -> "Unknown tier";
        };
    }
}
