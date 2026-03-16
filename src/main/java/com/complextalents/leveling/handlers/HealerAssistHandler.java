package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.data.HealerAssistTracker;
import com.complextalents.leveling.events.xp.XPContext;
import com.complextalents.leveling.events.xp.XPSource;
import com.complextalents.leveling.service.LevelingService;
import com.complextalents.origin.integration.OriginModIntegrationHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Handles healer assist mechanics.
 * When an attacker who was previously healed by a healer assists or kills a mob,
 * the healer retroactively receives assist XP for enabling the kill through healing.
 *
 * <p>This enables support/healer playstyles to gain XP through healing combat participants.
 * Listens to all spell healing from Iron's Spellbooks, not just holy spells.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class HealerAssistHandler {

    private static final long HEAL_WINDOW_MS = 30000; // 30 seconds

    /**
     * Listens to spell healing events from Iron's Spellbooks and records the healing relationship.
     * When a healed player later deals damage or kills a mob, the healer receives assist XP.
     * Works with all spell schools, not just holy spells.
     */
    @SubscribeEvent
    public static void onSpellHeal(io.redspace.ironsspellbooks.api.events.SpellHealEvent event) {
        // Check if Iron's Spellbooks is loaded
        if (!OriginModIntegrationHandler.isIronSpellbooksLoaded()) {
            return;
        }

        try {
            LivingEntity healer = event.getEntity();
            LivingEntity healedPlayer = event.getTargetEntity();

            // Only process if both caster and target are ServerPlayers
            if (!(healer instanceof ServerPlayer healerPlayer)) {
                return;
            }

            if (!(healedPlayer instanceof ServerPlayer healedPlayerObj)) {
                return;
            }

            if (healedPlayer.level().isClientSide) {
                return;
            }

            ServerLevel level = (ServerLevel) healedPlayer.level();
            long currentTime = level.getGameTime() * 50; // Convert ticks to milliseconds
            UUID healedPlayerId = healedPlayerObj.getUUID();
            UUID healerUUID = healerPlayer.getUUID();

            // Record this healing relationship
            HealerAssistTracker.recordHeal(healedPlayerId, healerUUID, currentTime);

            TalentsMod.LOGGER.debug(
                "Recorded heal: {} healed {} for {} health",
                healerPlayer.getName().getString(),
                healedPlayerObj.getName().getString(),
                event.getHealAmount()
            );
        } catch (Exception e) {
            TalentsMod.LOGGER.debug("Error processing SpellHealEvent for healer tracking: {}", e.getMessage());
        }
    }

    /**
     * Called by AssistHandler when an attacker gets assist XP for a kill.
     * Checks if this attacker was recently healed and awards the healer with assist XP.
     *
     * @param attacker     The ServerPlayer who assisted in the kill
     * @param killTime     The timestamp of the kill in milliseconds
     * @param victimHealth The health of the killed entity
     */
    public static void awardHealerAssist(ServerPlayer attacker, long killTime, double victimHealth) {
        UUID attackerId = attacker.getUUID();

        // Check if this attacker was recently healed
        UUID healerUUID = HealerAssistTracker.getMostRecentHealer(attackerId, killTime, HEAL_WINDOW_MS);
        if (healerUUID == null) {
            return; // No recent healer found
        }

        // Find the healer player on the server
        ServerLevel level = (ServerLevel) attacker.level();
        ServerPlayer healer = level.getServer().getPlayerList().getPlayer(healerUUID);
        if (healer == null || healer.level().isClientSide) {
            return; // Healer offline or client-side
        }

        // Award healer assist XP
        double baseXP = com.complextalents.leveling.util.XPFormula.calculatePrimaryXP(victimHealth);

        ChunkPos chunkPos = new ChunkPos(healer.blockPosition());
        XPContext context = XPContext.builder()
                .source(XPSource.PRIMARY_COMBAT)
                .chunkPos(chunkPos)
                .rawAmount(baseXP)
                .metadata("isHealerAssist", true)
                .metadata("assistedByUUID", attackerId.toString())
                .metadata("victimMaxHealth", victimHealth)
                .build();

        LevelingService.getInstance().awardXP(healer, baseXP, XPSource.PRIMARY_COMBAT, context);

        TalentsMod.LOGGER.debug(
            "Healer {} received assist XP for healing attacker {} who got a kill",
            healer.getName().getString(),
            attacker.getName().getString()
        );
    }
}
