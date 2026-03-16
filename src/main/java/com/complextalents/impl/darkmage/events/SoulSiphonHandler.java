package com.complextalents.impl.darkmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.complextalents.leveling.util.XPFormula;
import com.complextalents.leveling.events.LevelingEventHandler;

/**
 * Event handler for Soul Siphon passive.
 * Dark Mages gain souls when they kill enemies.
 * Souls gained = 3 × √(HP/10) - 5 (generous early, harsh late-game brake).
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class SoulSiphonHandler {

    /**
     * Handle enemy deaths - grant souls to Dark Mage killers.
     * Souls gained = 3 × √(HP/10) - 5 (generous early, harsh late-game brake).
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity victim = event.getEntity();

        // Don't grant souls for player kills (PvP protection)
        if (victim instanceof ServerPlayer) {
            return;
        }

        // Find the killer
        LivingEntity killer = victim.getKillCredit();

        // Try to get killer from damage source if kill credit is null
        if (killer == null && event.getSource().getEntity() instanceof LivingEntity living) {
            killer = living;
        }

        // Must be a player kill
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        // Must be a Dark Mage
        if (!DarkMageOrigin.isDarkMage(player)) {
            return;
        }

        // Calculate souls using the offset root formula
        float maxHealth = victim.getMaxHealth();
        double soulsGained = SoulData.calculateSoulsFromKill(maxHealth);

        // Add souls and sync
        SoulData.addSouls(player, soulsGained);

        // Award Soul Hoarder XP
        double soulXP = XPFormula.calculateDarkMageSoulHoarderXP(soulsGained);
        LevelingEventHandler.awardSecondaryXP(player, soulXP);

        // Award Edge of Death XP
        float currentHPPercentage = player.getHealth() / player.getMaxHealth();
        double edgeXP = XPFormula.calculateDarkMageEdgeOfDeathXP(victim.getMaxHealth(), currentHPPercentage);
        LevelingEventHandler.awardSecondaryXP(player, edgeXP);

        // Soul escape particles at the killed mob (more souls = more particles)
        if (victim.level() instanceof ServerLevel serverLevel) {
            int count = Math.max(1, Math.min(40, (int) (soulsGained * 2)));
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(),
                    count, 0.3, 0.4, 0.3, 0.05);
        }

        // Send chat message to player
        double totalSouls = SoulData.getSouls(player);
        player.sendSystemMessage(Component.literal(
                "\u00A75+" + String.format("%.1f", soulsGained) + " Souls \u00A78(" +
                        String.format("%.1f", totalSouls) + " total)"
        ));

        TalentsMod.LOGGER.debug("Dark Mage {} gained {:.2f} souls from killing {} (max HP: {}, total souls: {:.2f})",
                player.getName().getString(),
                soulsGained,
                victim.getName().getString(),
                maxHealth,
                totalSouls);
    }
}
