package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.api.events.ElementalReactionEvent;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.effects.ModEffects;
import com.complextalents.elemental.entity.BloomCoreEntity;
import com.complextalents.elemental.entity.SteamCloudEntity;
import com.complextalents.elemental.talents.mage.ElementalMageDefinition;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnParticlesPacket;
import com.complextalents.network.SpawnReactionTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

/**
 * Handles triggering and calculating elemental reactions
 * Uses the new attribute-based mastery system for scaling
 */
public class ElementalReactionHandler {

    /**
     * Triggers an elemental reaction between two elements
     * @param target The entity being affected
     * @param reaction The type of reaction triggered
     * @param triggeringElement The element that triggered the reaction
     * @param existingElement The element that was already on the target
     * @param attacker The player causing the reaction
     * @param triggeringSpellDamage The damage of the spell that triggered the reaction
     */
    public static void triggerReaction(LivingEntity target, ElementalReaction reaction,
                                     ElementType triggeringElement, ElementType existingElement,
                                     ServerPlayer attacker, float triggeringSpellDamage) {

        if (!ElementalReactionConfig.enableElementalSystem.get()) return;

        // Check for friendly fire protection
        if (ElementalReactionConfig.enableFriendlyFireProtection.get() && isTeammate(attacker, target)) {
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Reaction {} blocked by friendly fire protection: {} and {} are teammates",
                                      reaction, attacker.getName().getString(), target.getName().getString());
            }
            return;
        }

        // Calculate reaction damage using new formula
        float reactionDamage = calculateReactionDamage(reaction, triggeringElement,
                                                      triggeringSpellDamage, attacker);

        // Apply damage for instant-damage reactions
        if (reaction.isAmplifying() && !reaction.isSpawn()) {
            DamageSource damageSource = target.level().damageSources().magic();
            target.hurt(damageSource, reactionDamage);
        }

        // Apply reaction-specific effects
        applyReactionEffects(target, reaction, attacker, reactionDamage);

        // Spawn particle effects for the reaction
        if (target.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = target.position().add(0, target.getBbHeight() / 2, 0);
            SpawnParticlesPacket packet = new SpawnParticlesPacket(particlePos, reaction);
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);

            // Spawn floating reaction text
            SpawnReactionTextPacket textPacket = new SpawnReactionTextPacket(target, reaction, reactionDamage);
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), textPacket);
        }

        // Fire event for other mods
        ElementalReactionEvent event = new ElementalReactionEvent(target, attacker, reaction,
                                                                  reactionDamage, existingElement, triggeringElement);
        MinecraftForge.EVENT_BUS.post(event);

        // Generate Focus for Elemental Mage
        generateFocusFromReaction(attacker, reaction, false);

        // Consume elemental stacks
        ElementalStackManager.clearEntityStacks(target.getUUID());

        if (ElementalReactionConfig.enableDebugLogging.get()) {
            TalentsMod.LOGGER.info("Reaction {} triggered by {} on {}, damage: {}",
                                  reaction, attacker.getName().getString(),
                                  target.getName().getString(), reactionDamage);
        }
    }

    /**
     * Calculates reaction damage using the new mastery system
     * Formula: baseDamage = triggeringDamage * baseMultiplier
     *          generalBonus = generalMastery / (generalMastery + scaling)
     *          specificBonus = specificMastery / (specificMastery + scaling)
     *          finalDamage = baseDamage * (1 + generalBonus) * (1 + specificBonus)
     */
    private static float calculateReactionDamage(ElementalReaction reaction, ElementType element,
                                                float triggeringDamage, ServerPlayer attacker) {
        // Get base multiplier from config
        double baseMultiplier = ElementalReactionConfig.getReactionMultiplier(reaction);
        float baseDamage = triggeringDamage * (float) baseMultiplier;

        // Get mastery values from attributes
        double generalMastery = attacker.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        double specificMastery = getSpecificMasteryForElement(attacker, element);

        // Get scaling constants from config
        double generalScaling = ElementalReactionConfig.generalMasteryScaling.get();
        double specificScaling = ElementalReactionConfig.specificMasteryScaling.get();

        // Calculate combined multiplier
        double multiplier = MasteryAttributes.getCombinedMasteryMultiplier(
            generalMastery, specificMastery, generalScaling, specificScaling);

        return baseDamage * (float) multiplier;
    }

    /**
     * Gets the specific mastery attribute value for an element
     */
    private static double getSpecificMasteryForElement(ServerPlayer player, ElementType element) {
        Attribute masteryAttribute = switch (element) {
            case FIRE -> MasteryAttributes.FIRE_MASTERY.get();
            case AQUA -> MasteryAttributes.AQUA_MASTERY.get();
            case LIGHTNING -> MasteryAttributes.LIGHTNING_MASTERY.get();
            case ICE -> MasteryAttributes.ICE_MASTERY.get();
            case NATURE -> MasteryAttributes.NATURE_MASTERY.get();
            case ENDER -> MasteryAttributes.ENDER_MASTERY.get();
        };

        return player.getAttributeValue(masteryAttribute);
    }

    /**
     * Check if the target is a teammate of the attacker
     * Uses Minecraft's team system
     */
    private static boolean isTeammate(ServerPlayer attacker, LivingEntity target) {
        // Target must be on the same team
        // Minecraft's isAlliedTo checks team membership and friendly fire rules
        return attacker.isAlliedTo(target);
    }

    /**
     * Applies reaction-specific status effects and mechanics
     * This is where each reaction's unique behavior is implemented
     */
    private static void applyReactionEffects(LivingEntity target, ElementalReaction reaction,
                                           ServerPlayer attacker, float damage) {
        switch (reaction) {
            case VAPORIZE -> applyVaporize(target, attacker);
            case MELT -> applyMelt(target);
            case OVERLOADED -> applyOverloaded(target, attacker);
            case ELECTRO_CHARGED -> applyElectroCharged(target, attacker, damage);
            case FROZEN -> applyFrozen(target);
            case SUPERCONDUCT -> applySuperconduct(target);
            case BURNING -> applyBurning(target, attacker, damage);
            case BLOOM -> applyBloom(target, attacker);
            case HYPERBLOOM -> applyHyperbloom(target, attacker, damage);
            case BURGEON -> applyBurgeon(target, attacker, damage);
            // Ender reactions
            case UNSTABLE_WARD -> applyUnstableWard(target, attacker);
            case RIFT_PULL -> applyRiftPull(target, attacker);
            case SINGULARITY -> applySingularity(target, attacker);
            case FRACTURE -> applyFracture(target);
            case WITHERING_SEED -> applyWitheringSeed(target, attacker);
            case DECREPIT_GRASP -> applyDecrepitGrasp(target);
        }
    }

    // ===== Reaction Effect Implementations =====
    // These are placeholder implementations - full mechanics will be added in subsequent tasks

    private static void applyVaporize(LivingEntity target, ServerPlayer attacker) {
        // Spawn steam cloud entity
        if (target.level() instanceof ServerLevel serverLevel) {
            SteamCloudEntity steamCloud = new SteamCloudEntity(serverLevel, target.position(), attacker);
            serverLevel.addFreshEntity(steamCloud);
        }
    }

    private static void applyMelt(LivingEntity target) {
        // Apply Frostbite custom effect (armor reduction)
        int duration = ElementalReactionConfig.meltFrostbiteDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.FROSTBITE.get(), duration, 0));
    }

    private static void applyOverloaded(LivingEntity target, ServerPlayer attacker) {
        // Apply AoE damage to nearby entities
        float aoeRadius = ElementalReactionConfig.overloadedAoeRadius.get().floatValue();
        float aoeDamageMultiplier = 0.5f; // 50% damage to secondary targets

        // Calculate the damage for AoE
        float baseDamage = calculateReactionDamage(ElementalReaction.OVERLOADED, ElementType.LIGHTNING,
                                                   10.0f, attacker); // Base spell damage for calculation
        float aoeDamage = baseDamage * aoeDamageMultiplier;

        // Find and damage nearby entities
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(aoeRadius),
                entity -> entity != target && entity != attacker &&
                         !isTeammate(attacker, entity))
                .forEach(entity -> {
                    // Apply damage
                    DamageSource damageSource = target.level().damageSources().magic();
                    entity.hurt(damageSource, aoeDamage);

                    // Apply knockback from explosion center
                    double dx = entity.getX() - target.getX();
                    double dz = entity.getZ() - target.getZ();
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 0) {
                        double strength = ElementalReactionConfig.overloadedKnockbackStrength.get() *
                                        (1.0 - distance / aoeRadius); // Falloff with distance
                        entity.knockback(strength, -dx / distance, -dz / distance);
                    }
                });
        }

        // Apply strong knockback to primary target
        double strength = ElementalReactionConfig.overloadedKnockbackStrength.get();
        target.knockback(strength, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
    }

    private static void applyElectroCharged(LivingEntity target, ServerPlayer attacker, float baseDamage) {
        // Apply DoT damage
        int duration = ElementalReactionConfig.electroChargedDuration.get();
        int tickRate = ElementalReactionConfig.electroChargedTickRate.get();

        if (attacker != null) {
            DamageOverTimeManager.addDoT(target, attacker, ElementalReaction.ELECTRO_CHARGED,
                                        baseDamage, duration, tickRate);
        }

        // Apply Conductive effect for crit setup
        target.addEffect(new MobEffectInstance(ModEffects.CONDUCTIVE.get(), duration, 0));
    }

    private static void applyFrozen(LivingEntity target) {
        // Apply immobilization (high slowness) and Brittle effect
        int minDuration = ElementalReactionConfig.frozenMinDuration.get();
        int maxDuration = ElementalReactionConfig.frozenMaxDuration.get();
        int duration = (minDuration + maxDuration) / 2; // Average for now

        // High slowness for near-immobilization
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10));
        // Brittle effect for shatter bonus
        target.addEffect(new MobEffectInstance(ModEffects.BRITTLE.get(), duration, 0));
    }

    private static void applySuperconduct(LivingEntity target) {
        // Apply physical resistance reduction
        int duration = ElementalReactionConfig.superconductDuration.get();
        float resistanceReduction = ElementalReactionConfig.superconductResistanceReduction.get().floatValue();

        ResistanceModifier.applySuperconductReduction(target, resistanceReduction, duration);

        // Also apply weakness and slowness effects
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0));
    }

    private static void applyBurning(LivingEntity target, ServerPlayer attacker, float baseDamage) {
        // Apply DoT damage
        int duration = ElementalReactionConfig.burningDuration.get();
        int tickRate = ElementalReactionConfig.burningTickRate.get();

        if (attacker != null) {
            DamageOverTimeManager.addDoT(target, attacker, ElementalReaction.BURNING,
                                        baseDamage, duration, tickRate);
        }

        // Apply fire and Panic effect
        target.setSecondsOnFire(duration / 20);
        target.addEffect(new MobEffectInstance(ModEffects.PANIC.get(), duration, 0));
    }

    private static void applyBloom(LivingEntity target, ServerPlayer attacker) {
        // Spawn Bloom Core entity at target location
        if (target.level() instanceof ServerLevel serverLevel) {
            // Calculate damage for the bloom core
            float coreDamage = calculateReactionDamage(ElementalReaction.BLOOM, ElementType.NATURE,
                                                      10.0f, attacker);

            BloomCoreEntity bloomCore = new BloomCoreEntity(serverLevel, target.position(), attacker, coreDamage);
            serverLevel.addFreshEntity(bloomCore);
        }
    }

    private static void applyHyperbloom(LivingEntity target, ServerPlayer attacker, float damage) {
        // Hyperbloom is triggered by the Bloom Core entity itself, not here
        // This method is called for direct Hyperbloom reactions if needed
        // Apply Vulnerable effect
        int duration = ElementalReactionConfig.hyperbloomVulnerableDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), duration, 0));
    }

    private static void applyBurgeon(LivingEntity target, ServerPlayer attacker, float damage) {
        // Burgeon is triggered by the Bloom Core entity itself, not here
        // This method is called for direct Burgeon reactions if needed
        // The Bloom Core handles spawning the Smoldering Gloom zone
    }

    private static void applyUnstableWard(LivingEntity target, ServerPlayer attacker) {
        // Spawn a temporary shield effect (simplified implementation)
        // In a full implementation, this would spawn a collectible shard entity
        int duration = ElementalReactionConfig.unstableWardDuration.get();
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1));
    }

    private static void applyRiftPull(LivingEntity target, ServerPlayer attacker) {
        // Implement pull vector
        if (attacker != null) {
            Vec3 pullDirection = attacker.position().subtract(target.position()).normalize();
            double pullDistance = ElementalReactionConfig.riftPullDistance.get();
            // Use pull distance to calculate strength (closer = stronger pull)
            double distance = attacker.position().distanceTo(target.position());
            if (distance < pullDistance && distance > 0.5) {
                double pullStrength = (1.0 - (distance / pullDistance)) * 0.5; // Max 0.5 strength
                target.setDeltaMovement(target.getDeltaMovement().add(pullDirection.scale(pullStrength)));
                target.hurtMarked = true; // Force position update
            }
        }

        // Apply Spatial Instability effect
        int duration = ElementalReactionConfig.riftPullInstabilityDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.SPATIAL_INSTABILITY.get(), duration, 0));
    }

    private static void applySingularity(LivingEntity target, ServerPlayer attacker) {
        // Apply gravitational pull to nearby entities (simplified implementation)
        if (target.level() instanceof ServerLevel serverLevel) {
            float radius = ElementalReactionConfig.singularityRadius.get().floatValue();
            double pullStrength = ElementalReactionConfig.singularityPullStrength.get();

            // Find and pull nearby entities
            serverLevel.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                entity -> entity != target)
                .forEach(entity -> {
                    // Calculate pull vector towards the singularity center
                    Vec3 pullDirection = target.position().subtract(entity.position()).normalize();
                    double distance = entity.position().distanceTo(target.position());

                    // Stronger pull when closer
                    if (distance > 0.5) {
                        double strength = pullStrength * (1.0 - (distance / radius));
                        entity.setDeltaMovement(entity.getDeltaMovement().add(pullDirection.scale(strength)));
                        entity.hurtMarked = true;
                    }
                });

            // Apply slow to the target at the center
            int duration = ElementalReactionConfig.singularityDuration.get();
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
        }
    }

    private static void applyFracture(LivingEntity target) {
        // Apply Fracture custom effect (variable damage modifier)
        int duration = ElementalReactionConfig.fractureDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.FRACTURE.get(), duration, 0));
    }

    private static void applyWitheringSeed(LivingEntity target, ServerPlayer attacker) {
        // Apply Withering custom effect (damage reduction + life siphon)
        int duration = ElementalReactionConfig.witheringSeedDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.WITHERING.get(), duration, 0));
    }

    private static void applyDecrepitGrasp(LivingEntity target) {
        // Apply Decrepitude custom effect (attack speed reduction + heal prevention)
        int duration = ElementalReactionConfig.decrepitGraspDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.DECREPITUDE.get(), duration, 0));
    }

    /**
     * Generate Focus for Elemental Mage players when reactions occur
     */
    private static void generateFocusFromReaction(ServerPlayer player, ElementalReaction reaction, boolean isSuperReaction) {
        // Check if player has Elemental Mage Definition talent
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            ResourceLocation mageDefId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition");

            if (talents.hasTalent(mageDefId)) {
                // Calculate Focus generation
                float focusGenerated = ElementalMageDefinition.calculateFocusGeneration(player, isSuperReaction);

                // Add Focus to player's resource pool
                float actualAdded = talents.addResource(focusGenerated);

                // Reset Focus decay timer
                if (actualAdded > 0) {
                    ElementalMageDefinition.onFocusGained(player);

                    if (ElementalReactionConfig.enableDebugLogging.get()) {
                        TalentsMod.LOGGER.debug("Generated {} Focus for {} from {} reaction",
                            actualAdded, player.getName().getString(), reaction.name());
                    }
                }
            }
        });
    }
}
