package com.complextalents.impl.highpriest.origin;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.client.HighPriestRenderer;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.impl.highpriest.events.HolySpellHealEvent;
import com.complextalents.impl.highpriest.integration.HighPriestIntegration;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.ResourceType;
import com.complextalents.origin.events.HolySpellDamageEvent;
import com.complextalents.origin.events.OriginChangeEvent;
import com.complextalents.passive.PassiveManager;
import com.complextalents.passive.PassiveStackDef;
import com.complextalents.passive.events.PassiveStackChangeEvent;
import com.complextalents.util.UUIDHelper;
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
 * A skilled Priest is an immortal raid commander; a sloppy one is a liability with no resources.
 * </p>
 *
 * <h3>Resource: Piety (0-100)</h3>
 * <ul>
 *   <li><strong>Generation:</strong> Gain Piety when Iron's Spells successfully hit or heal</li>
 *   <li><strong>Punishment:</strong> Lose 20 Piety instantly when taking damage</li>
 *   <li><strong>Economy:</strong> Forces careful aim and dodging</li>
 * </ul>
 *
 * <h3>Passive: Grace of the Seraphim</h3>
 * <ul>
 *   <li>Passively gain stacks over time (Max 10)</li>
 *   <li>Lose ALL stacks when taking damage</li>
 *   <li><strong>Low Stacks:</strong> Increases Healing Potency</li>
 *   <li><strong>Max Stacks (10):</strong> Converts Healing Power into Spell Damage (DPS mode)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class HighPriestOrigin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "high_priest");
    private static final UUID CAST_TIME_REDUCTION_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest", "grace_cast_speed");
    private static final UUID HOLY_SPELL_POWER_UUID = UUIDHelper.generateAttributeModifierUUID("high_priest", "holy_spell_power");

    // Tracker for Piety gained from healing in the current second (per player)
    private static final java.util.HashMap<UUID, PietyGainTracker> PIETY_GAIN_TRACKERS = new java.util.HashMap<>();

    /**
     * Tracker for Piety gained from healing to enforce per-second cap.
     */
    private static class PietyGainTracker {
        private double pietyGainedThisSecond = 0;
        private long lastSecond = -1;
        double addGain(double amount, double cap, long currentSecond) {
            if (lastSecond != currentSecond) {
                lastSecond = currentSecond;
                pietyGainedThisSecond = 0;
            }
            double actualGain = Math.min(amount, cap - pietyGainedThisSecond);
            pietyGainedThisSecond += actualGain;
            return actualGain;
        }
    }

    // Define Piety resource type (gold color)
    public static final ResourceType PIETY = ResourceType.register(
            ResourceLocation.fromNamespaceAndPath("complextalents", "piety"),
            "Piety",
            0,
            100,
            0xFFFFD700  // Gold color
    );

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
        OriginBuilder.create("complextalents", "high_priest")
                .displayName("High Priest")
                .description(Component.literal("Holy Judgment - Divine Retribution through perfect positioning"))
                .resourceType(PIETY)
                .maxLevel(5)
                // Grace stacks - gain over time, lose on damage
                .passiveStack("grace", PassiveStackDef.create("grace")
                        .maxStacks(10)
                        .displayName("Grace")
                        .color(0xFFE6F0FF).build())  // Light blue
                // Custom HUD renderer for Piety bar + Grace stacks
                .renderer(new HighPriestRenderer())
                // Piety gained on holy spell hit: [5, 8, 12, 15, 20]
                .scaledStat("pietyOnHolyHit", new double[]{5.0, 8.0, 12.0, 15.0, 20.0})
                // Piety gained on holy heal: [5, 8, 12, 15, 20]
                .scaledStat("pietyOnHolyHeal", new double[]{5.0, 8.0, 12.0, 15.0, 20.0})
                // Piety lost when hurt (decreases with level): [20, 18, 15, 12, 10]
                .scaledStat("pietyLostOnDamage", new double[]{20.0, 18.0, 15.0, 12.0, 10.0})
                // Grace tick interval in ticks (decreases with level): [100, 90, 80, 70, 60]
                .scaledStat("graceTickInterval", new double[]{100.0, 90.0, 80.0, 70.0, 60.0})
                // Cast time reduction per Grace stack: [2%, 3%, 4%, 5%, 6%]
                .scaledStat("castTimeReductionPerGrace", new double[]{0.04, 0.05, 0.06, 0.07, 0.08})
                // Healing potency per Grace stack: [5%, 6%, 7%, 8%, 10%]
                .scaledStat("healingPotencyPerGrace", new double[]{0.05, 0.06, 0.07, 0.08, 0.10})
                // Overheal to absorption conversion rate at max Grace: [30%, 40%, 50%, 60%, 75%]
                .scaledStat("overhealToAbsorptionRate", new double[]{0.30, 0.40, 0.50, 0.60, 0.75})
                // Absorption duration in ticks at max Grace: [600, 800, 1000, 1200, 1500] (30-75 seconds)
                .scaledStat("absorptionDuration", new double[]{600.0, 800.0, 1000.0, 1200.0, 1500.0})
                // Spell damage multiplier at max Grace: [150%, 175%, 200%, 225%, 250%]
                .scaledStat("holySpellPowerAtMaxGrace", new double[]{1.5, 1.75, 2.0, 2.25, 2.5})
                .register();
    }

    /**
     * Event handler for holy spell damage from Iron's Spellbooks.
     * High Priests gain Piety when dealing holy spell damage.
     */
    @SubscribeEvent
    public static void onHolySpellDamage(HolySpellDamageEvent event) {
        if (event.getCaster() instanceof ServerPlayer player) {
            if (!ID.equals(OriginManager.getOriginId(player))) {
                return;
            }

            // Get scaled stat based on origin level
            double pietyGain = OriginManager.getOriginStat(player, "pietyOnHolyHit");
            if (pietyGain > 0) {
                OriginManager.modifyResource(player, pietyGain);
            }
        }
    }

    /**
     * Event handler for holy spell heals from Iron's Spellbooks.
     * High Priests gain Piety, bonus healing potency, and overheal-to-absorption conversion.
     */
    @SubscribeEvent
    public static void onHolySpellHeal(HolySpellHealEvent event) {
        if (!(event.getCaster() instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Get Grace stacks
        int graceStacks = PassiveManager.getPassiveStacks(player, "grace");

        if (graceStacks > 0) {
            // Calculate healing potency bonus
            double potencyPerGrace = OriginManager.getOriginStat(player, "healingPotencyPerGrace");
            double bonusMultiplier = potencyPerGrace * graceStacks;
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

            TalentsMod.LOGGER.debug("High Priest applied healing potency: {} stacks = {}x multiplier (original: {}, bonus: {}, effective: {}, overheal: {})",
                    graceStacks, bonusMultiplier, originalHealAmount, bonusHeal, effectiveHeal, overheal);

            // At max Grace (10 stacks): convert overheal to absorption hearts
            if (graceStacks >= 10 && overheal > 0) {
                applyOverhealToAbsorption(player, target, overheal);
            }
        }

        // Gain Piety from holy heals (uses original amount, not bonus)
        // Capped at 2x pietyOnHolyHeal per second
        double pietyOnHolyHeal = OriginManager.getOriginStat(player, "pietyOnHolyHeal");
        if (pietyOnHolyHeal > 0) {
            UUID playerId = player.getUUID();
            PietyGainTracker tracker = PIETY_GAIN_TRACKERS.computeIfAbsent(playerId, k -> new PietyGainTracker());

            long currentSecond = player.level().getGameTime() / 20;
            double cap = 2.0 * pietyOnHolyHeal;
            double actualGain = tracker.addGain(pietyOnHolyHeal, cap, currentSecond);

            if (actualGain > 0) {
                OriginManager.modifyResource(player, actualGain);
            }
        }
    }

    /**
     * Apply overheal-to-absorption conversion for High Priest at max Grace.
     *
     * @param player  The High Priest casting the heal
     * @param target  The target entity receiving the overheal
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
                true
            );
            target.addEffect(effectInstance);

            TalentsMod.LOGGER.debug("High Priest converted overheal to absorption: {} health (duration: {} ticks)",
                    absorptionHealthToGrant, absorptionDuration);
        }
    }

    /**
     * Event handler for server-side ticking.
     * Handles Grace stack generation, passive Piety regeneration, and cast time reduction updates.
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

        // Get the tick interval for Grace generation (scales with level)
        double interval = OriginManager.getOriginStat(player, "graceTickInterval");
        int tickInterval = (int) interval;

        // Grace generation: gain 1 stack every interval ticks
        if (gameTime % tickInterval == 0) {
            int currentGrace = OriginManager.getPassiveStacks(player, "grace");
            if (currentGrace < 10) {
                OriginManager.modifyPassiveStacks(player, "grace", 1);
            }
        }
    }

    /**
     * Update the player's cast time reduction and holy spell power based on current Grace stacks.
     * Uses Iron's Spellbooks cast_time_reduction and holy_spell_power attributes.
     * Only updates when the Grace count actually changes (lazy update).
     */
    private static void updateCastTimeReduction(ServerPlayer player) {
        int graceStacks = OriginManager.getPassiveStacks(player, "grace");

        // Update cast time reduction (applies at any Grace stack)
        double reductionPerStack = OriginManager.getOriginStat(player, "castTimeReductionPerGrace");
        double totalReduction = reductionPerStack * graceStacks;

        ResourceLocation castTimeAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cast_time_reduction");
        Attribute castTimeAttr = ForgeRegistries.ATTRIBUTES.getValue(castTimeAttrId);

        if (castTimeAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(castTimeAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(CAST_TIME_REDUCTION_UUID);

                if (graceStacks > 0 && totalReduction > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                        CAST_TIME_REDUCTION_UUID,
                        "High Priest Grace Cast Speed",
                        totalReduction,
                        AttributeModifier.Operation.ADDITION
                    );
                    attributeInstance.addTransientModifier(modifier);
                }
            }
        }

        // Update holy spell power (only applies at max Grace - 10 stacks)
        ResourceLocation holyPowerAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "holy_spell_power");
        Attribute holyPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(holyPowerAttrId);

        if (holyPowerAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(holyPowerAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(HOLY_SPELL_POWER_UUID);

                if (graceStacks >= 10) {
                    double spellPowerMultiplier = OriginManager.getOriginStat(player, "holySpellPowerAtMaxGrace");
                    // Convert multiplier to bonus (e.g., 1.5 = +50% bonus)
                    double spellPowerBonus = spellPowerMultiplier - 1.0;

                    AttributeModifier modifier = new AttributeModifier(
                        HOLY_SPELL_POWER_UUID,
                        "High Priest Divine Retribution",
                        spellPowerBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                    );
                    attributeInstance.addTransientModifier(modifier);
                }
            }
        }
    }

    /**
     * Event handler for when player takes damage.
     * Lose Piety and ALL Grace stacks when hurt - punishment mechanic.
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

            // Lose Piety when hurt (amount scales with level - gets better at higher levels)
            double pietyLoss = OriginManager.getOriginStat(player, "pietyLostOnDamage");
            if (pietyLoss > 0) {
                OriginManager.modifyResource(player, -pietyLoss);
            }

            // Lose ALL Grace stacks when taking damage
            // PassiveStackChangeEvent will handle attribute updates
            OriginManager.setPassiveStacks(player, "grace", 0);
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
        updateCastTimeReduction(player);
    }

    /**
     * Event handler for player logout.
     * Clean up tracker data when player leaves.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PIETY_GAIN_TRACKERS.remove(player.getUUID());
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
        updateCastTimeReduction(player);
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
            // Update attributes when level changes (scaled stat changes)
            updateCastTimeReduction(player);
        }
    }

    /**
     * Event handler for passive stack changes.
     * Updates attributes when Grace stacks change.
     */
    @SubscribeEvent
    public static void onPassiveStackChange(PassiveStackChangeEvent event) {
        if (!"grace".equals(event.getStackTypeName())) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Update attributes when Grace stacks change
        updateCastTimeReduction(player);
    }
}
