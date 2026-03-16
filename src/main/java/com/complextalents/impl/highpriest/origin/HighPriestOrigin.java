package com.complextalents.impl.highpriest.origin;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.client.HighPriestRenderer;
import com.complextalents.impl.highpriest.data.SeraphSwordData;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.impl.highpriest.events.HolySpellHealEvent;
import com.complextalents.impl.highpriest.integration.HighPriestIntegration;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;

import com.complextalents.origin.events.OriginChangeEvent;
import com.complextalents.passive.PassiveManager;
import com.complextalents.passive.PassiveStackDef;
import com.complextalents.passive.events.PassiveStackChangeEvent;
import com.complextalents.util.UUIDHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.ClientboundSyncMana;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * High Priest Origin - Holy Judgment, Sword & Shield, Divine Retribution.
 * <p>
 * High Risk / High Reward playstyle. You must maintain perfect positioning
 * to keep your resources (Piety) and buffs (Grace stacks) high.
 * A skilled Priest is an immortal raid commander; a sloppy one is a liability
 * with no resources.
 * </p>
 *
 * <h3>Resource: Piety (0-100/125/150/200 by level)</h3>
 * <ul>
 * <li><strong>Generation:</strong> Gain Piety when Iron's Spells successfully
 * hit or heal</li>
 * <li><strong>Punishment:</strong> Lose Piety instantly when taking damage</li>
 * <li><strong>Economy:</strong> Forces careful aim and dodging</li>
 * </ul>
 *
 * <h3>Passive: Grace of the Seraphim</h3>
 * <ul>
 * <li>Passively gain stacks over time (Max 10)</li>
 * <li>Lose ALL stacks when taking damage</li>
 * <li><strong>Low Stacks:</strong> Increases Healing Potency</li>
 * <li><strong>Max Stacks (10):</strong> Converts Healing Power into Spell
 * Damage (DPS mode)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class HighPriestOrigin {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "high_priest");
    private static final UUID CAST_TIME_REDUCTION_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest",
            "grace_cast_speed");
    private static final UUID HOLY_SPELL_POWER_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest",
            "faith_divine_retribution");
    private static final UUID MAX_MANA_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest", "faith_max_mana");

    /**
     * Check if a player is a High Priest.
     */
    public static boolean isHighPriest(ServerPlayer player) {
        return ID.equals(OriginManager.getOriginId(player));
    }

    /**
     * Initializes the Iron's Spellbooks integration for holy heal detection.
     * Call this during mod initialization before registering the origin.
     */
    public static void initIntegration() {
        HighPriestIntegration.init();
    }

    /**
     * Register the High Priest origin.
     * Call this during mod initialization.
     */
    public static void register() {
        OriginBuilder.create(ID)
                .displayName("High Priest")
                .description(Component.literal("Holy commander focusing on perfect positioning and divine retribution. Utilize Grace to gain massive cast speed and healing potency, but lose focus upon taking damage. Build Command stacks to control the Seraphic Echo."))
                .maxLevel(5)
                // Grace stack - binary state (ON/OFF), lost on damage
                .passiveStack("grace", PassiveStackDef.create("Grace")
                        .maxStacks(1)
                        .build())
                // Command stacks - gain over time, consumed by Seraphic Echo
                .passiveStack("command", PassiveStackDef.create("Command")
                        .maxStacks(10)
                        .build())
                // Grace recovery cooldown stack (synced timer)
                .passiveStack("grace_cooldown", PassiveStackDef.create("Grace Recovery")
                        .maxStacks(600)
                        .build())
                // Custom HUD renderer
                .renderer(new HighPriestRenderer())
                // Scaled Stats
                .scaledStat("commandTickInterval", new double[] { 200.0, 180.0, 160.0, 140.0, 100.0 })
                .scaledStat("graceRecoveryDuration", new double[] { 600.0, 600.0, 600.0, 600.0, 600.0 })
                .scaledStat("castTimeReduction", new double[] { 0.20, 0.30, 0.40, 0.50, 0.60 })
                .scaledStat("healingPotency", new double[] { 0.30, 0.50, 0.70, 0.90, 1.25 })
                .scaledStat("overhealToAbsorptionRate", new double[] { 0.30, 0.40, 0.50, 0.60, 0.75 })
                .scaledStat("absorptionDuration", new double[] { 600.0, 800.0, 1000.0, 1200.0, 1500.0 })
                .scaledStat("manaPerFaith", new double[] { 0.1, 0.15, 0.2, 0.25, 0.3 })
                .register();
    }

    /**
     * Event handler for holy spell heals from Iron's Spellbooks.
     * High Priests gain Piety, bonus healing potency, and overheal-to-absorption
     * conversion.
     */
    @SubscribeEvent
    public static void onHolySpellHeal(HolySpellHealEvent event) {
        if (!(event.getCaster() instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Check if Grace is on (binary state)
        if (PassiveManager.getPassiveStacks(player, "grace") > 0) {
            // Calculate healing potency bonus
            double bonusMultiplier = OriginManager.getOriginStat(player, "healingPotency");
            float originalHealAmount = event.getHealAmount();
            float bonusHeal = originalHealAmount * (float) bonusMultiplier;
            float totalHeal = originalHealAmount + bonusHeal;

            // Check if target would be overhealed
            LivingEntity target = event.getTarget();
            float targetMaxHealth = target.getMaxHealth();
            float targetCurrentHealth = target.getHealth();
            float effectiveHeal = Math.min(totalHeal, targetMaxHealth - targetCurrentHealth);
            float overheal = totalHeal - effectiveHeal;

            // Apply the bonus healing
            target.heal(bonusHeal);

            // convert overheal to absorption hearts
            if (overheal > 0) {
                applyOverhealToAbsorption(player, target, overheal);
            }
        }
    }

    /**
     * Apply overheal-to-absorption conversion for High Priest at max Grace.
     *
     * @param player   The High Priest casting the heal
     * @param target   The target entity receiving the overheal
     * @param overheal The amount of overheal to convert
     */
    private static void applyOverhealToAbsorption(ServerPlayer player, LivingEntity target, float overheal) {
        // Get overheal conversion percentage (scaled with level)
        double conversionRate = OriginManager.getOriginStat(player, "overhealToAbsorptionRate");
        // Get absorption duration in ticks (scaled with level)
        int absorptionDuration = (int) OriginManager.getOriginStat(player, "absorptionDuration");

        // Calculate absorption health to grant
        float absorptionHealthToGrant = (float) (overheal * conversionRate);

        if (absorptionHealthToGrant > 0) {
            // Add absorption hearts directly to the target
            float currentAbsorption = target.getAbsorptionAmount();
            target.setAbsorptionAmount(currentAbsorption + absorptionHealthToGrant);

            // Apply Seraphic Grace effect to track expiration
            MobEffectInstance effectInstance = new MobEffectInstance(
                    HighPriestEffects.SERAPHIC_GRACE.get(),
                    absorptionDuration,
                    0,
                    false,
                    true);
            target.addEffect(effectInstance);

            TalentsMod.LOGGER.debug("High Priest converted overheal to absorption: {} health (duration: {} ticks)",
                    absorptionHealthToGrant, absorptionDuration);
        }
    }

    /**
     * Event handler for server-side ticking.
     * Handles Grace stack generation.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        long gameTime = player.level().getGameTime();

        // 1. Command generation: gain 1 stack every interval ticks (scales with level)
        double interval = OriginManager.getOriginStat(player, "commandTickInterval");
        int commandInterval = (int) interval;

        if (gameTime % commandInterval == 0) {
            int currentCommand = PassiveManager.getPassiveStacks(player, "command");
            if (currentCommand < 10) {
                PassiveManager.modifyPassiveStacks(player, "command", 1);
            }
        }

        // 2. Grace recovery logic (stack-based for easy syncing)
        int currentGrace = PassiveManager.getPassiveStacks(player, "grace");
        if (currentGrace == 0) {
            int cooldownTicks = PassiveManager.getPassiveStacks(player, "grace_cooldown");
            if (cooldownTicks > 0) {
                PassiveManager.modifyPassiveStacks(player, "grace_cooldown", -1);
            } else {
                PassiveManager.setPassiveStacks(player, "grace", 1);
            }
        }
    }

    /**
     * Update the player's cast time reduction, max mana, and holy spell power based
     * on current Grace and Faith stacks.
     * Uses Iron's Spellbooks cast_time_reduction, holy_spell_power, and max_mana
     * attributes.
     */
    public static void updateAttributes(ServerPlayer player) {
        boolean hasGrace = PassiveManager.getPassiveStacks(player, "grace") > 0;

        // Update cast time reduction (applies only when Grace is active)
        double totalReduction = hasGrace ? OriginManager.getOriginStat(player, "castTimeReduction") : 0;

        ResourceLocation castTimeAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks",
                "cast_time_reduction");
        Attribute castTimeAttr = ForgeRegistries.ATTRIBUTES.getValue(castTimeAttrId);

        if (castTimeAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(castTimeAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(CAST_TIME_REDUCTION_UUID);

                if (totalReduction > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            CAST_TIME_REDUCTION_UUID,
                            "High Priest Grace Cast Speed",
                            totalReduction,
                            AttributeModifier.Operation.ADDITION);
                    attributeInstance.addTransientModifier(modifier);
                }
            }
        }

        // Background: Faith increases Holy Spell Power when Grace is at max (10
        // stacks).
        // Faith increases Max Mana permanently.

        double faith = com.complextalents.impl.highpriest.data.FaithData.getFaith(player);

        // Update Max Mana (always applies)
        ResourceLocation maxManaAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "max_mana");
        Attribute maxManaAttr = ForgeRegistries.ATTRIBUTES.getValue(maxManaAttrId);

        if (maxManaAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(maxManaAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(MAX_MANA_UUID);

                double manaPerFaith = OriginManager.getOriginStat(player, "manaPerFaith");
                double totalManaBonus = faith * manaPerFaith;

                if (totalManaBonus > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            MAX_MANA_UUID,
                            "High Priest Faith Max Mana",
                            totalManaBonus,
                            AttributeModifier.Operation.ADDITION);
                    attributeInstance.addTransientModifier(modifier);
                }

                // Sync mana to client after max mana changes
                try {
                    MagicData magicData = MagicData.getPlayerMagicData(player);
                    Messages.sendToPlayer(new ClientboundSyncMana(magicData), player);
                } catch (Exception e) {
                    // Iron's Spellbooks not loaded or error
                }
            }
        }

        // Update holy spell power (only applies at max Grace - 10 stacks)
        ResourceLocation holyPowerAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks",
                "holy_spell_power");
        Attribute holyPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(holyPowerAttrId);

        if (holyPowerAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(holyPowerAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(HOLY_SPELL_POWER_UUID);

                if (hasGrace && faith > 0) {
                    // Faith directly increases Holy Spell Power when Grace is ON.
                    double spellPowerBonus = faith * 0.0001;

                    AttributeModifier modifier = new AttributeModifier(
                            HOLY_SPELL_POWER_UUID,
                            "High Priest Faith Divine Retribution",
                            spellPowerBonus,
                            AttributeModifier.Operation.MULTIPLY_BASE);
                    attributeInstance.addTransientModifier(modifier);
                }
            }
        }
    }

    /**
     * Event handler for when player takes damage.
     * Lose ALL Grace stacks when hurt - punishment mechanic.
     * PassiveStackChangeEvent will handle attribute updates.
     * <p>
     * EXCEPTION: Players protected by Covenant of Protection do NOT lose resources.
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!ID.equals(OriginManager.getOriginId(player))) {
                return;
            }

            // Lose Grace state when taking damage
            PassiveManager.setPassiveStacks(player, "grace", 0);
            // Start recovery cooldown timer (synced stack)
            int cooldownDuration = (int) OriginManager.getOriginStat(player, "graceRecoveryDuration");
            PassiveManager.setPassiveStacks(player, "grace_cooldown", cooldownDuration);
        }
    }

    /**
     * Event handler for player join.
     * Initialize attributes when player joins with High Priest origin.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Initialize attributes on join
        updateAttributes(player);
    }

    /**
     * Event handler for player logout.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Cleanup Seraph's Beacon when player logs out
            SeraphSwordData.cleanup(player.getUUID());
        }
    }

    /**
     * Event handler for player respawn.
     * Re-apply attributes after respawning.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Re-apply attributes after respawn
        updateAttributes(player);
    }

    /**
     * Event handler for origin level changes.
     * Updates attributes when the origin level changes.
     */
    @SubscribeEvent
    public static void onOriginChange(OriginChangeEvent event) {
        if (!ID.equals(event.getOriginId())) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        if (event.getChangeType() == OriginChangeEvent.ChangeType.LEVEL_CHANGE) {
            // Update attributes when level changes
            updateAttributes(player);
        }
    }

    /**
     * Event handler for passive stack changes.
     * Updates attributes when Grace stacks change.
     */
    @SubscribeEvent
    public static void onPassiveStackChange(PassiveStackChangeEvent event) {
        if (!"grace".equals(event.getStackTypeName()) && !"command".equals(event.getStackTypeName())) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Update attributes when Grace or Command stacks change (Command might affect
        // renderer/etc)
        updateAttributes(player);
    }
}
