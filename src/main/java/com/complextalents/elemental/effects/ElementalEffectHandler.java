package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.attributes.MasteryAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles damage and healing events for custom elemental status effects
 * This is where effects like Conductive, Brittle, Fracture, etc. actually modify combat
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalEffectHandler {

    /**
     * Main damage event handler - modifies damage based on active effects
     * Priority: HIGH - We want to run early to modify damage before other mods
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // Check for friendly fire protection on elemental effects
        if (ElementalReactionConfig.enableFriendlyFireProtection.get() &&
            source.getEntity() instanceof Player attacker &&
            attacker.isAlliedTo(target)) {

            // Cancel all elemental effect damage modifications for teammates
            if (target.hasEffect(ModEffects.CONDUCTIVE.get()) ||
                target.hasEffect(ModEffects.BRITTLE.get()) ||
                target.hasEffect(ModEffects.FRACTURE.get()) ||
                target.hasEffect(ModEffects.VULNERABLE.get())) {

                if (ElementalReactionConfig.enableDebugLogging.get()) {
                    TalentsMod.LOGGER.debug("Elemental effect damage blocked by friendly fire protection: {} -> {}",
                                          attacker.getName().getString(), target.getName().getString());
                }
                return; // Don't modify damage for teammates
            }
        }

        // Track if we modified damage
        boolean damageModified = false;
        float modifiedDamage = damage;

        // === Effects on the TARGET (receiving damage) ===

        // Conductive: Guaranteed critical hit with mastery scaling
        if (target.hasEffect(ModEffects.CONDUCTIVE.get())) {
            modifiedDamage = applyConductiveCrit(target, source, modifiedDamage);
            damageModified = true;
        }

        // Brittle: Shatter bonus damage
        if (target.hasEffect(ModEffects.BRITTLE.get())) {
            modifiedDamage = applyBrittleShatter(target, modifiedDamage);
            damageModified = true;
        }

        // Fracture: Variable damage modifier (25% ignore, 25% amplify)
        if (target.hasEffect(ModEffects.FRACTURE.get())) {
            Float fractureResult = applyFracture(target, modifiedDamage);
            if (fractureResult != null) {
                modifiedDamage = fractureResult;
                damageModified = true;
            }
        }

        // Vulnerable: Damage amplification
        if (target.hasEffect(ModEffects.VULNERABLE.get())) {
            modifiedDamage = applyVulnerable(modifiedDamage);
            damageModified = true;
        }

        // === Super-Reaction Effects (Ice) ===

        // Shattering Prism: AoE explosion on next hit
        if (target.hasEffect(ModEffects.SHATTERING_PRISM.get())) {
            applyShatteringPrismExplosion(target, source, modifiedDamage);
            // Note: Effect is removed in the explosion handler
        }

        // Cryo-Shatter: Convert damage to Poise damage
        if (target.hasEffect(ModEffects.CRYO_SHATTER.get())) {
            // TODO: Implement Poise system integration
            // For now, just amplify damage by the poise multiplier
            modifiedDamage *= (float) CryoShatterEffect.POISE_DAMAGE_MULTIPLIER;
            damageModified = true;
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Cryo-Shatter amplified damage on {} (Poise system pending): {}x",
                    target.getName().getString(), CryoShatterEffect.POISE_DAMAGE_MULTIPLIER);
            }
        }

        // === Super-Reaction Effects (Ender) ===

        // Void Touched: Armor reduction (damage taken amplification)
        if (target.hasEffect(ModEffects.VOID_TOUCHED.get())) {
            modifiedDamage *= (1.0f + (float) VoidTouchedEffect.ARMOR_REDUCTION);
            damageModified = true;
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Void Touched amplified damage on {}: +{}%",
                    target.getName().getString(), VoidTouchedEffect.ARMOR_REDUCTION * 100);
            }
        }

        // Unraveling: Increased damage taken + true damage per hit
        if (target.hasEffect(ModEffects.UNRAVELING.get())) {
            int amplifier = target.getEffect(ModEffects.UNRAVELING.get()).getAmplifier();
            double damageMultiplier = amplifier == 1
                ? UnravelingEffect.TIER_4_DAMAGE_MULTIPLIER
                : UnravelingEffect.TIER_3_DAMAGE_MULTIPLIER;

            modifiedDamage *= (float) damageMultiplier;
            damageModified = true;

            // Tier 4: Apply 1% max HP true damage
            if (amplifier == 1) {
                float trueDamage = target.getMaxHealth() * (float) UnravelingEffect.TIER_4_TRUE_DAMAGE_PERCENT;
                target.hurt(target.level().damageSources().magic(), trueDamage);

                if (ElementalReactionConfig.enableDebugLogging.get()) {
                    TalentsMod.LOGGER.debug("Unraveling Tier 4 on {}: {}x damage + {} true damage",
                        target.getName().getString(), damageMultiplier, trueDamage);
                }
            } else if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Unraveling Tier 3 on {}: {}x damage",
                    target.getName().getString(), damageMultiplier);
            }
        }

        // === Effects on the ATTACKER (dealing damage) ===

        if (source.getEntity() instanceof LivingEntity attacker) {
            // Withering: Reduced outgoing damage
            if (attacker.hasEffect(ModEffects.WITHERING.get())) {
                modifiedDamage = applyWitheringDamageReduction(modifiedDamage);
                damageModified = true;

                // Life siphon: Heal attacker when hitting withered target
                // Skip if friendly fire protection is enabled and they're teammates
                boolean skipLifeSiphon = ElementalReactionConfig.enableFriendlyFireProtection.get() &&
                                       attacker instanceof Player &&
                                       ((Player) attacker).isAlliedTo(target);

                if (target.hasEffect(ModEffects.WITHERING.get()) && !skipLifeSiphon) {
                    applyLifeSiphon(attacker, modifiedDamage);
                }
            }
        }

        // Apply modified damage
        if (damageModified) {
            event.setAmount(modifiedDamage);
        }
    }

    /**
     * Conductive effect: Apply guaranteed critical hit with mastery scaling
     * Scales with attacker's Elemental Mastery and Lightning Mastery
     * Provides extra amplification if the attack was already a crit
     */
    private static float applyConductiveCrit(LivingEntity target, DamageSource source, float damage) {
        // Default multiplier for non-player attacks or players without mastery
        float damageMultiplier = 1.5f;

        // If attacker is a player, apply mastery scaling
        if (source.getEntity() instanceof Player attacker) {
            // Get attacker's mastery values
            double elementalMastery = attacker.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
            double lightningMastery = attacker.getAttributeValue(MasteryAttributes.LIGHTNING_MASTERY.get());

            // Calculate mastery bonus using diminishing returns
            double generalScaling = ElementalReactionConfig.generalMasteryScaling.get();
            double specificScaling = ElementalReactionConfig.specificMasteryScaling.get();

            double generalBonus = MasteryAttributes.calculateGeneralMasteryBonus(elementalMastery, generalScaling);
            double specificBonus = MasteryAttributes.calculateSpecificMasteryBonus(lightningMastery, specificScaling);

            // Combined multiplier: (1 + generalBonus) * (1 + specificBonus)
            double masteryMultiplier = (1.0 + generalBonus) * (1.0 + specificBonus);

            // Check if this was likely a critical hit already
            // Heuristic: if damage source is from a player doing a melee attack
            boolean wasLikelyCrit = source.getDirectEntity() == attacker &&
                                   !attacker.onGround() &&
                                   attacker.fallDistance > 0;

            // Base amplification:
            // Normal hit: 1.5x (crit-like) + extra amplification
            // Already crit: 1.5x (existing crit) + even more amplification
            float baseAmplification = wasLikelyCrit ? 2.0f : 1.5f;

            // Apply mastery scaling to get final multiplier
            damageMultiplier = baseAmplification * (float) masteryMultiplier;

            if (ElementalReactionConfig.enableDebugLogging.get()) {
                String critType = wasLikelyCrit ? "SUPER-CRIT" : "FORCED-CRIT";
                TalentsMod.LOGGER.debug("Conductive {} on {}: mastery {:.1f}/{:.1f}, multiplier: {:.2f}x, {} -> {} damage",
                                      critType, target.getName().getString(),
                                      elementalMastery, lightningMastery,
                                      damageMultiplier, damage, damage * damageMultiplier);
            }
        } else {
            // Non-player attack: just use base crit multiplier
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Conductive crit (non-player) on {}: {} -> {} damage ({}x)",
                                      target.getName().getString(), damage, damage * damageMultiplier, damageMultiplier);
            }
        }

        float finalDamage = damage * damageMultiplier;

        // Remove effect after triggering (one-time use)
        target.removeEffect(ModEffects.CONDUCTIVE.get());

        return finalDamage;
    }

    /**
     * Brittle effect: Apply shatter bonus when frozen entity is hit
     * Uses configured shatter multiplier from config
     */
    private static float applyBrittleShatter(LivingEntity target, float damage) {
        double shatterMultiplier = ElementalReactionConfig.frozenShatterMultiplier.get();
        float shatteredDamage = damage * (float) shatterMultiplier;

        // Remove brittle effect after shattering
        target.removeEffect(ModEffects.BRITTLE.get());

        if (ElementalReactionConfig.enableDebugLogging.get()) {
            TalentsMod.LOGGER.debug("Brittle shatter triggered on {}: {} -> {} damage ({}x)",
                                  target.getName().getString(), damage, shatteredDamage, shatterMultiplier);
        }

        return shatteredDamage;
    }

    /**
     * Fracture effect: Variable damage modifier
     * 25% chance to ignore damage (return 0)
     * 25% chance to amplify damage by 1.25x
     * 50% chance normal damage (return null to indicate no change)
     */
    private static Float applyFracture(LivingEntity target, float damage) {
        double roll = target.level().random.nextDouble();
        double ignoreChance = ElementalReactionConfig.fractureIgnoreChance.get();
        double amplifyChance = ElementalReactionConfig.fractureAmplifyChance.get();
        double amplifyMultiplier = ElementalReactionConfig.fractureAmplifyMultiplier.get();

        if (roll < ignoreChance) {
            // Damage ignored
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Fracture ignored damage on {}: {} -> 0",
                                      target.getName().getString(), damage);
            }
            return 0.0f;
        } else if (roll < ignoreChance + amplifyChance) {
            // Damage amplified
            float amplifiedDamage = damage * (float) amplifyMultiplier;
            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Fracture amplified damage on {}: {} -> {} ({}x)",
                                      target.getName().getString(), damage, amplifiedDamage, amplifyMultiplier);
            }
            return amplifiedDamage;
        }

        // Normal damage (50% chance)
        return null;
    }

    /**
     * Vulnerable effect: Amplify incoming damage
     */
    private static float applyVulnerable(float damage) {
        double amplifier = VulnerableEffect.getDamageAmplifier();
        return damage * (float) amplifier;
    }

    /**
     * Withering effect: Reduce outgoing damage
     */
    private static float applyWitheringDamageReduction(float damage) {
        double reduction = WitheringEffect.getDamageReduction();
        return damage * (float) (1.0 - reduction);
    }

    /**
     * Withering life siphon: Heal attacker based on damage dealt
     */
    private static void applyLifeSiphon(LivingEntity attacker, float damage) {
        double lifeSteal = WitheringEffect.getLifeSteal();
        float healAmount = damage * (float) lifeSteal;

        if (healAmount > 0) {
            attacker.heal(healAmount);

            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Life siphon healed {} for {} health",
                                      attacker.getName().getString(), healAmount);
            }
        }
    }

    /**
     * Shattering Prism effect: AoE explosion on hit
     * Deals 150% of the triggering damage to all enemies in 4-block radius
     */
    private static void applyShatteringPrismExplosion(LivingEntity target, DamageSource source, float triggerDamage) {
        float explosionDamage = triggerDamage * (float) ShatteringPrismEffect.SHATTER_DAMAGE_MULTIPLIER;
        float radius = ShatteringPrismEffect.SHATTER_RADIUS;

        // Find all nearby entities
        target.level().getEntitiesOfClass(LivingEntity.class,
            target.getBoundingBox().inflate(radius),
            entity -> entity != target && entity.isAlive() && !entity.isAlliedTo(target))
            .forEach(nearbyEntity -> {
                // Deal AoE damage
                nearbyEntity.hurt(target.level().damageSources().magic(), explosionDamage);

                if (ElementalReactionConfig.enableDebugLogging.get()) {
                    TalentsMod.LOGGER.debug("Shattering Prism AoE hit {} for {} damage",
                        nearbyEntity.getName().getString(), explosionDamage);
                }
            });

        // Remove effect after triggering
        target.removeEffect(ModEffects.SHATTERING_PRISM.get());

        if (ElementalReactionConfig.enableDebugLogging.get()) {
            TalentsMod.LOGGER.debug("Shattering Prism explosion on {}: {} trigger damage -> {} AoE damage in {} block radius",
                target.getName().getString(), triggerDamage, explosionDamage, radius);
        }
    }

    /**
     * Healing event handler for Decrepitude effect and Unraveling
     * Prevents healing and converts first heal to damage
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();

        // Decrepitude: Prevent healing and convert to damage
        if (entity.hasEffect(ModEffects.DECREPITUDE.get())) {
            float healAmount = event.getAmount();

            // Cancel the healing
            event.setCanceled(true);

            // Convert heal to damage (first heal only)
            entity.hurt(entity.level().damageSources().magic(), healAmount);

            // Remove effect after triggering once
            entity.removeEffect(ModEffects.DECREPITUDE.get());

            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Decrepitude converted {} healing to damage on {}",
                                      healAmount, entity.getName().getString());
            }
        }

        // Unraveling: Disable healing
        if (entity.hasEffect(ModEffects.UNRAVELING.get())) {
            event.setCanceled(true);

            if (ElementalReactionConfig.enableDebugLogging.get()) {
                TalentsMod.LOGGER.debug("Unraveling blocked {} healing on {}",
                    event.getAmount(), entity.getName().getString());
            }
        }
    }
}
