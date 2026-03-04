package com.complextalents.impl.darkmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import com.complextalents.impl.darkmage.skill.BloodPactSkill;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.SkillToggleTerminationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Blood Pact ongoing effects.
 * <p>
 * Handles:
 * <ul>
 *   <li>HP drain per second</li>
 *   <li>Infinite mana (set to max each tick)</li>
 *   <li>Auto-deactivate at critical HP</li>
 *   <li>Soul damage bonus during Blood Pact</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class BloodPactTickHandler {

    // Check every 2 ticks for performance
    private static final int TICK_INTERVAL = 2;

    /**
     * Server tick handler for Blood Pact effects.
     * - HP drain per second
     * - Infinite mana (set to max each tick)
     * - Auto-deactivate if HP drops to critical
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        long gameTime = server.getTickCount();

        // Only check every N ticks for performance
        if (gameTime % TICK_INTERVAL != 0) {
            return;
        }

        // Check all levels for Dark Mages with active Blood Pact
        for (ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    continue;
                }
                if (!serverPlayer.isAlive()) {
                    continue;
                }
                if (!DarkMageOrigin.isDarkMage(serverPlayer)) {
                    continue;
                }
                if (!isBloodPactActive(serverPlayer)) {
                    continue;
                }

                // Get drain rate from scaled stats
                double drainPerSecond = OriginManager.getOriginStat(serverPlayer, "bloodPactHpDrainPercent");
                // Convert to per-tick (20 ticks/sec) and multiply by interval
                float hpToDrain = (float) (serverPlayer.getMaxHealth() * drainPerSecond / 20.0 * TICK_INTERVAL);

                // Check if this would kill the player (leave at 1 HP minimum)
                if (serverPlayer.getHealth() - hpToDrain <= 1.0f) {
                    // Auto-deactivate at 1 HP
                    MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                            serverPlayer,
                            BloodPactSkill.ID,
                            SkillToggleTerminationEvent.TerminationReason.INSUFFICIENT_RESOURCE
                    ));
                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A7cBlood Pact deactivated - HP critical!"
                    ));
                    continue;
                }

                // Drain HP
                serverPlayer.setHealth(serverPlayer.getHealth() - hpToDrain);

                // Set mana to max (infinite mana effect)
                setManaToMax(serverPlayer);
            }
        }
    }

    /**
     * Set player's mana to maximum using Iron's Spellbooks API.
     * Uses the MAX_MANA attribute to determine the maximum value.
     */
    private static void setManaToMax(ServerPlayer player) {
        try {
            io.redspace.ironsspellbooks.api.magic.MagicData magicData =
                    io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
            // Get max mana from player's attribute
            double maxMana = player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
            magicData.setMana((float) maxMana);
        } catch (Exception e) {
            // Iron's Spellbooks not loaded or other issue - silently ignore
            TalentsMod.LOGGER.trace("Could not set mana for Blood Pact: {}", e.getMessage());
        }
    }

    /**
     * Handle damage dealt by Dark Mage - apply soul damage bonus during Blood Pact.
     * Uses HIGH priority to apply bonus before other modifiers.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // Check if attacker is a player
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Must be a Dark Mage
        if (!DarkMageOrigin.isDarkMage(player)) {
            return;
        }

        // Must have Blood Pact active
        if (!isBloodPactActive(player)) {
            return;
        }

        // Get soul count and damage bonus per soul
        double souls = SoulData.getSouls(player.getUUID());
        if (souls <= 0) {
            return;
        }

        double bonusPerSoul = OriginManager.getOriginStat(player, "soulDamageBonusPercent");
        float damageMultiplier = 1.0f + (float) (souls * bonusPerSoul);

        float originalDamage = event.getAmount();
        float newDamage = originalDamage * damageMultiplier;
        event.setAmount(newDamage);

        TalentsMod.LOGGER.debug("Blood Pact soul bonus: {} souls = {}x damage ({} -> {})",
                souls, damageMultiplier, originalDamage, newDamage);
    }

    /**
     * Check if a player has Blood Pact active.
     */
    public static boolean isBloodPactActive(ServerPlayer player) {
        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return false;
        }
        return data.isToggleActive(BloodPactSkill.ID);
    }
}
