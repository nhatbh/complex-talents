package com.complextalents.elemental.talents.mage.unleash;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementalReactionHandler;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.superreaction.SuperReactionHandler;
import com.complextalents.talent.BranchingHybridTalent;
import com.complextalents.talent.TalentBranches;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Elemental Unleash - Core combo mechanic
 * Crescendo Slot talent with toggle functionality
 * While active: Prevents basic reactions, drains Focus, allows detonation of stacks
 */
public class ElementalUnleashTalent extends BranchingHybridTalent {

    // Toggle state tracking for each player
    private static final Map<UUID, UnleashState> unleashStates = new HashMap<>();

    // Base values per rank
    private static final float[] FOCUS_COST = {40f, 35f, 30f, 25f};
    private static final float[] FOCUS_DRAIN_PER_SECOND = {12f, 10f, 8f, 5f};
    private static final int[] COOLDOWN_SECONDS = {30, 20, 15, 10};

    // Rank 2 branch values
    private static final float[] CHAIN_DAMAGE_PERCENT = {0.60f, 0.75f, 0.90f, 1.10f};
    private static final float[] LINGERING_DRAIN = {8f, 6f, 4f, 2f};
    private static final int[] LINGERING_DECAY_SECONDS = {8, 10, 12, 15};

    // Rank 3 branch values
    private static final int[] OVERLOAD_JUMPS = {2, 3, 4, 5};
    private static final float[] OVERLOAD_DECAY_PER_JUMP = {0.50f, 0.40f, 0.30f, 0.20f};
    private static final float[] AMPLIFICATION_MAX_BONUS = {0.80f, 1.20f, 1.60f, 2.50f};

    public ElementalUnleashTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_unleash"),
            Component.translatable("talent.complextalents.elemental_unleash"),
            Component.translatable("talent.complextalents.elemental_unleash.desc"),
            4,
            TalentSlotType.CRESCENDO,
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition"),
            COOLDOWN_SECONDS[0] * 20 // Base cooldown in ticks
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Initialize unleash state
        unleashStates.putIfAbsent(player.getUUID(), new UnleashState());

