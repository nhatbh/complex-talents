package com.complextalents.impl.darkmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import com.complextalents.origin.OriginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Phylactery death-defy mechanic.
 * <p>
 * Intercepts fatal damage and saves the Dark Mage at the cost of 50% souls.
 * Uses HIGHEST priority to run before other damage handlers.
 * </p>
 * <p>
 * <strong>Trigger conditions:</strong>
 * <ul>
 *   <li>Player is a Dark Mage</li>
 *   <li>Damage would be fatal (health - damage <= 0)</li>
 *   <li>Player has at least 1 soul</li>
 *   <li>Phylactery is not on cooldown (5-minute cooldown)</li>
 * </ul>
 * <p>
 * <strong>Effects:</strong>
 * <ul>
 *   <li>Cancels fatal damage</li>
 *   <li>Sets HP to 1</li>
 *   <li>Loses 50% of souls</li>
 *   <li>5-minute internal cooldown</li>
 *   <li>Totem-like visual and audio effect</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class PhylacteryHandler {

    /**
     * Intercept fatal damage and trigger Phylactery if available.
     * Uses HIGHEST priority to run before other damage handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!DarkMageOrigin.isDarkMage(player)) {
            return;
        }

        // Check if this damage would be fatal
        float incomingDamage = event.getAmount();
        float currentHealth = player.getHealth();

        if (currentHealth - incomingDamage > 0) {
            return; // Not fatal, let it through
        }

        // Check soul count - need at least 1 soul
        double souls = SoulData.getSouls(player.getUUID());
        if (souls < 1.0) {
            return; // Not enough souls, cannot trigger Phylactery
        }

        // Check cooldown
        long currentTime = player.level().getGameTime();
        if (SoulData.isPhylacteryOnCooldown(player.getUUID(), currentTime)) {
            // On cooldown - player dies normally
            long remainingTicks = SoulData.getPhylacteryCooldownRemaining(player.getUUID(), currentTime);
            int remainingSeconds = (int) (remainingTicks / 20);
            player.sendSystemMessage(Component.literal(
                    "\u00A74Phylactery on cooldown! (" + remainingSeconds + "s remaining)"
            ));
            return;
        }

        // === TRIGGER PHYLACTERY! ===

        // 1. Cancel the fatal damage
        event.setAmount(0);
        event.setCanceled(true);

        // 2. Set HP to 1
        player.setHealth(1.0f);

        // 3. Lose 50% of souls
        double soulsLost = SoulData.loseSouls(player, 0.5);
        double remainingSouls = SoulData.getSouls(player.getUUID());

        // 4. Set internal cooldown (from scaled stats - 5 minutes)
        double cooldownSeconds = OriginManager.getOriginStat(player, "phylacteryCooldown");
        long cooldownTicks = (long) (cooldownSeconds * 20);
        SoulData.setPhylacteryCooldown(player.getUUID(), currentTime + cooldownTicks);

        // 5. Visual and audio feedback
        if (player.level() instanceof ServerLevel level) {
            // Totem-like effect
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

            // Additional soul-themed sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
        }

        // 6. Message
        player.sendSystemMessage(Component.literal(
                "\u00A75\u00A7lPHYLACTERY TRIGGERED!\u00A7r \u00A7dLost " + String.format("%.1f", soulsLost) +
                        " souls to cheat death! (" + String.format("%.1f", remainingSouls) + " remaining)"
        ));

        TalentsMod.LOGGER.info("Dark Mage {} triggered Phylactery! Souls: {:.2f} -> {:.2f} (lost {:.2f})",
                player.getName().getString(), souls, remainingSouls, soulsLost);
    }
}
