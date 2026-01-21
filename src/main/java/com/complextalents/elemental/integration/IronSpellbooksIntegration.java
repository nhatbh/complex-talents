package com.complextalents.elemental.integration;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.ElementType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
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

        try {
            // Get spell damage source from the event
            io.redspace.ironsspellbooks.damage.SpellDamageSource spellDamageSource = event.getSpellDamageSource();
            
            // Get spell information using SpellDamageSource's spell() method
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = spellDamageSource.spell();
            if (spell == null) {
                return;
            }

            // Get school type from spell
            io.redspace.ironsspellbooks.api.spells.SchoolType schoolType = spell.getSchoolType();
            if (schoolType == null) {
                return;
            }

            // Log the detected spell school
            TalentsMod.LOGGER.info("Detected spell: '{}' with school: '{}'",
                spell.getSpellId(), schoolType.getId().toString());

            // Map school type to element type
            ElementType element = mapSchoolTypeToElement(schoolType);
            if (element == null) {
                TalentsMod.LOGGER.info("School '{}' not mapped to any element", schoolType.getId().toString());
                return;
            }

            TalentsMod.LOGGER.info("Mapped school '{}' to element: {}",
                schoolType.getId().toString(), element.name());

            // Get target entity (the entity being damaged)
            LivingEntity target = event.getEntity();
            if (target == null) return;

            // Get caster from the damage source
            // SpellDamageSource extends DamageSource, which provides:
            // - getEntity() returns the causingEntity (spell caster)
            // - getDirectEntity() returns the directEntity (could be projectile or caster)
            LivingEntity caster = null;

            // First try to get the actual spell caster (causingEntity)
            if (spellDamageSource.getEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getEntity();
            }

            // Fallback to direct entity if needed (for projectile-based spells)
            if (caster == null && spellDamageSource.getDirectEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getDirectEntity();
            }

            if (caster == null) return;

            // Send chat message to caster about the detection
            caster.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§a[Complex Talents] §eSpell: §f%s §e| School: §f%s §e| Element: §6%s",
                    spell.getSpellId(),
                    schoolType.getId().toString(),
                    element.name()
                ))
            );

            // Apply elemental stack to target with spell damage
            float spellDamage = event.getAmount();
            ElementalStackManager.applyElementStack(target, element, caster, spellDamage);

        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing Iron's Spellbooks SpellDamageEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Maps Iron's Spellbooks school types to elemental types
     * Updated for 6-element system: Fire, Aqua, Lightning, Ice, Nature, Ender
     * Uses ResourceLocation.getPath() to compare school IDs since SchoolType is now a class, not an enum
     */
    private static ElementType mapSchoolTypeToElement(io.redspace.ironsspellbooks.api.spells.SchoolType schoolType) {
        if (schoolType == null) return null;

        // Convert school type to element type using the resource location path
        String schoolId = schoolType.getId().getPath();
        return switch (schoolId) {
            case "fire" -> ElementType.FIRE;
            case "ice" -> ElementType.ICE;
            case "lightning" -> ElementType.LIGHTNING;
            case "evocation" -> ElementType.NATURE;  // Evocation mapped to Nature
            case "ender" -> ElementType.ENDER;
            case "blood" -> ElementType.FIRE;  // Blood magic is hot-themed
            case "holy" -> ElementType.ICE;    // Holy is purifying/cooling
            case "eldritch" -> ElementType.ENDER;  // Eldritch is void/ender themed
            case "nature" -> ElementType.NATURE;
            case "none" -> null;
            default -> null;
        };
    }
}
