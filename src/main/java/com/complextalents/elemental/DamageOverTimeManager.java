package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages damage over time effects from elemental reactions
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DamageOverTimeManager {

    private static final Map<UUID, List<DoTInstance>> activeDots = new ConcurrentHashMap<>();

    public static class DoTInstance {
        public final UUID targetUUID;
        public final UUID casterUUID;
        public final ElementalReaction reaction;
        public final float damagePerTick;
        public final int tickInterval;
        public int remainingTicks;
        public int tickCounter;

        public DoTInstance(UUID targetUUID, UUID casterUUID, ElementalReaction reaction,
                         float totalDamage, int duration, int tickInterval) {
            this.targetUUID = targetUUID;
            this.casterUUID = casterUUID;
            this.reaction = reaction;
            this.tickInterval = tickInterval;
            this.remainingTicks = duration / tickInterval;
            this.damagePerTick = totalDamage / this.remainingTicks;
            this.tickCounter = 0;
        }
    }

    /**
     * Add a new DoT effect to an entity
     */
    public static void addDoT(LivingEntity target, ServerPlayer caster, ElementalReaction reaction,
                             float totalDamage, int duration, int tickRate) {
        UUID targetId = target.getUUID();

        // Remove existing DoT of the same type
        removeDotOfType(targetId, reaction);

        DoTInstance dot = new DoTInstance(targetId, caster.getUUID(), reaction,
                                         totalDamage, duration, tickRate);

        activeDots.computeIfAbsent(targetId, k -> new ArrayList<>()).add(dot);

        if (ElementalReactionConfig.enableDebugLogging.get()) {
            TalentsMod.LOGGER.debug("Added {} DoT to {} for {} damage over {} ticks",
                reaction, target.getName().getString(), totalDamage, dot.remainingTicks);
        }
    }

    /**
     * Remove all DoT effects of a specific type from an entity
     */
    public static void removeDotOfType(UUID targetId, ElementalReaction reaction) {
        List<DoTInstance> dots = activeDots.get(targetId);
        if (dots != null) {
            dots.removeIf(dot -> dot.reaction == reaction);
            if (dots.isEmpty()) {
                activeDots.remove(targetId);
            }
        }
    }

    /**
     * Remove all DoT effects from an entity
     */
    public static void removeAllDots(UUID targetId) {
        activeDots.remove(targetId);
    }

    /**
     * Process DoT ticks for all active effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Process each entity's DoTs
        Iterator<Map.Entry<UUID, List<DoTInstance>>> entityIterator = activeDots.entrySet().iterator();

        while (entityIterator.hasNext()) {
            Map.Entry<UUID, List<DoTInstance>> entry = entityIterator.next();
            UUID targetId = entry.getKey();
            List<DoTInstance> dots = entry.getValue();

            // Find the target entity
            LivingEntity target = findEntity(targetId, event.getServer());
            if (target == null || !target.isAlive()) {
                entityIterator.remove();
                continue;
            }

            // Process each DoT
            Iterator<DoTInstance> dotIterator = dots.iterator();
            while (dotIterator.hasNext()) {
                DoTInstance dot = dotIterator.next();
                dot.tickCounter++;

                // Check if it's time to apply damage
                if (dot.tickCounter >= dot.tickInterval) {
                    dot.tickCounter = 0;
                    dot.remainingTicks--;

                    // Apply damage
                    applyDotDamage(target, dot);

                    // Remove if expired
                    if (dot.remainingTicks <= 0) {
                        dotIterator.remove();

                        if (ElementalReactionConfig.enableDebugLogging.get()) {
                            TalentsMod.LOGGER.debug("{} DoT expired on {}",
                                dot.reaction, target.getName().getString());
                        }
                    }
                }
            }

            // Remove entry if no more DoTs
            if (dots.isEmpty()) {
                entityIterator.remove();
            }
        }
    }

    /**
     * Apply damage from a DoT tick
     */
    private static void applyDotDamage(LivingEntity target, DoTInstance dot) {
        // Find caster for proper damage attribution
        ServerPlayer caster = findPlayer(dot.casterUUID, target.getServer());

        DamageSource source;
        if (dot.reaction == ElementalReaction.BURNING) {
            source = target.level().damageSources().onFire();
        } else if (dot.reaction == ElementalReaction.ELECTRO_CHARGED) {
            source = target.level().damageSources().lightningBolt();
        } else {
            source = target.level().damageSources().magic();
        }

        // Apply damage (will be properly attributed to caster if they exist)
        if (caster != null && caster.isAlive()) {
            source = target.level().damageSources().playerAttack(caster);
        }

        target.hurt(source, dot.damagePerTick);

        // Apply reaction-specific effects on tick
        switch (dot.reaction) {
            case BURNING -> {
                // Keep entity on fire
                if (target.getRemainingFireTicks() < 20) {
                    target.setRemainingFireTicks(20);
                }
            }
            case ELECTRO_CHARGED -> {
                // Chance to spread to nearby wet entities
                if (target.level().random.nextFloat() < 0.1f) { // 10% chance per tick
                    spreadElectroCharged(target, caster, dot.damagePerTick);
                }
            }
            default -> {
                // Other reactions don't have special tick effects
            }
        }
    }

    /**
     * Spread Electro-Charged to nearby wet entities
     */
    private static void spreadElectroCharged(LivingEntity source, ServerPlayer caster, float baseDamage) {
        float spreadRadius = 3.0f;

        source.level().getEntitiesOfClass(LivingEntity.class,
            source.getBoundingBox().inflate(spreadRadius),
            entity -> entity != source && entity.isInWaterOrRain())
            .forEach(entity -> {
                // Check if entity already has Electro-Charged
                UUID entityId = entity.getUUID();
                boolean hasElectroCharged = activeDots.containsKey(entityId) &&
                    activeDots.get(entityId).stream()
                        .anyMatch(dot -> dot.reaction == ElementalReaction.ELECTRO_CHARGED);

                if (!hasElectroCharged && caster != null) {
                    // Apply reduced damage for spread
                    int duration = ElementalReactionConfig.electroChargedDuration.get();
                    int tickRate = ElementalReactionConfig.electroChargedTickRate.get();
                    addDoT(entity, caster, ElementalReaction.ELECTRO_CHARGED,
                          baseDamage * 0.5f, duration, tickRate);
                }
            });
    }

    /**
     * Find an entity by UUID
     */
    private static LivingEntity findEntity(UUID uuid, net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof LivingEntity entity) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Find a player by UUID
     */
    private static ServerPlayer findPlayer(UUID uuid, net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayer(uuid);
    }

    /**
     * Check if an entity has an active DoT of a specific type
     */
    public static boolean hasDoT(UUID targetId, ElementalReaction reaction) {
        List<DoTInstance> dots = activeDots.get(targetId);
        return dots != null && dots.stream().anyMatch(dot -> dot.reaction == reaction);
    }

    /**
     * Get the total remaining damage for a DoT effect
     */
    public static float getRemainingDotDamage(UUID targetId, ElementalReaction reaction) {
        List<DoTInstance> dots = activeDots.get(targetId);
        if (dots != null) {
            return dots.stream()
                .filter(dot -> dot.reaction == reaction)
                .map(dot -> dot.damagePerTick * dot.remainingTicks)
                .findFirst()
                .orElse(0f);
        }
        return 0f;
    }

    /**
     * Clear all DoT data (used on world unload)
     */
    public static void clearAll() {
        activeDots.clear();
    }
}