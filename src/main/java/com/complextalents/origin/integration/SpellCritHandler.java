package com.complextalents.origin.integration;

import com.complextalents.TalentsMod;
import com.complextalents.util.IronParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles spell critical hit logic for all players.
 * <p>
 * Hooks into Iron's Spellbooks' SpellDamageEvent at HIGH priority.
 * All players have a base spell crit chance of 0% and crit damage of 150%.
 * These can be modified via the attribute system.
 * </p>
 */
public class SpellCritHandler {

    private static boolean initialized = false;

    /**
     * Initialize the spell crit handler. Must only be called when Iron's Spellbooks
     * is loaded.
     */
    public static void init() {
        if (initialized)
            return;
        initialized = true;

        try {
            MinecraftForge.EVENT_BUS.register(SpellCritHandler.class);
            TalentsMod.LOGGER.info("Origin: Spell crit handler initialized");
        } catch (Exception e) {
            TalentsMod.LOGGER.warn("Failed to initialize spell crit handler: {}", e.getMessage());
        }
    }

    /**
     * Intercept SpellDamageEvent to apply spell crit multiplier.
     * Runs at HIGH priority so the critted amount feeds into subsequent handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellDamage(io.redspace.ironsspellbooks.api.events.SpellDamageEvent event) {
        if (!OriginModIntegrationHandler.isIronSpellbooksLoaded())
            return;

        try {
            if (event.getEntity().level().isClientSide())
                return;

            // Caster must be a player
            io.redspace.ironsspellbooks.damage.SpellDamageSource source = event.getSpellDamageSource();
            if (!(source.getEntity() instanceof ServerPlayer caster))
                return;

            // Read spell crit chance
            AttributeInstance critChanceInst = caster.getAttribute(SpellCritAttributes.SPELL_CRIT_CHANCE.get());
            if (critChanceInst == null)
                return;

            double critChance = critChanceInst.getValue();

            // Inject Harmonic Convergence Buffs for the tracked spell
            double convergenceDamBonus = 0.0;
            if (com.complextalents.impl.elementalmage.origin.ElementalMageOrigin.isElementalMage(caster)) {
                com.complextalents.impl.elementalmage.ElementalMageData.ConvergenceBuff buff = 
                        com.complextalents.impl.elementalmage.ElementalMageData.getConvergenceBuff(caster.getUUID());
                if (buff.buffWindowTicks > 0 && source.spell() != null && source.spell().getSpellId().equals(buff.buffedSpellId)) {
                    critChance += buff.cachedCritChanceOffset;
                    convergenceDamBonus = buff.cachedCritDamageBonus;
                }
            }

            if (critChance <= 0.0)
                return;

            // Roll for crit
            double roll = caster.getRandom().nextDouble();
            if (roll >= critChance)
                return;

            // Crit! Apply damage multiplier
            AttributeInstance critDamageInst = caster.getAttribute(SpellCritAttributes.SPELL_CRIT_DAMAGE.get());
            double critDamage = (critDamageInst != null) ? critDamageInst.getValue() : 1.5;
            critDamage += convergenceDamBonus;

            float originalDamage = event.getAmount();
            float newDamage = (float) (originalDamage * critDamage);
            event.setAmount(newDamage);

            // Drain 4% of souls when Blood Pact is active and a spell crits
            if (com.complextalents.impl.darkmage.origin.DarkMageOrigin.isDarkMage(caster)
                    && com.complextalents.impl.darkmage.events.BloodPactTickHandler.isBloodPactActive(caster)) {
                double soulsLost = com.complextalents.impl.darkmage.data.SoulData.loseSouls(caster, 0.04);
                // Soul escape particles on the caster (more souls lost = more particles)
                ServerLevel serverLevel = caster.serverLevel();
                int count = Math.max(1, Math.min(30, (int) (soulsLost * 2)));
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        caster.getX(), caster.getY() + caster.getBbHeight() / 2.0, caster.getZ(),
                        count, 0.3, 0.4, 0.3, 0.08);
            }

            // Spawn school-themed particles on the target
            LivingEntity target = event.getEntity();
            spawnCritParticles(source, target);

            // Notify the caster
            caster.sendSystemMessage(Component.literal(
                    String.format("\u00A76\u2736 Spell Critical! \u00A7f%.1f damage", newDamage)));

            TalentsMod.LOGGER.debug(
                    "Spell crit! Player={} roll={} vs chance={}, multiplier={}x ({} -> {})",
                    caster.getName().getString(),
                    String.format("%.4f", roll),
                    String.format("%.4f", critChance),
                    String.format("%.2f", critDamage),
                    originalDamage, newDamage);

        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing SpellDamageEvent for spell crit: {}", e.getMessage());
        }
    }

    private static void spawnCritParticles(io.redspace.ironsspellbooks.damage.SpellDamageSource source,
            LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel))
            return;

        String particleName = "magic"; // default fallback
        try {
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = source.spell();
            if (spell != null) {
                io.redspace.ironsspellbooks.api.spells.SchoolType school = spell.getSchoolType();
                if (school != null) {
                    String schoolFull = school.getId().toString();
                    String schoolPath = school.getId().getPath();
                    if (schoolFull.equals("traveloptics:aqua")) {
                        particleName = "acid_bubble";
                    } else {
                        particleName = switch (schoolPath) {
                            case "fire" -> "fire";
                            case "ice" -> "snowflake";
                            case "lightning" -> "lightning";
                            case "ender" -> "unstable_ender";
                            case "nature" -> "nature";
                            case "blood" -> "acid_bubble"; // blood maps to AQUA
                            default -> "magic";
                        };
                    }
                }
            }
        } catch (Exception ignored) {
        }

        ParticleOptions particle = IronParticleHelper.getIronParticle(particleName);
        if (particle == null)
            return;

        serverLevel.sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                20, 0.4, 0.4, 0.4, 0.1);
    }
}
