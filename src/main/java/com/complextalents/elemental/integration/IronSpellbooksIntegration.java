package com.complextalents.elemental.integration;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.ElementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class IronSpellbooksIntegration {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            // Register event listener for Iron's Spellbooks SpellDamageEvent
            // Using actual SpellDamageEvent class from Iron's Spellbooks
            MinecraftForge.EVENT_BUS.register(IronSpellbooksIntegration.class);
            TalentsMod.LOGGER.info("Iron's Spellbooks integration initialized (using SpellDamageEvent)");
        } catch (Exception e) {
            TalentsMod.LOGGER.warn("Failed to initialize Iron's Spellbooks integration: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(io.redspace.ironsspellbooks.api.events.SpellDamageEvent event) {
        if (!ModIntegrationHandler.isIronSpellbooksLoaded()) return;

        // Helper to broadcast messages to all players
        java.util.function.Consumer<String> broadcast = (String message) -> {
            if (event.getEntity() != null && event.getEntity().level() != null) {
                event.getEntity().level().players().forEach(player -> {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                });
            }
        };

        // Send immediate chat message to confirm event is firing
        broadcast.accept(String.format("§c[EVENT FIRED] §fSpellDamageEvent | Damage: §e%.2f §f| Target: §e%s",
            event.getAmount(),
            event.getEntity() != null ? event.getEntity().getName().getString() : "null"));

        try {
            // Get spell damage source from the event
            io.redspace.ironsspellbooks.damage.SpellDamageSource spellDamageSource = event.getSpellDamageSource();

            // Get spell information using SpellDamageSource's spell() method
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = spellDamageSource.spell();
            if (spell == null) {
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] SpellDamageEvent fired but spell was null");
                broadcast.accept("§c[DEBUG] §fSpell was null - exiting early");
                return;
            }

            // Get school type from spell
            io.redspace.ironsspellbooks.api.spells.SchoolType schoolType = spell.getSchoolType();
            if (schoolType == null) {
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Spell '{}' has no school type", spell.getSpellId());
                broadcast.accept(String.format("§c[DEBUG] §fSpell '%s' has no school type - exiting early", spell.getSpellId()));
                return;
            }

            // LOG SPELL DAMAGE AND SCHOOL FOR TO MOD DEBUGGING
            float spellDamage = event.getAmount();
            float originalDamage = event.getOriginalAmount();
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] ==================================================");
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Spell ID: '{}'", spell.getSpellId());
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] School: '{}'", schoolType.getId().toString());
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Original Damage: {}", originalDamage);
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Current Damage: {}", spellDamage);
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Target: {}", event.getEntity().getName().getString());
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] ==================================================");

            // Log the detected spell school
            TalentsMod.LOGGER.info("Detected spell: '{}' with school: '{}'",
                spell.getSpellId(), schoolType.getId().toString());

            // Map school type to element type
            ElementType element = mapSchoolTypeToElement(schoolType);
            if (element == null) {
                TalentsMod.LOGGER.info("School '{}' not mapped to any element", schoolType.getId().toString());
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Element mapping failed - no element assigned");
                broadcast.accept(String.format("§c[DEBUG] §fSchool '%s' not mapped to any element - exiting early",
                    schoolType.getId().toString()));
                return;
            }

            TalentsMod.LOGGER.info("Mapped school '{}' to element: {}",
                schoolType.getId().toString(), element.name());
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Mapped element: {}", element.name());
            broadcast.accept(String.format("§a[DEBUG] §fMapped school '%s' to element: §6%s",
                schoolType.getId().toString(), element.name()));

            // Get target entity (the entity being damaged)
            LivingEntity target = event.getEntity();
            if (target == null) {
                broadcast.accept("§c[DEBUG] §fTarget entity was null - exiting early");
                return;
            }

            // Get caster from the damage source
            // SpellDamageSource extends DamageSource, which provides:
            // - getEntity() returns the causingEntity (spell caster)
            // - getDirectEntity() returns the directEntity (could be projectile or caster)
            LivingEntity caster = null;

            // First try to get the actual spell caster (causingEntity)
            if (spellDamageSource.getEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getEntity();
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Caster from getEntity(): {}", caster.getName().getString());
                broadcast.accept(String.format("§a[DEBUG] §fCaster found via getEntity(): §e%s", caster.getName().getString()));
            }

            // Fallback to direct entity if needed (for projectile-based spells)
            if (caster == null && spellDamageSource.getDirectEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getDirectEntity();
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Caster from getDirectEntity(): {}", caster.getName().getString());
                broadcast.accept(String.format("§a[DEBUG] §fCaster found via getDirectEntity(): §e%s", caster.getName().getString()));
            }

            if (caster == null) {
                TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] No valid caster found for spell damage event");
                broadcast.accept("§c[DEBUG] §fNo valid caster found - exiting early");
                return;
            }

            // Send chat message to caster about the detection
            caster.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§a[Complex Talents] §eSpell: §f%s §e| School: §f%s §e| Element: §6%s",
                    spell.getSpellId(),
                    schoolType.getId().toString(),
                    element.name()
                ))
            );

            // Send detailed debug info to caster's chat for in-game debugging
            caster.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§7[DEBUG] §bOriginal Damage: §f%.2f §b| Current Damage: §f%.2f §b| Target: §f%s",
                    originalDamage,
                    spellDamage,
                    target.getName().getString()
                ))
            );

            // Apply elemental stack to target with spell damage
            TalentsMod.LOGGER.info("[SPELL DAMAGE DEBUG] Applying elemental stack - Element: {}, Damage: {}, Target: {}, Caster: {}",
                element.name(), spellDamage, target.getName().getString(), caster.getName().getString());
            ElementalStackManager.applyElementStack(target, element, caster, spellDamage);

        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing Iron's Spellbooks SpellDamageEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Fallback detection for Travel Optics aqua spells that don't fire damage events properly.
     * Detects when the "traveloptics:wet" effect is applied and tracks the source entity.
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!ModIntegrationHandler.isIronSpellbooksLoaded()) return;

        LivingEntity target = event.getEntity();

        // Only run on server side
        if (target.level().isClientSide) return;

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) return;

        // Broadcast that ANY effect was added (for debugging)
        String effectDescriptionId = effectInstance.getEffect().getDescriptionId();
        target.level().players().forEach(player -> {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§d[EFFECT ADDED] §fEffect: §e%s §fon §e%s",
                    effectDescriptionId,
                    target.getName().getString()
                ))
            );
        });

        // Check if this is the traveloptics:wet effect by description ID only
        boolean isWetEffect = effectDescriptionId.equals("effect.traveloptics.wet");

        // Debug logging for wet effect detection
        target.level().players().forEach(player -> {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§e[WET CHECK] §fIs wet effect: §6%s §f(comparing '%s' == 'effect.traveloptics.wet')",
                    isWetEffect,
                    effectDescriptionId
                ))
            );
        });

        if (!isWetEffect) {
            target.level().players().forEach(player -> {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c[WET CHECK] §fNot wet effect - exiting early")
                );
            });
            return;
        }

        // Broadcast that wet effect was confirmed
        target.level().players().forEach(player -> {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§a[WET CHECK] §fWet effect confirmed! Checking for caster...")
            );
        });

        // AQUA element from wet effect is passive - it doesn't trigger reactions
        // It can only be triggered BY other elements, not trigger them itself
        // This is because we can't reliably find the caster from the wet effect

        // Use target as the "source" since we don't have a real caster
        // This prevents the reaction check from running (requires ServerPlayer source)
        LivingEntity pseudoSource = target;

        target.level().players().forEach(player -> {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§a[WET CHECK] §fApplying passive AQUA stack to §e%s §f(no caster - won't trigger reactions)",
                    target.getName().getString()
                ))
            );
        });

        // Apply AQUA elemental stack with a small base damage value
        // Using target as source means it won't trigger reactions (not a ServerPlayer)
        // Other elements hitting this target will trigger reactions with the AQUA stack
        float tokenDamage = 1.0f;

        TalentsMod.LOGGER.info("[WET EFFECT DEBUG] Applying passive AQUA stack - Target: {} (no caster)",
            target.getName().getString());

        ElementalStackManager.applyElementStack(target, ElementType.AQUA, pseudoSource, tokenDamage);
    }
    /**
     * Maps Iron's Spellbooks school types to elemental types
     * Updated for 6-element system: Fire, Aqua, Lightning, Ice, Nature, Ender
     * Uses ResourceLocation.getPath() to compare school IDs since SchoolType is now a class, not an enum
     * Also checks full ResourceLocation string for mod-specific schools like "traveloptics:aqua"
     */
    private static ElementType mapSchoolTypeToElement(io.redspace.ironsspellbooks.api.spells.SchoolType schoolType) {
        if (schoolType == null) return null;

        // Get both the path and full string representation
        String schoolPath = schoolType.getId().getPath();
        String schoolFull = schoolType.getId().toString();

        // First check full ResourceLocation for mod-specific schools
        switch (schoolFull) {
            case "traveloptics:aqua" -> { return ElementType.AQUA; }  // Travel Optics mod aqua school
        }

        // Then check path-only for standard schools
        return switch (schoolPath) {
            case "fire" -> ElementType.FIRE;
            case "ice" -> ElementType.ICE;
            case "lightning" -> ElementType.LIGHTNING;
            case "evocation" -> ElementType.NATURE;  // Evocation mapped to Nature
            case "ender" -> ElementType.ENDER;
            case "blood" -> ElementType.FIRE;  // Blood magic is hot-themed
            case "holy" -> ElementType.ICE;    // Holy is purifying/cooling
            case "eldritch" -> ElementType.ENDER;  // Eldritch is void/ender themed
            case "nature" -> ElementType.NATURE;
            case "aqua" -> ElementType.AQUA;  // Generic aqua school
            case "none" -> null;
            default -> null;
        };
    }
}
