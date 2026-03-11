package com.complextalents.elemental.handlers;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.effects.OPEffects;

import net.minecraft.core.particles.ParticleOptions;
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
    private static final Map<ServerLevel, List<ScorchedZone>> activeScorchedZones = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<WaterColumn>> activeWaterColumns = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<SandTornado>> activeSandTornados = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<MiniatureSun>> activeMiniatureSuns = new ConcurrentHashMap<>();

    public static void spawnScorchedZone(ServerLevel level, Vec3 pos, float radius, int duration, float dps,
            LivingEntity attacker) {
        activeScorchedZones.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new ScorchedZone(pos, radius, duration, dps, attacker));
    }

    public static void spawnWaterColumn(ServerLevel level, Vec3 pos, float radius, int duration,
            LivingEntity attacker) {
        activeWaterColumns.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new WaterColumn(pos, radius, duration, attacker));
    }

    public static void spawnSandTornado(ServerLevel level, Vec3 pos, float radius, int duration, float dps,
            LivingEntity attacker) {
        activeSandTornados.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new SandTornado(pos, radius, duration, dps, attacker));
    }

    public static void spawnMiniatureSun(ServerLevel level, Vec3 pos, float radius, int duration, float baseDps,
            LivingEntity attacker) {
        activeMiniatureSuns.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new MiniatureSun(pos, radius, duration, baseDps, attacker));
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

    private static class WaterColumn {
        final Vec3 pos;
        final float radius;
        final LivingEntity attacker;
        int remainingTicks;

        WaterColumn(Vec3 pos, float radius, int remainingTicks, LivingEntity attacker) {
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
            this.attacker = attacker;
        }
    }

    private static class SandTornado {
        final Vec3 pos;
        final float radius;
        final float dps;
        final LivingEntity attacker;
        int remainingTicks;

        SandTornado(Vec3 pos, float radius, int remainingTicks, float dps, LivingEntity attacker) {
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
            this.dps = dps;
            this.attacker = attacker;
        }
    }

    private static class MiniatureSun {
        final Vec3 pos;
        final float radius;
        final float baseDps;
        final LivingEntity attacker;
        int remainingTicks;
        int totalTicks;

        MiniatureSun(Vec3 pos, float radius, int remainingTicks, float baseDps, LivingEntity attacker) {
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
            this.totalTicks = remainingTicks;
            this.baseDps = baseDps;
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

        // Process Scorched Zones
        processScorchedZones(level);
        // Process Water Columns
        processWaterColumns(level);
        // Process Sand Tornados
        processSandTornados(level);
        // Process Miniature Suns
        processMiniatureSuns(level);

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

    private static void processMiniatureSuns(ServerLevel level) {
        List<MiniatureSun> suns = activeMiniatureSuns.get(level);
        if (suns == null || suns.isEmpty())
            return;

        Iterator<MiniatureSun> it = suns.iterator();
        while (it.hasNext()) {
            MiniatureSun sun = it.next();
            sun.remainingTicks--;

            // Damage every 10 ticks, ramping up
            if (sun.remainingTicks % 10 == 0) {
                float progress = 1.0f - ((float) sun.remainingTicks / sun.totalTicks);
                float rampingMultiplier = 1.0f + (progress * 2.0f); // Up to 3x damage
                float currentDps = sun.baseDps * rampingMultiplier;

                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(sun.pos.subtract(sun.radius, sun.radius, sun.radius),
                                sun.pos.add(sun.radius, sun.radius, sun.radius)));

                for (LivingEntity target : targets) {
                    if (sun.attacker != null && com.complextalents.util.TeamHelper.isAlly(sun.attacker, target))
                        continue;
                    target.hurt(level.damageSources().onFire(), currentDps / 2);
                    target.setSecondsOnFire(5);
                }
            }

            if (sun.remainingTicks <= 0) {
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
                    if (zone.attacker != null
                            && (target == zone.attacker || (ElementalReactionConfig.enableFriendlyFireProtection.get()
                                    && com.complextalents.util.TeamHelper.isAlly(zone.attacker, target))))
                        continue;
                    target.hurt(level.damageSources().indirectMagic(zone.attacker, target), zone.dps / 2); // True
                                                                                                           // damage
                                                                                                           // (magic)
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

    private static void processWaterColumns(ServerLevel level) {
        List<WaterColumn> columns = activeWaterColumns.get(level);
        if (columns == null || columns.isEmpty())
            return;

        Iterator<WaterColumn> it = columns.iterator();
        while (it.hasNext()) {
            WaterColumn column = it.next();
            column.remainingTicks--;

            // Initial Burst (First tick)
            if (column.remainingTicks % 60 == 59) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(column.pos.subtract(column.radius, 1, column.radius),
                                column.pos.add(column.radius, 10, column.radius)));
                for (LivingEntity target : targets) {
                    if (column.attacker != null && com.complextalents.util.TeamHelper.isAlly(column.attacker, target))
                        continue;
                    target.hurt(level.damageSources().indirectMagic(column.attacker, target), 40f);
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 1.5, 0));
                    target.hurtMarked = true;
                }
            }

            // Continuous Lift
            if (column.remainingTicks % 2 == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(column.pos.subtract(column.radius, 1, column.radius),
                                column.pos.add(column.radius, 12, column.radius)));
                for (LivingEntity target : targets) {
                    if (column.attacker != null && com.complextalents.util.TeamHelper.isAlly(column.attacker, target))
                        continue;
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.15, 0));
                    target.hurtMarked = true;
                }
            }

            if (column.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    private static void processSandTornados(ServerLevel level) {
        List<SandTornado> tornados = activeSandTornados.get(level);
        if (tornados == null || tornados.isEmpty())
            return;

        Iterator<SandTornado> it = tornados.iterator();
        while (it.hasNext()) {
            SandTornado tornado = it.next();
            tornado.remainingTicks--;

            double maxHeight = tornado.radius * 2.5;
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(tornado.pos.subtract(tornado.radius, 1, tornado.radius),
                            tornado.pos.add(tornado.radius, maxHeight + 1, tornado.radius)));

            for (LivingEntity target : targets) {
                if (tornado.attacker != null && com.complextalents.util.TeamHelper.isAlly(tornado.attacker, target))
                    continue;

                // Physics: Pulling
                Vec3 toCenter = tornado.pos.subtract(target.position());
                Vec3 pull = new Vec3(toCenter.x, 0, toCenter.z).normalize().scale(0.2);
                target.setDeltaMovement(target.getDeltaMovement().add(pull).add(0, 0.05, 0));
                target.hurtMarked = true;

                // Effects
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0));
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2));

                // Damage
                if (tornado.remainingTicks % 10 == 0) {
                    target.hurt(level.damageSources().indirectMagic(tornado.attacker, target), tornado.dps / 2);
                }
            }

            if (tornado.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    private static void handlePeriodicEffects(LivingEntity entity, ServerLevel level) {
    }
}
