package com.complextalents.elemental.handlers;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.effects.OPEffects;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class OPTickHandler {
    private static final Map<ServerLevel, List<FlamingGeyser>> activeFlamingGeysers = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<ScorchedZone>> activeScorchedZones = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<Whirlpool>> activeWhirlpools = new ConcurrentHashMap<>();

    public static void spawnFlamingGeyser(ServerLevel level, Vec3 pos, float scale, LivingEntity attacker) {
        activeFlamingGeysers.computeIfAbsent(level, k -> new ArrayList<>()).add(new FlamingGeyser(pos, scale, 120, attacker)); // 6s
    }

    public static void spawnScorchedZone(ServerLevel level, Vec3 pos, float radius, int duration, float dps,
            LivingEntity attacker) {
        activeScorchedZones.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new ScorchedZone(pos, radius, duration, dps, attacker));
    }

    public static void spawnWhirlpool(ServerLevel level, Vec3 pos, float radius, int duration, float dps, LivingEntity attacker) {
        activeWhirlpools.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new Whirlpool(pos, radius, duration, dps, attacker));
    }

    private static class Whirlpool {
        final Vec3 pos;
        final float radius;
        final float dps;
        final LivingEntity attacker;
        int remainingTicks;

        Whirlpool(Vec3 pos, float radius, int remainingTicks, float dps, LivingEntity attacker) {
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
            this.dps = dps;
            this.attacker = attacker;
        }
    }

    private static class FlamingGeyser {
        final Vec3 pos;
        final float scale;
        final LivingEntity attacker;
        int remainingTicks;

        FlamingGeyser(Vec3 pos, float scale, int remainingTicks, LivingEntity attacker) {
            this.pos = pos;
            this.scale = scale;
            this.remainingTicks = remainingTicks;
            this.attacker = attacker;
        }
    }

    private static class ScorchedZone {
        final Vec3 pos;
        final float radius;
        final float dps;
        final LivingEntity attacker;
        int remainingTicks;

        ScorchedZone(Vec3 pos, float radius, int remainingTicks, float dps, LivingEntity attacker) {
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
            this.dps = dps;
            this.attacker = attacker;
        }
    }

    public static ParticleOptions getIronParticle(String name) {
        return com.complextalents.util.IronParticleHelper.getIronParticle(name);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.level().getGameTime() % 10 == 0) {
            com.complextalents.elemental.OPCooldownTracker.tickCooldowns(event.player);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide)
            return;
        ServerLevel level = (ServerLevel) event.level;

        // Process Flaming Geysers
        processFlamingGeysers(level);
        // Process Scorched Zones
        processScorchedZones(level);
        // Process Whirlpools
        processWhirlpools(level);

        // Efficient entity processing
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (!(e instanceof LivingEntity entity))
                continue;

            if (entity.tickCount % 20 == 0) { // Check every second
                handlePeriodicEffects(entity, level);
            }

            // AI Stop for Ice T5 (Absolute Zero)
            if (entity.hasEffect(OPEffects.ABSOLUTE_ZERO.get())) {
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                    entity.getPersistentData().putBoolean("OP_FrozenAI", true);
                }
            } else if (entity.getPersistentData().getBoolean("OP_FrozenAI")) {
                if (entity instanceof Mob mob) {
                    mob.setNoAi(false);
                    entity.getPersistentData().remove("OP_FrozenAI");
                }
            }
        }
    }

    private static void processFlamingGeysers(ServerLevel level) {
        List<FlamingGeyser> geysers = activeFlamingGeysers.get(level);
        if (geysers == null || geysers.isEmpty())
            return;

        Iterator<FlamingGeyser> it = geysers.iterator();
        while (it.hasNext()) {
            FlamingGeyser geyser = it.next();
            geyser.remainingTicks--;

            // Visuals: Improved Fanning Geyser Eruption (Every 2 ticks)
            if (level.getGameTime() % 2 == 0) {
                int jetDensity = 15; // Balanced for server-side performance
                double power = 0.6 + (geyser.scale * 0.3);
                double spread = 0.2 + (geyser.scale * 0.1);
                
                for (int i = 0; i < jetDensity; i++) {
                    // 1. Concentrated Spawn Point (Tighter base)
                    double spawnX = geyser.pos.x + (level.random.nextDouble() - 0.5) * 0.1;
                    double spawnY = geyser.pos.y + (level.random.nextDouble() * 0.1); 
                    double spawnZ = geyser.pos.z + (level.random.nextDouble() - 0.5) * 0.1;

                    // 2. Fanning Velocity Math
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    // Magic distribution to keep center thick
                    double outwardSpeed = Math.pow(level.random.nextDouble(), 2) * spread;

                    double velX = Math.cos(angle) * outwardSpeed;
                    double velZ = Math.sin(angle) * outwardSpeed;
                    double velY = power * (0.8 + level.random.nextDouble() * 0.4); 

                    // 3. Spawning Particles
                    level.sendParticles(ParticleTypes.FLAME, spawnX, spawnY, spawnZ, 0, velX, velY, velZ, 1.0);

                    // 4. Layering Edges (Smoke and Lava)
                    if (outwardSpeed > spread * 0.5) {
                        if (level.random.nextFloat() < 0.15f) {
                            level.sendParticles(ParticleTypes.LAVA, spawnX, spawnY, spawnZ, 0, velX * 1.1, velY * 0.7, velZ * 1.1, 1.0);
                        }
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY, spawnZ, 0, velX * 0.8, velY * 0.6, velZ * 0.8, 1.0);
                    }
                }
            }

            // Damage: Every 10 ticks
            if (geyser.remainingTicks % 10 == 0) {
                float radius = 4.0f * (float) Math.sqrt(geyser.scale);
                float damage = 60f * geyser.scale; // Slightly buffed for the geyser theme

                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(geyser.pos.subtract(radius, 1, radius),
                                geyser.pos.add(radius, 8, radius))); // Vertical cylinder shape

                for (LivingEntity target : targets) {
                    if (geyser.attacker != null && (target == geyser.attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get() 
                        && com.complextalents.util.TeamHelper.isAlly(geyser.attacker, target))))
                        continue;
                    target.hurt(level.damageSources().indirectMagic(geyser.attacker, target), damage / 2);
                    target.setSecondsOnFire(5);
                }
            }

            if (geyser.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    private static void processScorchedZones(ServerLevel level) {
        List<ScorchedZone> zones = activeScorchedZones.get(level);
        if (zones == null || zones.isEmpty())
            return;

        Iterator<ScorchedZone> it = zones.iterator();
        while (it.hasNext()) {
            ScorchedZone zone = it.next();
            zone.remainingTicks--;

            // Visuals: Ground fire particles
            if (level.getGameTime() % 5 == 0) {
                int count = (int) (zone.radius * 5);
                ParticleOptions fire = com.complextalents.util.IronParticleHelper.getIronParticle("fire");
                if (fire != null) {
                    level.sendParticles(fire, zone.pos.x, zone.pos.y + 0.1, zone.pos.z, count, zone.radius / 2, 0.1,
                            zone.radius / 2, 0.02);
                }
            }

            // Damage every 10 ticks
            if (level.getGameTime() % 10 == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(zone.pos.subtract(zone.radius, 1, zone.radius),
                                zone.pos.add(zone.radius, 2, zone.radius)));

                for (LivingEntity target : targets) {
                    if (zone.attacker != null && (target == zone.attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get() 
                        && com.complextalents.util.TeamHelper.isAlly(zone.attacker, target))))
                        continue;
                    target.hurt(level.damageSources().indirectMagic(zone.attacker, target), zone.dps / 2); // True damage (magic)
                    target.setSecondsOnFire(3);
                }
                                                                                                           // 
                                                                                                           // 
            }

            if (zone.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    private static void processWhirlpools(ServerLevel level) {
        List<Whirlpool> pools = activeWhirlpools.get(level);
        if (pools == null || pools.isEmpty())
            return;

        Iterator<Whirlpool> it = pools.iterator();
        while (it.hasNext()) {
            Whirlpool pool = it.next();
            pool.remainingTicks--;

            // Visuals: Short, Violent, Multi-Particle Vortex
            if (level.getGameTime() % 2 == 0) {
                double maxRadius = pool.radius;
                double maxHeight = maxRadius * 2.5; 
                double timeOffset = level.getGameTime() * 0.15;

                // 1. The Tornado Core (Curved, Bending, & Messy)
                for (double y = 0; y < maxHeight; y += 0.5) {
                    
                    double heightRatio = y / maxHeight;
                    // Keep the 1.8 exponent for that flaring trumpet shape
                    double baseRadiusAtHeight = maxRadius * Math.pow(heightRatio, 1.8);
                    baseRadiusAtHeight = Math.max(0.4, baseRadiusAtHeight);

                    // Core sway
                    double swayX = Math.sin(y * 0.6 + timeOffset) * (y * 0.15);
                    double swayZ = Math.cos(y * 0.4 + timeOffset) * (y * 0.15);

                    int density = (int) Math.max(2, baseRadiusAtHeight * 4); 
                    for (int i = 0; i < density; i++) {
                        if (level.random.nextFloat() > 0.6f) continue;

                        double angle = (i * (Math.PI * 2) / density) + timeOffset + (y * 1.5);
                        double variantRadius = baseRadiusAtHeight * (0.6 + level.random.nextDouble() * 0.8);
                        double jitterY = (level.random.nextDouble() - 0.5) * 1.5;

                        double centerX = pool.pos.x + swayX;
                        double centerZ = pool.pos.z + swayZ;

                        double x = centerX + variantRadius * Math.cos(angle);
                        double z = centerZ + variantRadius * Math.sin(angle);
                        
                        double velX = -Math.sin(angle) * 0.15 - (Math.cos(angle) * 0.05);
                        double velY = 0.08; 
                        double velZ = Math.cos(angle) * 0.15 - (Math.sin(angle) * 0.05);

                        double finalY = pool.pos.y + y + jitterY;

                        // 2. The Particle Lottery (Mixing Textures)
                        float particleTypeChoice = level.random.nextFloat();
                        
                        if (particleTypeChoice < 0.4f) {
                            // 40% Chance: Standard condensation clouds
                            level.sendParticles(ParticleTypes.CLOUD, x, finalY, z, 0, velX, velY, velZ, 1.0);
                        } else if (particleTypeChoice < 0.7f) {
                            // 30% Chance: Violent splashing water (looks like mist/spray)
                            level.sendParticles(ParticleTypes.SPLASH, x, finalY, z, 0, velX * 2, velY, velZ * 2, 1.0);
                        } else if (particleTypeChoice < 0.9f) {
                            // 20% Chance: Heavy falling water/rain inside the funnel
                            level.sendParticles(ParticleTypes.FALLING_WATER, x, finalY, z, 0, 0, 0, 0, 1.0);
                        } else {
                            // 10% Chance: Dark debris/smoke mixed into the wind
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, finalY, z, 0, velX, velY, velZ, 1.0);
                        }
                    }
                }

                // 3. The Base Debris & Suction Field
                int suctionDensity = 12; // Slightly higher density for the ground chaos
                for (int i = 0; i < suctionDensity; i++) {
                    if (level.random.nextFloat() > 0.6f) continue;

                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double radius = maxRadius * (0.8 + level.random.nextDouble() * 0.8);
                    
                    double spawnX = pool.pos.x + Math.cos(angle) * radius;
                    // Keep debris focused strictly at the bottom quarter of the tornado
                    double spawnY = pool.pos.y + level.random.nextDouble() * (maxHeight * 0.25); 
                    double spawnZ = pool.pos.z + Math.sin(angle) * radius;

                    Vec3 pPos = new Vec3(spawnX, spawnY, spawnZ);
                    Vec3 toCenter = pool.pos.add(0, spawnY - pool.pos.y, 0).subtract(pPos).normalize();
                    Vec3 tangent = new Vec3(-toCenter.z, 0, toCenter.x);

                    double inwardSpeed = 0.6; 
                    double swirlSpeed = 0.4;
                    Vec3 velocity = toCenter.scale(inwardSpeed).add(tangent.scale(swirlSpeed));

                    // Ground debris is mostly smoke and splashing water tearing up the dirt
                    if (level.random.nextBoolean()) {
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY, spawnZ, 0, velocity.x, velocity.y, velocity.z, 1.0);
                    } else {
                        level.sendParticles(ParticleTypes.CLOUD, spawnX, spawnY, spawnZ, 0, velocity.x, velocity.y, velocity.z, 1.0);
                    }
                }
            }

            // Damage and Physics every tick
            double maxHeight = pool.radius * 2.5;
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pool.pos.subtract(pool.radius, 1, pool.radius),
                            pool.pos.add(pool.radius, maxHeight + 1, pool.radius)));

            for (LivingEntity target : targets) {
                if (pool.attacker != null && (target == pool.attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get() 
                    && com.complextalents.util.TeamHelper.isAlly(pool.attacker, target))))
                    continue;

                Vec3 targetPos = target.position();
                Vec3 toCenter = pool.pos.subtract(targetPos);
                double distSq = toCenter.x * toCenter.x + toCenter.z * toCenter.z;
                double pullRadius = pool.radius;
                
                if (distSq < pullRadius * pullRadius) {
                    Vec3 horizontalToCenter = new Vec3(toCenter.x, 0, toCenter.z).normalize();
                    Vec3 currentVel = target.getDeltaMovement();
                    
                    // 1. Core Logic: Lift or Eject
                    if (distSq < 1.5 * 1.5) {
                        if (target.getY() > pool.pos.y + (maxHeight * 0.8)) {
                            // EJECT: Violently throw away from center at the top
                            Vec3 ejectDir = horizontalToCenter.scale(-1.2).add(0, 0.5, 0);
                            target.setDeltaMovement(ejectDir);
                        } else {
                            // LIFT: Suck up the center
                            target.setDeltaMovement(new Vec3(currentVel.x * 0.8, 0.45, currentVel.z * 0.8));
                        }
                    } else {
                        // 2. Swirling Pull Logic
                        Vec3 tangent = new Vec3(-horizontalToCenter.z, 0, horizontalToCenter.x);
                        double pullStrength = 0.25;
                        double swirlStrength = 0.35;
                        
                        Vec3 force = horizontalToCenter.scale(pullStrength).add(tangent.scale(swirlStrength));
                        target.setDeltaMovement(currentVel.add(force));
                    }
                    target.hurtMarked = true;
                }

                // Damage every 10 ticks
                if (pool.remainingTicks % 10 == 0) {
                    target.hurt(level.damageSources().drown(), pool.dps / 2);
                }
            }

            if (pool.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    private static void handlePeriodicEffects(LivingEntity entity, ServerLevel level) {
        // Fire T5 (Miniature Sun) - MOVED TO LOCATION AOE

        // Nature T5 (Overgrowth Pulse)
        if (entity.hasEffect(OPEffects.OVERGROWTH.get())) {
            MobEffectInstance effect = entity.getEffect(OPEffects.OVERGROWTH.get());
            if (effect != null && entity.tickCount % 60 == 0) { // Every 3s
                float scale = effect.getAmplifier() / 10f;
                float damage = 300f * scale; // Approx 20% of 1500 * Scale

                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(6.0));
                for (LivingEntity target : nearby) {
                    if (target == entity)
                        continue;
                    // If we have an attacker stored in NBT, use it for friendly fire check
                    String attackerName = entity.getPersistentData().getString("OP_Attacker");
                    if (!attackerName.isEmpty()) {
                        net.minecraft.server.level.ServerPlayer attacker = level.getServer().getPlayerList().getPlayerByName(attackerName);
                        if (attacker != null) {
                            if (target == attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get() && com.complextalents.util.TeamHelper.isAlly(attacker, target)))
                                continue;
                            target.hurt(level.damageSources().indirectMagic(attacker, target), damage);
                        } else {
                            target.hurt(entity.damageSources().magic(), damage);
                        }
                    } else {
                        target.hurt(entity.damageSources().magic(), damage);
                    }
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1, entity.getZ(), 20,
                        1.0, 1.0, 1.0, 0.1);
            }
        }

        // Lightning T5 (Thundergod's Wrath Pulse)
        if (entity.hasEffect(OPEffects.THUNDERGODS_WRATH.get())) {
            MobEffectInstance effect = entity.getEffect(OPEffects.THUNDERGODS_WRATH.get());
            if (effect != null && entity.tickCount % 30 == 0) { // Every 1.5s
                float scale = effect.getAmplifier() / 10f;
                float damage = 450f * scale; // 30% of 1500 * Scale

                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(6.0));
                for (LivingEntity target : nearby) {
                    if (target == entity)
                        continue;
                    String attackerName = entity.getPersistentData().getString("OP_Attacker");
                    if (!attackerName.isEmpty()) {
                        net.minecraft.server.level.ServerPlayer attacker = level.getServer().getPlayerList().getPlayerByName(attackerName);
                        if (attacker != null) {
                            if (target == attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get() && com.complextalents.util.TeamHelper.isAlly(attacker, target)))
                                continue;
                            target.hurt(level.damageSources().indirectMagic(attacker, target), damage);
                        } else {
                            target.hurt(entity.damageSources().lightningBolt(), damage);
                        }
                    } else {
                        target.hurt(entity.damageSources().lightningBolt(), damage);
                    }
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1, entity.getZ(), 20,
                        0.5, 0.5, 0.5, 0.2);
            }
        }
    }
}
