package com.complextalents.impl.highpriest.integration;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.events.HolySpellHealEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Integration with Iron's Spellbooks for holy spell detection.
 * <p>
 * Listens for SpellHealEvent from Iron's Spellbooks and fires
 * HolySpellHealEvent when holy school type heals are detected.
 * </p>
 * <p>
 * This complements the existing HolySpellbooksIntegration which handles
 * SpellDamageEvent for holy spell damage.
 * </p>
 * <p>
 * The actual healing bonus and overheal mechanics are handled in
 * {@link com.complextalents.impl.highpriest.origin.HighPriestOrigin}.
 * </p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class HighPriestIntegration {
    private static boolean initialized = false;

    /**
     * Initializes the Iron's Spellbooks integration for holy heal detection.
     */
    public static void init() {
        if (initialized)
            return;
        initialized = true;

        try {
            MinecraftForge.EVENT_BUS.register(HighPriestIntegration.class);
            TalentsMod.LOGGER.info("High Priest: Iron's Spellbooks holy heal integration initialized");
        } catch (Exception e) {
            TalentsMod.LOGGER.warn("Failed to initialize High Priest Iron's Spellbooks integration: {}",
                    e.getMessage());
        }
    }

    /**
     * Handles SpellHealEvent from Iron's Spellbooks.
     * Detects holy heals and fires HolySpellHealEvent.
     * The HighPriestOrigin class handles the actual healing bonuses.
     */
    @SubscribeEvent
    public static void onSpellHeal(io.redspace.ironsspellbooks.api.events.SpellHealEvent event) {
        // Check if Iron's Spellbooks is loaded
        if (!com.complextalents.origin.integration.OriginModIntegrationHandler.isIronSpellbooksLoaded()) {
            return;
        }

        try {
            io.redspace.ironsspellbooks.api.spells.SchoolType schoolType = event.getSchoolType();
            if (schoolType == null)
                return;

            // Check if this is a holy spell
            String schoolPath = schoolType.getId().getPath();
            if (!"holy".equals(schoolPath)) {
                return;
            }

            LivingEntity caster = event.getEntity();
            LivingEntity target = event.getTargetEntity();

            if (caster == null || target == null)
                return;

            // Server-side only
            if (caster.level().isClientSide)
                return;

            float originalHealAmount = event.getHealAmount();

            // Fire holy spell heal event for healing bonuses
            // HighPriestOrigin will handle the actual bonus healing and overheal conversion
            HolySpellHealEvent holyEvent = new HolySpellHealEvent(target, caster, originalHealAmount, schoolType);
            MinecraftForge.EVENT_BUS.post(holyEvent);

        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing holy SpellHealEvent: {}", e.getMessage());
        }
    }

    /**
     * Handles ChangeManaEvent from Iron's Spellbooks.
     * Detects mana depletion and rewards Faith based on the removed mana
     * if the player is a High Priest at maximum Grace.
     */
    @SubscribeEvent
    public static void onManaChange(io.redspace.ironsspellbooks.api.events.ChangeManaEvent event) {
        if (!com.complextalents.origin.integration.OriginModIntegrationHandler.isIronSpellbooksLoaded()) {
            return;
        }

        try {
            if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            if (!com.complextalents.impl.highpriest.origin.HighPriestOrigin.isHighPriest(player)) {
                return;
            }

            // check if mana decreased
            float oldMana = event.getOldMana();
            float newMana = event.getNewMana();
            if (newMana >= oldMana)
                return;

            int graceStacks = com.complextalents.passive.PassiveManager.getPassiveStacks(player, "grace");
            if (graceStacks >= 10) {
                float manaCost = oldMana - newMana;

                // 100 mana = 1 Faith (previously 10000 mana = 1 Faith)
                double faithGained = manaCost / 100.0;
                if (faithGained > 0) {
                    com.complextalents.impl.highpriest.data.FaithData.addFaith(player, faithGained);
                    com.complextalents.impl.highpriest.origin.HighPriestOrigin.updateAttributes(player);
                }
            }
        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing ChangeManaEvent for Faith computation: {}", e.getMessage());
        }
    }
}
