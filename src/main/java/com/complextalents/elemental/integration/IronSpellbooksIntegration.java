package com.complextalents.elemental.integration;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.events.ElementalDamageEvent;
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
            MinecraftForge.EVENT_BUS.register(IronSpellbooksIntegration.class);
            TalentsMod.LOGGER.info("Iron's Spellbooks integration initialized");
        } catch (Exception e) {
            TalentsMod.LOGGER.warn("Failed to initialize Iron's Spellbooks integration: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(io.redspace.ironsspellbooks.api.events.SpellDamageEvent event) {
        if (!ModIntegrationHandler.isIronSpellbooksLoaded()) return;

        try {
            io.redspace.ironsspellbooks.damage.SpellDamageSource spellDamageSource = event.getSpellDamageSource();
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = spellDamageSource.spell();
            if (spell == null) return;

            io.redspace.ironsspellbooks.api.spells.SchoolType schoolType = spell.getSchoolType();
            if (schoolType == null) return;

            ElementType element = mapSchoolTypeToElement(schoolType);
            if (element == null) return;

            LivingEntity target = event.getEntity();
            if (target == null) return;

            // Server-side only
            if (target.level().isClientSide) return;

            LivingEntity caster = null;
            if (spellDamageSource.getEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getEntity();
            } else if (spellDamageSource.getDirectEntity() instanceof LivingEntity) {
                caster = (LivingEntity) spellDamageSource.getDirectEntity();
            }

            if (caster == null) return;

            // Fire elemental damage event instead of directly applying stacks
            float damage = event.getAmount();
            ElementalDamageEvent elementalEvent = new ElementalDamageEvent(target, caster, element, damage);
            MinecraftForge.EVENT_BUS.post(elementalEvent);

        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing SpellDamageEvent: {}", e.getMessage());
        }
    }

    /**
     * Fallback detection for Travel Optics wet effect.
     * Applies passive AQUA element (won't trigger reactions without a valid caster).
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!ModIntegrationHandler.isIronSpellbooksLoaded()) return;

        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) return;

        String effectDescriptionId = effectInstance.getEffect().getDescriptionId();
        if (!effectDescriptionId.equals("effect.traveloptics.wet")) return;

        // Fire elemental damage event with self as source
        ElementalDamageEvent elementalEvent = new ElementalDamageEvent(target, target, ElementType.AQUA, 0);
        MinecraftForge.EVENT_BUS.post(elementalEvent);
    }
    private static ElementType mapSchoolTypeToElement(io.redspace.ironsspellbooks.api.spells.SchoolType schoolType) {
        if (schoolType == null) return null;

        String schoolPath = schoolType.getId().getPath();
        String schoolFull = schoolType.getId().toString();

        if (schoolFull.equals("traveloptics:aqua")) return ElementType.AQUA;

        return switch (schoolPath) {
            case "fire" -> ElementType.FIRE;
            case "ice" -> ElementType.ICE;
            case "lightning" -> ElementType.LIGHTNING;
            case "ender" -> ElementType.ENDER;
            case "nature" -> ElementType.NATURE;
            case "blood" -> ElementType.AQUA;
            default -> null;
        };
    }
}
