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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Void's Gaze - Ender Super-Reaction
 * Reality manipulation and void corruption
 *
 * Tier 1 - Void Touched: Brand particles + 12.5% damage/armor reduction debuff (4s, mastery-scaled)
 * Tier 2 - Reality Fracture: Void exile (1.33s), track accumulated damage, apply on return with bleed, mastery-scaled
 * Tier 3 - Null Singularity: Pulling sphere entity, Unraveling debuff (25% damage taken, no healing), mastery-scaled
 * Tier 4 - Unraveling Nexus: Rift entity, Unraveling to all in range (20% damage taken, no healing, 1% true damage), mastery-scaled
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class EnderSuperReaction implements SuperReaction {

    // Tier 2 - Reality Fracture exile tracking
    private static final Map<UUID, RealityFractureData> activeExiles = new ConcurrentHashMap<>();

    // Tier 3 - Null Singularity tracking
    private static final Map<UUID, NullSingularityData> activeSingularities = new ConcurrentHashMap<>();

    // Tier 4 - Unraveling Nexus rift tracking
    private static final Map<UUID, UnravelingNexusData> activeRifts = new ConcurrentHashMap<>();

    static {
        MinecraftForge.EVENT_BUS.register(EnderSuperReaction.class);
    }

    @Override
    public void execute(ServerPlayer caster, LivingEntity target, SuperReactionTier tier,
                       Set<ElementType> elements, float baseDamage) {

        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (tier) {
            case TIER_1 -> executeTier1VoidTouched(caster, target, serverLevel, baseDamage);
            case TIER_2 -> executeTier2RealityFracture(caster, target, serverLevel, baseDamage);
            case TIER_3 -> executeTier3NullSingularity(caster, target, serverLevel, baseDamage);
            case TIER_4 -> executeTier4UnravelingNexus(caster, target, serverLevel, baseDamage);
            case NONE -> TalentsMod.LOGGER.warn("Ender Super-Reaction called with NONE tier");
        }
    }

    /**
     * Tier 1 - Void Touched
     * Brand particles + 12.5% damage/armor reduction debuff (4s, mastery-scaled)
     */
    private void executeTier1VoidTouched(ServerPlayer caster, LivingEntity target,
                                         ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();

        // Calculate mastery-scaled duration (4s base = 80 ticks)
        int baseDuration = 80;
        int brandDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage);

        // Apply Void Touched debuff (12.5% damage/armor reduction)
        target.addEffect(new MobEffectInstance(ModEffects.VOID_TOUCHED.get(), brandDuration, 0));

        // Spawn void brand particles (spiral pattern)
        spawnVoidBrandParticles(serverLevel, pos);

        TalentsMod.LOGGER.debug("Ender Tier 1 - Void Touched applied for {} ticks", brandDuration);
    }

    /**
     * Tier 2 - Reality Fracture
     * Void exile (1.33s = 27 ticks), track accumulated damage, apply on return with bleed
     */
    private void executeTier2RealityFracture(ServerPlayer caster, LivingEntity target,
                                             ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();

        // Calculate mastery-scaled duration (1.33s base = 27 ticks, rounded to nearest tick)
        int baseDuration = 27;
        int exileDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 1.5f);

        // Create Reality Fracture data
        RealityFractureData fractureData = new RealityFractureData();
        fractureData.targetUUID = target.getUUID();
        fractureData.casterUUID = caster.getUUID();
        fractureData.ticksRemaining = exileDuration;
        fractureData.accumulatedDamage = 0f;
        fractureData.originalPosition = pos;

        activeExiles.put(target.getUUID(), fractureData);

        // Apply exile effects (invisibility + invulnerability simulation)
        target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, exileDuration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, exileDuration, 4)); // Resistance V

        // Mark target with persistent data for damage tracking
        target.getPersistentData().putBoolean("reality_fractured", true);
        target.getPersistentData().putFloat("fracture_accumulated_damage", 0f);

        // Spawn exile particles
        spawnExileParticles(serverLevel, pos);

        TalentsMod.LOGGER.debug("Ender Tier 2 - Reality Fracture exiled target for {} ticks", exileDuration);
    }

    /**
     * Tier 3 - Null Singularity
     * Pulling sphere entity, Unraveling debuff (25% damage taken, no healing)
     */
    private void executeTier3NullSingularity(ServerPlayer caster, LivingEntity target,
                                             ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float pullRadius = 12.0f;

        // Calculate mastery-scaled duration (8s base = 160 ticks)
        int baseDuration = 160;
        int singularityDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 2.0f);

        // Apply Unraveling debuff (amplifier 0 = Tier 3: 25% damage taken, no healing)
        target.addEffect(new MobEffectInstance(ModEffects.UNRAVELING.get(), singularityDuration, 0));

        // Create Null Singularity data
        NullSingularityData singularityData = new NullSingularityData();
        singularityData.singularityUUID = UUID.randomUUID();
        singularityData.casterUUID = caster.getUUID();
        singularityData.position = pos;
        singularityData.radius = pullRadius;
        singularityData.ticksRemaining = singularityDuration;

        activeSingularities.put(singularityData.singularityUUID, singularityData);

        // Spawn singularity particles
        spawnSingularityParticles(serverLevel, pos, pullRadius);

        TalentsMod.LOGGER.debug("Ender Tier 3 - Null Singularity created for {} ticks", singularityDuration);
    }

    /**
     * Tier 4 - Unraveling Nexus
     * Rift entity, Unraveling to all in range (20% damage taken, no healing, 1% true damage on hit)
     */
    private void executeTier4UnravelingNexus(ServerPlayer caster, LivingEntity target,
                                             ServerLevel serverLevel, float baseDamage) {
        Vec3 pos = target.position();
        float riftRadius = 15.0f;

        // Calculate mastery-scaled duration (12s base = 240 ticks)
        int baseDuration = 240;
        int riftDuration = calculateMasteryScaledDuration(caster, baseDuration);

        // Deal primary damage
        DamageSource damageSource = target.damageSources().playerAttack(caster);
        target.hurt(damageSource, baseDamage * 3.0f);

        // Apply Unraveling debuff to target (amplifier 1 = Tier 4: 20% damage taken, no healing, 1% true damage)
        target.addEffect(new MobEffectInstance(ModEffects.UNRAVELING.get(), riftDuration, 1));

        // Create Unraveling Nexus data
        UnravelingNexusData nexusData = new UnravelingNexusData();
        nexusData.riftUUID = UUID.randomUUID();
        nexusData.casterUUID = caster.getUUID();
        nexusData.position = pos;
        nexusData.radius = riftRadius;
        nexusData.ticksRemaining = riftDuration;

        activeRifts.put(nexusData.riftUUID, nexusData);

        // Spawn rift particles
        spawnRiftParticles(serverLevel, pos, riftRadius);

        TalentsMod.LOGGER.debug("Ender Tier 4 - Unraveling Nexus created for {} ticks", riftDuration);
    }

    /**
     * Calculate mastery-scaled duration
     */
    private int calculateMasteryScaledDuration(ServerPlayer caster, int baseDuration) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0); // Base is 1

        // Get Ender-specific mastery
        double enderMasteryAttr = caster.getAttributeValue(MasteryAttributes.ENDER_MASTERY.get());
        float enderMastery = (float)(enderMasteryAttr + 1.0);

        // Use higher mastery
        float effectiveMastery = Math.max(generalMastery, enderMastery);

        // Scale duration
        return (int)(baseDuration * effectiveMastery);
    }

    /**
     * Spawn void brand particles (spiral pattern)
     */
    private void spawnVoidBrandParticles(ServerLevel level, Vec3 center) {
        // Spiral upward pattern
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double height = i * 0.1;
            double radius = 0.5;

            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + height;
            double z = center.z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.PORTAL,
                x, y, z,
                1, 0, 0, 0, 0.02);

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    x, y, z,
                    1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    /**
     * Spawn exile particles (dimensional tear)
     */
    private void spawnExileParticles(ServerLevel level, Vec3 center) {
        // Spherical implosion effect
        for (int i = 0; i < 100; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double radius = 2.0 + Math.random() * 2;

            double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.y + 1 + radius * Math.cos(phi);
            double z = center.z + radius * Math.sin(phi) * Math.sin(theta);

            // Particles move toward center
            double dx = (center.x - x) * 0.1;
            double dy = (center.y + 1 - y) * 0.1;
            double dz = (center.z - z) * 0.1;

            level.sendParticles(ParticleTypes.PORTAL,
                x, y, z,
                1, dx, dy, dz, 0.1);

            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    x, y, z,
                    1, dx, dy, dz, 0.05);
            }
        }
    }

    /**
     * Spawn singularity particles (pulling void sphere)
     */
    private void spawnSingularityParticles(ServerLevel level, Vec3 center, float radius) {
        // Spherical void effect with rotating rings
        for (int i = 0; i < 80; i++) {
            double angle = (Math.PI * 2) * i / 80;
            for (int r = 1; r < radius; r += 2) {
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;

                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    x, center.y + 1, z,
                    1, 0, 0.1, 0, 0.01);

                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.PORTAL,
                        x, center.y + 1, z,
                        1, 0, 0, 0, 0.02);
                }
            }
        }

        // Central void core
        for (int i = 0; i < 30; i++) {
            level.sendParticles(ParticleTypes.DRAGON_BREATH,
                center.x, center.y + 1, center.z,
                1, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /**
     * Spawn rift particles (dimensional tear with emanating corruption)
     */
    private void spawnRiftParticles(ServerLevel level, Vec3 center, float radius) {
        // Large dimensional tear effect
        for (int i = 0; i < 150; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double r = Math.random() * radius;

            double x = center.x + r * Math.sin(phi) * Math.cos(theta);
            double y = center.y + 1 + r * Math.cos(phi);
            double z = center.z + r * Math.sin(phi) * Math.sin(theta);

            level.sendParticles(ParticleTypes.PORTAL,
                x, y, z,
                1, 0, 0, 0, 0.05);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    x, y, z,
                    1, 0.1, 0.1, 0.1, 0.02);
            }

            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    x, y, z,
                    1, 0, 0, 0, 0.03);
            }
        }
    }

    /**
     * Global tick event handler for Ender Super-Reaction effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Process Reality Fractures (exile tracking)
        activeExiles.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            RealityFractureData fracture = entry.getValue();

            // Find target and caster
            LivingEntity target = findEntity(targetId, server);
            if (target == null || !target.isAlive()) {
                return true; // Remove exile
            }

            ServerPlayer caster = server.getPlayerList().getPlayer(fracture.casterUUID);
            if (caster == null) {
                return true; // Remove exile if caster offline
            }

            fracture.ticksRemaining--;

            // Track accumulated damage during exile
            if (target.getPersistentData().contains("fracture_accumulated_damage")) {
                fracture.accumulatedDamage = target.getPersistentData().getFloat("fracture_accumulated_damage");
            }

            // Remove exile when time is up
            if (fracture.ticksRemaining <= 0) {
                target.getPersistentData().remove("reality_fractured");
                target.getPersistentData().remove("fracture_accumulated_damage");

                // Apply accumulated damage with bleed
                if (fracture.accumulatedDamage > 0) {
                    DamageSource damageSource = target.damageSources().playerAttack(caster);
                    target.hurt(damageSource, fracture.accumulatedDamage);

                    // Apply bleed effect (Wither I for 4 seconds)
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));

                    TalentsMod.LOGGER.debug("Reality Fracture ended: Applied {} accumulated damage + bleed", fracture.accumulatedDamage);
                }

                // Spawn return particles
                if (target.level() instanceof ServerLevel serverLevel) {
                    spawnReturnParticles(serverLevel, target.position());
                }

                return true;
            }

            return false;
        });

        // Process Null Singularities (pulling sphere)
        activeSingularities.entrySet().removeIf(entry -> {
            UUID singularityId = entry.getKey();
            NullSingularityData singularity = entry.getValue();

            ServerPlayer caster = server.getPlayerList().getPlayer(singularity.casterUUID);
            if (caster == null) {
                return true; // Remove singularity if caster offline
            }

            ServerLevel level = (ServerLevel) caster.level();
            singularity.ticksRemaining--;

            // Pull entities every tick
            AABB pullArea = new AABB(
                singularity.position.add(-singularity.radius, -5, -singularity.radius),
                singularity.position.add(singularity.radius, 5, singularity.radius)
            );
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pullArea);

            for (LivingEntity entity : targets) {
                if (entity == caster) continue;

                double distance = entity.position().distanceTo(singularity.position);
                if (distance <= singularity.radius) {
                    // Pull toward center
                    Vec3 pullDirection = singularity.position.subtract(entity.position()).normalize();
                    float pullStrength = 0.15f * (1 - (float)(distance / singularity.radius));

                    entity.setDeltaMovement(entity.getDeltaMovement().add(
                        pullDirection.x * pullStrength,
                        pullDirection.y * pullStrength * 0.3,
                        pullDirection.z * pullStrength
                    ));
                }
            }

            // Spawn pull particles every 5 ticks
            if (singularity.ticksRemaining % 5 == 0) {
                spawnPullParticles(level, singularity.position, singularity.radius);
            }

            // Remove singularity when time is up
            return singularity.ticksRemaining <= 0;
        });

        // Process Unraveling Nexus rifts (apply Unraveling debuff every second)
        activeRifts.entrySet().removeIf(entry -> {
            UUID riftId = entry.getKey();
            UnravelingNexusData rift = entry.getValue();

            ServerPlayer caster = server.getPlayerList().getPlayer(rift.casterUUID);
            if (caster == null) {
                return true; // Remove rift if caster offline
            }

            ServerLevel level = (ServerLevel) caster.level();
            rift.ticksRemaining--;

            // Apply Unraveling every 20 ticks (1 second)
            if (rift.ticksRemaining % 20 == 0) {
                AABB riftArea = new AABB(
                    rift.position.add(-rift.radius, -5, -rift.radius),
                    rift.position.add(rift.radius, 5, rift.radius)
                );
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, riftArea);

                for (LivingEntity entity : targets) {
                    if (entity == caster) continue;

                    double distance = entity.position().distanceTo(rift.position);
                    if (distance <= rift.radius) {
                        // Apply Unraveling (amplifier 1 = Tier 4)
                        entity.addEffect(new MobEffectInstance(ModEffects.UNRAVELING.get(), 60, 1));
                    }
                }

                // Spawn rift pulse particles
                spawnRiftPulseParticles(level, rift.position, rift.radius);
            }

            // Remove rift when time is up
            return rift.ticksRemaining <= 0;
        });
    }

    /**
     * Spawn return particles when exile ends
     */
    private static void spawnReturnParticles(ServerLevel level, Vec3 center) {
        // Explosive outward burst
        for (int i = 0; i < 50; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;

            double dx = Math.sin(phi) * Math.cos(theta) * 0.3;
            double dy = Math.cos(phi) * 0.3;
            double dz = Math.sin(phi) * Math.sin(theta) * 0.3;

            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                center.x, center.y + 1, center.z,
                1, dx, dy, dz, 0.2);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 1, center.z,
                    1, dx, dy, dz, 0.15);
            }
        }
    }

    /**
     * Spawn pull particles for singularity
     */
    private static void spawnPullParticles(ServerLevel level, Vec3 center, float radius) {
        // Particles moving toward center
        for (int i = 0; i < 20; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double r = Math.random() * radius;

            double x = center.x + r * Math.sin(phi) * Math.cos(theta);
            double y = center.y + 1 + r * Math.cos(phi);
            double z = center.z + r * Math.sin(phi) * Math.sin(theta);

            double dx = (center.x - x) * 0.05;
            double dy = (center.y + 1 - y) * 0.05;
            double dz = (center.z - z) * 0.05;

            level.sendParticles(ParticleTypes.DRAGON_BREATH,
                x, y, z,
                1, dx, dy, dz, 0.05);

            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                    x, y, z,
                    1, dx, dy, dz, 0.03);
            }
        }
    }

    /**
     * Spawn rift pulse particles for Unraveling Nexus
     */
    private static void spawnRiftPulseParticles(ServerLevel level, Vec3 center, float radius) {
        // Expanding ring pulse
        for (int i = 0; i < 60; i++) {
            double angle = (Math.PI * 2) * i / 60;
            double x = center.x + Math.cos(angle) * radius * 0.8;
            double z = center.z + Math.sin(angle) * radius * 0.8;

            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                x, center.y + 1, z,
                1, 0, 0.2, 0, 0.1);

            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    x, center.y + 1, z,
                    1, 0, 0.1, 0, 0.05);
            }
        }
    }

    /**
     * Find entity by UUID across all server levels
     */
    private static LivingEntity findEntity(UUID entityUUID, net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(entityUUID);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Void's Gaze";
    }

    @Override
    public String getDescription(SuperReactionTier tier) {
        return switch (tier) {
            case TIER_1 -> "Void Touched: Brand particles + 12.5% damage/armor reduction debuff";
            case TIER_2 -> "Reality Fracture: Void exile with accumulated damage application + bleed";
            case TIER_3 -> "Null Singularity: Pulling sphere + Unraveling debuff (25% damage, no healing)";
            case TIER_4 -> "Unraveling Nexus: Rift with mass Unraveling (20% damage, no healing, 1% true damage)";
            case NONE -> "Unknown tier";
        };
    }

    /**
     * Data class for Reality Fracture tracking
     */
    private static class RealityFractureData {
        UUID targetUUID;
        UUID casterUUID;
        int ticksRemaining;
        float accumulatedDamage;
        Vec3 originalPosition;
    }

    /**
     * Data class for Null Singularity tracking
     */
    private static class NullSingularityData {
        UUID singularityUUID;
        UUID casterUUID;
        Vec3 position;
        float radius;
        int ticksRemaining;
    }

    /**
     * Data class for Unraveling Nexus tracking
     */
    private static class UnravelingNexusData {
        UUID riftUUID;
        UUID casterUUID;
        Vec3 position;
        float radius;
        int ticksRemaining;
    }
}