        TalentsMod.LOGGER.info("Player {} unlocked Elemental Unleash at level {}",
            player.getName().getString(), level);
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Clean up unleash state
        UnleashState state = unleashStates.get(player.getUUID());
        if (state != null && state.isActive) {
            deactivateUnleash(player, false); // Don't detonate on removal
        }
        unleashStates.remove(player.getUUID());
    }

    @Override
    public void onActivate(ServerPlayer player, int level) {
        UnleashState state = unleashStates.get(player.getUUID());
        if (state == null) {
            state = new UnleashState();
            unleashStates.put(player.getUUID(), state);
        }

        if (!state.isActive) {
            // Activate Elemental Unleash
            activateUnleash(player, level);
        } else {
            // Deactivate and detonate
            deactivateUnleash(player, true);
        }
    }

    private void activateUnleash(ServerPlayer player, int level) {
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            float activationCost = FOCUS_COST[Math.min(level - 1, 3)];

            // Check if player has enough Focus
            if (!talents.hasResource(activationCost)) {
                player.sendSystemMessage(Component.translatable("message.complextalents.not_enough_focus"));
                return;
            }

            // Consume Focus and activate
            talents.consumeResource(activationCost);

            UnleashState state = unleashStates.get(player.getUUID());
            state.isActive = true;
            state.level = level;
            state.activationTime = System.currentTimeMillis();
            state.clearMarks(); // Clear any previous marks

            player.sendSystemMessage(Component.translatable("message.complextalents.unleash_activated"));

            TalentsMod.LOGGER.debug("Elemental Unleash activated for {} at level {}",
                player.getName().getString(), level);
        });
    }

    private void deactivateUnleash(ServerPlayer player, boolean detonate) {
        UnleashState state = unleashStates.get(player.getUUID());
        if (state == null || !state.isActive) return;

        state.isActive = false;

        if (detonate) {
            detonateStacks(player, state.level);
        }

        // Clear all glowing effects from marked targets
        clearAllGlowingEffects(player, state);

        // Clear marked targets
        state.clearMarks();

        // Set cooldown
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            int cooldownTicks = COOLDOWN_SECONDS[Math.min(state.level - 1, 3)] * 20;
            talents.setTalentCooldown(this.getId(), cooldownTicks);
        });

        player.sendSystemMessage(Component.translatable("message.complextalents.unleash_deactivated"));

        TalentsMod.LOGGER.debug("Elemental Unleash deactivated for {}",
            player.getName().getString());
    }

    private void clearAllGlowingEffects(ServerPlayer player, UnleashState state) {
        // Remove glowing effect from all marked targets
        for (UUID targetId : state.markedTargets) {
            player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(100), // Large radius to catch all
                entity -> entity.getUUID().equals(targetId)
            ).forEach(entity -> entity.setGlowingTag(false));
        }
    }

    private void detonateStacks(ServerPlayer player, int level) {
        UnleashState state = unleashStates.get(player.getUUID());
        if (state == null) return;

        // Find all MARKED entities with elemental stacks
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(100), // Large radius to find all marked targets
            entity -> entity != player && state.isMarked(entity.getUUID()) &&
                     ElementalStackManager.hasAnyStack(entity.getUUID())
        );

        for (LivingEntity target : targets) {
            Map<ElementType, Integer> stacks = ElementalStackManager.getEntityStacks(target.getUUID());
            int uniqueElements = stacks.size();

            if (uniqueElements >= 2) {
                // Determine reaction type based on element count
                if (uniqueElements == 2) {
                    // Basic reaction
                    triggerBasicReaction(player, target, stacks);
                } else {
                    // Super reaction (3+ elements)
                    triggerSuperReaction(player, target, stacks, uniqueElements, level);
                }
            }

            // Remove glow effect after detonation
            target.setGlowingTag(false);
            state.unmarkTarget(target.getUUID());
        }
    }

    private void triggerBasicReaction(ServerPlayer player, LivingEntity target, Map<ElementType, Integer> stacks) {
        // Get the two elements present
        List<ElementType> elements = new ArrayList<>(stacks.keySet());
        if (elements.size() < 2) {
            return; // Safety check
        }

        ElementType element1 = elements.get(0);
        ElementType element2 = elements.get(1);

        // Determine which element triggered the reaction (used for mastery scaling)
        ElementType triggeringElement = element1;

        // Get the reaction type between these two elements
        ElementalReaction reaction = element1.getReactionWith(element2);
        if (reaction == null) {
            TalentsMod.LOGGER.warn("No reaction found between {} and {}", element1, element2);
            return;
        }

        // Calculate base damage (using a standard value for detonation)
        float baseDamage = 10f;

        // Trigger the reaction through ElementalReactionHandler
        ElementalReactionHandler.triggerReaction(target, reaction, triggeringElement, element2, player, baseDamage);

        // Check for Chain Detonation (Rank 2A)
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            int level = talents.getTalentLevel(this.getId());
            if (level >= 2 && TalentBranches.hasBranch(player, this.getId(), 2, TalentBranches.BranchChoice.PATH_A)) {
                applyChainDetonationBasic(player, target, level, reaction, baseDamage);
            }
        });

        TalentsMod.LOGGER.debug("Basic reaction {} detonated on {} by {}",
            reaction.name(), target.getName().getString(), player.getName().getString());
    }

    private void applyChainDetonationBasic(ServerPlayer player, LivingEntity initialTarget, int level,
                                           ElementalReaction reaction, float baseDamage) {
        // Chain basic reactions to nearby enemies with reduced damage
        float chainDamagePercent = CHAIN_DAMAGE_PERCENT[Math.min(level - 1, 3)];
        int chainRange = 10;
        int maxChains = 3;

        List<LivingEntity> nearbyTargets = player.level().getEntitiesOfClass(
            LivingEntity.class,
            initialTarget.getBoundingBox().inflate(chainRange),
            entity -> entity != player && entity != initialTarget && ElementalStackManager.hasAnyStack(entity)
        );

        int chains = 0;
        for (LivingEntity chainTarget : nearbyTargets) {
            if (chains >= maxChains) break;

            Map<ElementType, Integer> chainStacks = ElementalStackManager.getEntityStacks(chainTarget);
            if (chainStacks.size() >= 2) {
                // Get first two elements
                List<ElementType> chainElements = new ArrayList<>(chainStacks.keySet());
                ElementType elem1 = chainElements.get(0);
                ElementType elem2 = chainElements.get(1);

                ElementalReaction chainReaction = elem1.getReactionWith(elem2);
                if (chainReaction != null) {
                    // Apply reduced damage
                    float chainDamage = baseDamage * chainDamagePercent;
                    ElementalReactionHandler.triggerReaction(chainTarget, chainReaction, elem1, elem2, player, chainDamage);
                    chains++;
                }
            }
        }
    }

    private void triggerSuperReaction(ServerPlayer player, LivingEntity target,
                                     Map<ElementType, Integer> stacks, int uniqueElements, int level) {
        // Get the element that was applied last (stored in state)
        UnleashState state = unleashStates.get(player.getUUID());
        ElementType triggeringElement = state != null ? state.getLastElement() : null;

        if (triggeringElement == null) {
            // Fallback to element with highest stack count
            triggeringElement = stacks.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ElementType.FIRE);
        }

        // Trigger the Super-Reaction through the handler
        boolean triggered = SuperReactionHandler.checkAndTrigger(player, target, triggeringElement);

        if (triggered) {
            // Apply branch-specific bonuses
            applyBranchBonuses(player, target, level, uniqueElements);

            // Track element in history
            state.addElement(triggeringElement);
        }

        TalentsMod.LOGGER.debug("Super reaction ({} elements) detonated on {} by {}",
            uniqueElements, target.getName().getString(), player.getName().getString());
    }

    private void applyBranchBonuses(ServerPlayer player, LivingEntity target, int level, int uniqueElements) {
        // Check for chain reaction (Rank 2A: Chain Detonation)
        if (level >= 2 && TalentBranches.hasBranch(player, this.getId(), 2, TalentBranches.BranchChoice.PATH_A)) {
            applyChainDetonation(player, target, level, uniqueElements);
        }

        // Check for lingering chaos (Rank 2B: Lingering Chaos)
        if (level >= 2 && TalentBranches.hasBranch(player, this.getId(), 2, TalentBranches.BranchChoice.PATH_B)) {
            applyLingeringChaos(player, target, level);
        }

        // Check for overload (Rank 3A: Elemental Overload)
        if (level >= 3 && TalentBranches.hasBranch(player, this.getId(), 3, TalentBranches.BranchChoice.PATH_A)) {
            applyElementalOverload(player, target, level, uniqueElements);
        }

        // Check for amplification (Rank 3B: Elemental Amplification)
        if (level >= 3 && TalentBranches.hasBranch(player, this.getId(), 3, TalentBranches.BranchChoice.PATH_B)) {
            applyElementalAmplification(player, target, level);
        }
    }

    private void applyChainDetonation(ServerPlayer player, LivingEntity initialTarget, int level, int uniqueElements) {
        // Chain jumps to nearby enemies with reduced damage
        float chainDamagePercent = CHAIN_DAMAGE_PERCENT[Math.min(level - 1, 3)];
        int chainRange = 10;
        int maxChains = 3;

        List<LivingEntity> nearbyTargets = player.level().getEntitiesOfClass(
            LivingEntity.class,
            initialTarget.getBoundingBox().inflate(chainRange),
            entity -> entity != player && entity != initialTarget && ElementalStackManager.hasAnyStack(entity)
        );

        int chains = 0;
        for (LivingEntity chainTarget : nearbyTargets) {
            if (chains >= maxChains) break;

            // Trigger a weaker super reaction on chain targets
            if (SuperReactionHandler.canTriggerSuperReaction(chainTarget)) {
                // Get the primary element for chain target
                ElementType chainElement = ElementalStackManager.getEntityStacks(chainTarget).entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

                if (chainElement != null) {
                    SuperReactionHandler.checkAndTrigger(player, chainTarget, chainElement);
                    chains++;
                }
            }
        }
    }

    private void applyLingeringChaos(ServerPlayer player, LivingEntity target, int level) {
        // Apply lingering Focus drain reduction
        float lingeringDrain = LINGERING_DRAIN[Math.min(level - 1, 3)];
        int decayDuration = LINGERING_DECAY_SECONDS[Math.min(level - 1, 3)] * 20;

        // Create lingering elemental field effect at target location
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 targetPos = target.position();

            // Create AreaEffectCloud for the lingering field
            AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, targetPos.x, targetPos.y, targetPos.z);
            cloud.setRadius(4.0f); // 4 block radius
            cloud.setDuration(decayDuration); // Duration from config
            cloud.setRadiusPerTick(-0.005f); // Slowly shrink over time
            cloud.setWaitTime(0); // No delay before effect starts
            cloud.setRadiusOnUse(0f); // Don't shrink on use

            // Set visual particles (portal particles for chaos effect)
            cloud.setParticle(ParticleTypes.PORTAL);

            // Store custom data to identify this as a Lingering Chaos cloud
            cloud.getPersistentData().putBoolean("lingering_chaos", true);
            cloud.getPersistentData().putUUID("caster_uuid", player.getUUID());
            cloud.getPersistentData().putInt("reapply_interval", 20); // Reapply every 1 second
            cloud.getPersistentData().putLong("last_reapply", serverLevel.getGameTime());

            // Spawn the cloud
            serverLevel.addFreshEntity(cloud);

            TalentsMod.LOGGER.debug("Lingering Chaos field created at {} with duration {}s",
                target.getName().getString(), decayDuration / 20);
        }
    }

    private void applyElementalOverload(ServerPlayer player, LivingEntity target, int level, int uniqueElements) {
        // Multi-hit with damage decay
        int overloadJumps = OVERLOAD_JUMPS[Math.min(level - 1, 3)];
        float decayPerJump = OVERLOAD_DECAY_PER_JUMP[Math.min(level - 1, 3)];

        // Get the triggering element from state
        UnleashState state = unleashStates.get(player.getUUID());
        ElementType triggeringElement = state != null ? state.getLastElement() : null;

        if (triggeringElement == null) {
            TalentsMod.LOGGER.warn("No triggering element found for Elemental Overload");
            return;
        }

        // Store the element stacks before they get cleared by the first hit
        Map<ElementType, Integer> originalStacks = ElementalStackManager.getEntityStacks(target);

        // Schedule additional hits with damage decay
        for (int i = 1; i < overloadJumps; i++) {
            final int hitIndex = i;
            final float damageMultiplier = 1.0f - (decayPerJump * hitIndex);

            // Schedule hit with delay (2 ticks per hit = 0.1 seconds)
            player.getServer().tell(new net.minecraft.server.TickTask(
                player.getServer().getTickCount() + (i * 2),
                () -> {
                    // Check if target is still valid
                    if (!target.isAlive() || target.isRemoved()) {
                        return;
                    }

                    // Re-apply element stacks (they were consumed by previous hit)
                    for (Map.Entry<ElementType, Integer> entry : originalStacks.entrySet()) {
                        ElementalStackManager.setStacks(target, entry.getKey(), entry.getValue());
                    }

                    // Store damage multiplier in player persistent data for SuperReactionHandler to use
                    player.getPersistentData().putFloat("overload_damage_multiplier", damageMultiplier);

                    // Trigger the super-reaction again
                    SuperReactionHandler.checkAndTrigger(player, target, triggeringElement);

                    TalentsMod.LOGGER.debug("Elemental Overload hit {} of {} ({}% damage)",
                        hitIndex + 1, overloadJumps, (int)(damageMultiplier * 100));
                }
            ));
        }

        TalentsMod.LOGGER.debug("Elemental Overload triggered with {} jumps", overloadJumps);
    }

    private void applyElementalAmplification(ServerPlayer player, LivingEntity target, int level) {
        // Damage bonus based on Focus consumed during Unleash activation
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            float currentFocus = talents.getResource();
            float maxFocus = talents.getMaxResource();

            // Calculate Focus consumed (capped at 50% of max Focus)
            UnleashState state = unleashStates.get(player.getUUID());
            if (state == null) return;

            // Time Unleash has been active (in seconds)
            long activeTime = System.currentTimeMillis() - state.activationTime;
            float activeSeconds = activeTime / 1000f;

            // Calculate total Focus consumed (drain rate × time)
            float drainPerSecond = FOCUS_DRAIN_PER_SECOND[Math.min(level - 1, 3)];
            float focusConsumed = drainPerSecond * activeSeconds;

            // Cap consumed Focus at 50% of max Focus
            float maxConsumable = maxFocus * 0.5f;
            focusConsumed = Math.min(focusConsumed, maxConsumable);

            // Calculate amplification percentage based on consumed Focus
            // Formula: (Focus Consumed / Max Consumable) × Max Bonus
            float consumptionPercent = focusConsumed / maxConsumable;
            float maxBonus = AMPLIFICATION_MAX_BONUS[Math.min(level - 1, 3)];
            float amplificationMultiplier = maxBonus * consumptionPercent;

            // Store amplification multiplier in player persistent data for SuperReactionHandler
            player.getPersistentData().putFloat("amplification_multiplier", amplificationMultiplier);

            TalentsMod.LOGGER.debug("Elemental Amplification: {}% damage bonus (consumed {} Focus over {}s)",
                (int)(amplificationMultiplier * 100), focusConsumed, activeSeconds);
        });
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UnleashState state = unleashStates.get(player.getUUID());
        if (state != null && state.isActive) {
            // Drain Focus while active
            player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                float drainPerTick = FOCUS_DRAIN_PER_SECOND[Math.min(state.level - 1, 3)] / 20f;

                if (!talents.consumeResource(drainPerTick)) {
                    // Out of Focus, auto-detonate
                    deactivateUnleash(player, true);
                }
            });

            // Update visual marking for entities with stacks
            updateMarkedTargets(player, state);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }

        if (event.level instanceof ServerLevel serverLevel) {
            // Process all area effect clouds by iterating through all entities
            for (net.minecraft.world.entity.Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof AreaEffectCloud cloud) {
                    // Process Lingering Chaos clouds
                    if (cloud.getPersistentData().contains("lingering_chaos")) {
                        processLingeringChaosCloud(serverLevel, cloud);
                    }
                    // Process Lava Pool zones (Fire Tier 2)
                    else if (cloud.getPersistentData().contains("lava_pool")) {
                        processDamageZone(serverLevel, cloud);
                    }
                    // Process Scorched Earth zones (Fire Tier 3)
                    else if (cloud.getPersistentData().contains("scorched_earth")) {
                        processDamageZone(serverLevel, cloud);
                    }
                    // Process Slow Field zones (Aqua Tier 2)
                    else if (cloud.getPersistentData().contains("slow_field")) {
                        processSlowField(serverLevel, cloud);
                    }
                    // Process Great Flood zones (Aqua Tier 4)
                    else if (cloud.getPersistentData().contains("great_flood")) {
                        processGreatFlood(serverLevel, cloud);
                    }
                    // Process Jungle zones (Nature Tier 2)
                    else if (cloud.getPersistentData().contains("jungle_zone")) {
                        processJungleZone(serverLevel, cloud);
                    }
                }
            }
        }
    }

    private void processLingeringChaosCloud(ServerLevel level, AreaEffectCloud cloud) {
        long currentTime = level.getGameTime();
        long lastReapply = cloud.getPersistentData().getLong("last_reapply");
        int reapplyInterval = cloud.getPersistentData().getInt("reapply_interval");

        // Check if it's time to reapply elements
        if (currentTime - lastReapply >= reapplyInterval) {
            cloud.getPersistentData().putLong("last_reapply", currentTime);

            // Get caster UUID
            UUID casterUUID = cloud.getPersistentData().getUUID("caster_uuid");
            ServerPlayer caster = casterUUID != null ? level.getServer().getPlayerList().getPlayer(casterUUID) : null;

            if (caster == null) {
                // Caster is offline, remove cloud
                cloud.discard();
                return;
            }

            // Find all living entities within the cloud radius
            List<LivingEntity> entitiesInCloud = level.getEntitiesOfClass(
                LivingEntity.class,
                cloud.getBoundingBox().inflate(cloud.getRadius()),
                entity -> entity.distanceToSqr(cloud.position()) <= cloud.getRadius() * cloud.getRadius()
            );

            // Apply random element stacks to each entity
            for (LivingEntity entity : entitiesInCloud) {
                if (entity == caster) continue; // Don't affect caster

                // Pick a random element
                ElementType[] elements = ElementType.values();
                ElementType randomElement = elements[level.random.nextInt(elements.length)];

                // Apply 1 stack of random element
                ElementalStackManager.applyElementStack(entity, randomElement, caster, 1.0f);
            }
        }
    }

    /**
     * Process damage zones (Lava Pool and Scorched Earth)
     */
    private void processDamageZone(ServerLevel level, AreaEffectCloud cloud) {
        long currentTime = level.getGameTime();
        long lastDamageTime = cloud.getPersistentData().getLong("last_damage_time");
        int damageInterval = cloud.getPersistentData().getInt("damage_interval");

        // Check if it's time to apply damage
        if (currentTime - lastDamageTime >= damageInterval) {
            cloud.getPersistentData().putLong("last_damage_time", currentTime);

            float dotDamage = cloud.getPersistentData().getFloat("dot_damage");
            UUID casterUUID = cloud.getPersistentData().getUUID("caster_uuid");
            ServerPlayer caster = casterUUID != null ? level.getServer().getPlayerList().getPlayer(casterUUID) : null;

            if (caster == null) {
                // Caster is offline, remove zone
                cloud.discard();
                return;
            }

            // Find all living entities within the zone radius
            List<LivingEntity> entitiesInZone = level.getEntitiesOfClass(
                LivingEntity.class,
                cloud.getBoundingBox().inflate(cloud.getRadius()),
                entity -> entity.distanceToSqr(cloud.position()) <= cloud.getRadius() * cloud.getRadius()
            );

            // Apply damage to each entity
            for (LivingEntity entity : entitiesInZone) {
                if (entity == caster) continue; // Don't damage caster

                DamageSource damageSource = entity.damageSources().playerAttack(caster);
                entity.hurt(damageSource, dotDamage);

                // Set on fire if it's a lava pool
                if (cloud.getPersistentData().contains("lava_pool")) {
                    entity.setSecondsOnFire(2); // Keep entities burning
                }
            }
        }
    }

    /**
     * Process Slow Field zones (Aqua Tier 2)
     * Applies 17% slow (Slowness I = ~15%, close enough) to enemies every 0.5s
     */
    private void processSlowField(ServerLevel level, AreaEffectCloud cloud) {
        long currentTime = level.getGameTime();
        long lastEffectTime = cloud.getPersistentData().getLong("last_effect_time");
        int effectInterval = cloud.getPersistentData().getInt("effect_interval");

        // Check if it's time to apply slow
        if (currentTime - lastEffectTime >= effectInterval) {
            cloud.getPersistentData().putLong("last_effect_time", currentTime);

            UUID casterUUID = cloud.getPersistentData().getUUID("caster_uuid");
            ServerPlayer caster = casterUUID != null ? level.getServer().getPlayerList().getPlayer(casterUUID) : null;

            if (caster == null) {
                // Caster is offline, remove zone
                cloud.discard();
                return;
            }

            // Find all living entities within the zone radius
            List<LivingEntity> entitiesInZone = level.getEntitiesOfClass(
                LivingEntity.class,
                cloud.getBoundingBox().inflate(cloud.getRadius()),
                entity -> entity.distanceToSqr(cloud.position()) <= cloud.getRadius() * cloud.getRadius()
            );

            // Apply slow to each entity
            for (LivingEntity entity : entitiesInZone) {
                if (entity == caster) continue; // Don't slow caster

                // Apply Slowness I (17% slow, close to the 15% actual value)
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectInterval + 5, 0));
            }
        }
    }

    /**
     * Process Great Flood zones (Aqua Tier 4)
     * Applies 14% slow to enemies and 30% speed to player every 0.5s
     */
    private void processGreatFlood(ServerLevel level, AreaEffectCloud cloud) {
        long currentTime = level.getGameTime();
        long lastEffectTime = cloud.getPersistentData().getLong("last_effect_time");
        int effectInterval = cloud.getPersistentData().getInt("effect_interval");

        // Check if it's time to apply effects
        if (currentTime - lastEffectTime >= effectInterval) {
            cloud.getPersistentData().putLong("last_effect_time", currentTime);

            UUID casterUUID = cloud.getPersistentData().getUUID("caster_uuid");
            ServerPlayer caster = casterUUID != null ? level.getServer().getPlayerList().getPlayer(casterUUID) : null;

            if (caster == null) {
                // Caster is offline, remove zone
                cloud.discard();
                return;
            }

            // Find all living entities within the zone radius
            List<LivingEntity> entitiesInZone = level.getEntitiesOfClass(
                LivingEntity.class,
                cloud.getBoundingBox().inflate(cloud.getRadius()),
                entity -> entity.distanceToSqr(cloud.position()) <= cloud.getRadius() * cloud.getRadius()
            );

            // Apply effects to each entity
            for (LivingEntity entity : entitiesInZone) {
                if (entity == caster) {
                    // Apply 30% speed to caster (Speed I = 20%, Speed II = 40%, so use Speed I)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effectInterval + 5, 0));
                } else {
                    // Apply 14% slow to enemies (Slowness I = ~15%, close enough)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectInterval + 5, 0));
                }
            }
        }
    }

    /**
     * Process Jungle zones (Nature Tier 2)
     * Applies damage + 1s root + silence effect every 1s
     */
    private void processJungleZone(ServerLevel level, AreaEffectCloud cloud) {
        long currentTime = level.getGameTime();
        long lastEffectTime = cloud.getPersistentData().getLong("last_effect_time");
        int effectInterval = cloud.getPersistentData().getInt("effect_interval");

        // Check if it's time to apply effects
        if (currentTime - lastEffectTime >= effectInterval) {
            cloud.getPersistentData().putLong("last_effect_time", currentTime);

            float zoneDamage = cloud.getPersistentData().getFloat("zone_damage");
            int rootDuration = cloud.getPersistentData().getInt("root_duration");
            UUID casterUUID = cloud.getPersistentData().getUUID("caster_uuid");
            ServerPlayer caster = casterUUID != null ? level.getServer().getPlayerList().getPlayer(casterUUID) : null;

            if (caster == null) {
                // Caster is offline, remove zone
                cloud.discard();
                return;
            }

            // Find all living entities within the zone radius
            List<LivingEntity> entitiesInZone = level.getEntitiesOfClass(
                LivingEntity.class,
                cloud.getBoundingBox().inflate(cloud.getRadius()),
                entity -> entity.distanceToSqr(cloud.position()) <= cloud.getRadius() * cloud.getRadius()
            );

            // Apply effects to each entity
            for (LivingEntity entity : entitiesInZone) {
                if (entity == caster) continue; // Don't affect caster

                // Deal zone damage
                DamageSource damageSource = entity.damageSources().playerAttack(caster);
                entity.hurt(damageSource, zoneDamage);

                // Apply root effect (Slowness IV + Jump boost -10)
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootDuration, 3));
                entity.addEffect(new MobEffectInstance(MobEffects.JUMP, rootDuration, -10));

                // Apply silence effect (Mining Fatigue to simulate inability to cast)
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, rootDuration, 2));
            }
        }
    }

    /**
     * Update visual marking for marked enemies
     * Only marked enemies will glow and be detonated
     */
    private void updateMarkedTargets(ServerPlayer player, UnleashState state) {
        // Unmark targets that no longer have stacks
        Set<UUID> toUnmark = new HashSet<>();
        for (UUID markedId : state.markedTargets) {
            boolean stillExists = false;

            // Check if entity still exists and has stacks
            List<LivingEntity> found = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(100),
                entity -> entity.getUUID().equals(markedId)
            );

            for (LivingEntity entity : found) {
                if (ElementalStackManager.hasAnyStack(entity.getUUID())) {
                    stillExists = true;
                } else {
                    // Lost stacks, remove glow
                    entity.setGlowingTag(false);
                }
            }

            if (!stillExists) {
                toUnmark.add(markedId);
            }
        }

        // Clean up unmarked targets
        toUnmark.forEach(state::unmarkTarget);
    }

    /**
     * Check if Elemental Unleash is active for a player
     */
    public static boolean isUnleashActive(ServerPlayer player) {
        UnleashState state = unleashStates.get(player.getUUID());
        return state != null && state.isActive;
    }

    /**
     * Get the current level of active Unleash
     */
    public static int getActiveUnleashLevel(ServerPlayer player) {
        UnleashState state = unleashStates.get(player.getUUID());
        return (state != null && state.isActive) ? state.level : 0;
    }

    /**
     * Mark a target entity for detonation when Unleash is deactivated
     * Called by ElementalStackManager when stacks are applied while Unleash is active
     */
    public static void markTargetForUnleash(ServerPlayer player, LivingEntity target) {
        UnleashState state = unleashStates.get(player.getUUID());
        if (state != null && state.isActive) {
            state.markTarget(target.getUUID());
            target.setGlowingTag(true);
        }
    }

    /**
     * Internal state tracking for Elemental Unleash
     */
    private static class UnleashState {
        boolean isActive = false;
        int level = 0;
        long activationTime = 0;
        List<ElementType> elementHistory = new ArrayList<>();
        Set<UUID> markedTargets = new HashSet<>(); // Track marked enemies

        void addElement(ElementType element) {
            elementHistory.add(element);
            // Keep only last 6 elements
            if (elementHistory.size() > 6) {
                elementHistory.remove(0);
            }
        }

        ElementType getLastElement() {
            return elementHistory.isEmpty() ? null : elementHistory.get(elementHistory.size() - 1);
        }

        int getUniqueElementCount() {
            return new HashSet<>(elementHistory).size();
        }

        void markTarget(UUID entityId) {
            markedTargets.add(entityId);
        }

        void unmarkTarget(UUID entityId) {
            markedTargets.remove(entityId);
        }

        boolean isMarked(UUID entityId) {
            return markedTargets.contains(entityId);
        }

        void clearMarks() {
            markedTargets.clear();
        }
    }

    @Override
    public boolean hasBranchingAtRank(int rank) {
        return rank == 2 || rank == 3 || rank == 4;
    }

    @Override
    public Component getBranchDescription(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_unleash.rank%d.%s.desc",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }

    @Override
    public Component getBranchName(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_unleash.rank%d.%s",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }
}