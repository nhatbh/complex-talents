package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.api.events.ElementalReactionEvent;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.effects.ModEffects;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnParticlesPacket;
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
        }

        // Fire event for other mods
        ElementalReactionEvent event = new ElementalReactionEvent(target, attacker, reaction,
                                                                  reactionDamage, existingElement, triggeringElement);
        MinecraftForge.EVENT_BUS.post(event);

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
            case ELECTRO_CHARGED -> applyElectroCharged(target, damage);
            case FROZEN -> applyFrozen(target);
            case SUPERCONDUCT -> applySuperconduct(target);
            case BURNING -> applyBurning(target, damage);
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
        // TODO: Create steam cloud entity with ranged miss chance
        // For now, just apply a brief blindness effect
        int duration = ElementalReactionConfig.vaporizeSteamCloudDuration.get();
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
    }

    private static void applyMelt(LivingEntity target) {
        // Apply Frostbite custom effect (armor reduction)
        int duration = ElementalReactionConfig.meltFrostbiteDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.FROSTBITE.get(), duration, 0));
    }

    private static void applyOverloaded(LivingEntity target, ServerPlayer attacker) {
        // TODO: Implement AoE damage to nearby entities
        // Apply strong knockback
        double strength = ElementalReactionConfig.overloadedKnockbackStrength.get();
        target.knockback(strength, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
    }

    private static void applyElectroCharged(LivingEntity target, float baseDamage) {
        // TODO: Implement proper DoT damage ticking system
        // Apply Conductive effect for crit setup
        int duration = ElementalReactionConfig.electroChargedDuration.get();
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
        // TODO: Implement physical resistance reduction
        int duration = ElementalReactionConfig.superconductDuration.get();
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0));
    }

    private static void applyBurning(LivingEntity target, float baseDamage) {
        // TODO: Implement proper DoT damage ticking system
        // Apply fire and Panic effect
        int duration = ElementalReactionConfig.burningDuration.get();
        target.setSecondsOnFire(duration / 20);
        target.addEffect(new MobEffectInstance(ModEffects.PANIC.get(), duration, 0));
    }

    private static void applyBloom(LivingEntity target, ServerPlayer attacker) {
        // TODO: Spawn Bloom Core entity at target location
        TalentsMod.LOGGER.debug("Bloom reaction - core spawning not yet implemented");
    }

    private static void applyHyperbloom(LivingEntity target, ServerPlayer attacker, float damage) {
        // TODO: Spawn tracking projectiles
        // Apply Vulnerable effect
        int duration = ElementalReactionConfig.hyperbloomVulnerableDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), duration, 0));
        TalentsMod.LOGGER.debug("Hyperbloom reaction - projectile spawning not yet implemented");
    }

    private static void applyBurgeon(LivingEntity target, ServerPlayer attacker, float damage) {
        // TODO: Create large AoE and Smoldering Gloom zone
        TalentsMod.LOGGER.debug("Burgeon reaction - AoE zone not yet implemented");
    }

    private static void applyUnstableWard(LivingEntity target, ServerPlayer attacker) {
        // TODO: Spawn collectible shard entity
        TalentsMod.LOGGER.debug("Unstable Ward reaction - shard spawning not yet implemented");
    }

    private static void applyRiftPull(LivingEntity target, ServerPlayer attacker) {
        // TODO: Implement pull vector
        // Apply Spatial Instability effect
        int duration = ElementalReactionConfig.riftPullInstabilityDuration.get();
        target.addEffect(new MobEffectInstance(ModEffects.SPATIAL_INSTABILITY.get(), duration, 0));
    }

    private static void applySingularity(LivingEntity target, ServerPlayer attacker) {
        // TODO: Spawn gravity well entity
        TalentsMod.LOGGER.debug("Singularity reaction - gravity well not yet implemented");
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
}
