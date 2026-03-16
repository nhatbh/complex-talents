package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.data.AssistTracker;
import com.complextalents.leveling.data.KillParticipantTracker;
import com.complextalents.leveling.events.xp.XPContext;
import com.complextalents.leveling.events.xp.XPSource;
import com.complextalents.leveling.service.LevelingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Handles player assists during combat.
 * Records damage dealt to entities and awards XP to all assistants when the entity dies.
 *
 * <p>Assist window is 10 seconds. Players who dealt damage within this window receive
 * XP equal to the killer's XP reward.</p>
 *
 * <p>Also tracks kill participants for 30 seconds so that healers can receive assist XP
 * when they heal a player who participated in a kill.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class AssistHandler {

    private static final long ASSIST_WINDOW_MS = 10000; // 10 seconds

    /**
     * Records a player's damage on an entity (tracking assists).
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            long timestamp = player.level().getGameTime() * 50; // Convert ticks to milliseconds
            AssistTracker.recordAssist(
                    player.getUUID(),
                    event.getEntity().getUUID(),
                    timestamp
            );
        }
    }

    /**
     * Awards XP to all players who assisted in killing the entity.
     * Also tracks kill participants for 30 seconds so healers can receive assist XP.
     * Runs at LOW priority to execute after PrimaryXPHandler.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        ServerLevel level = (ServerLevel) victim.level();
        ChunkPos chunkPos = new ChunkPos(victim.blockPosition());
        UUID victimId = victim.getUUID();

        long currentTime = level.getGameTime() * 50; // Convert ticks to milliseconds

        // Award XP to all players who assisted (but didn't kill)
        for (ServerPlayer player : level.players()) {
            // Skip the killer (they already got XP from PrimaryXPHandler)
            if (player == event.getSource().getEntity()) continue;

            // Check if player has assist within window
            if (AssistTracker.hasAssist(player.getUUID(), victimId, currentTime, ASSIST_WINDOW_MS)) {
                // Calculate assist XP (same as killer XP)
                double baseXP = com.complextalents.leveling.util.XPFormula.calculatePrimaryXP(victim.getMaxHealth());

                // Build context
                XPContext context = XPContext.builder()
                        .source(XPSource.PRIMARY_COMBAT)
                        .chunkPos(chunkPos)
                        .rawAmount(baseXP)
                        .metadata("isAssist", true)
                        .metadata("victimMaxHealth", victim.getMaxHealth())
                        .build();

                // Award assist XP via service
                LevelingService.getInstance().awardXP(player, baseXP, XPSource.PRIMARY_COMBAT, context);

                // Check if this attacker was recently healed and award healer assist XP
                HealerAssistHandler.awardHealerAssist(player, currentTime, victim.getMaxHealth());

                // Record this player as a kill participant (for healer assist XP)
                KillParticipantTracker.recordParticipant(victimId, player.getUUID(), currentTime);

                // Clear assist
                AssistTracker.clearAssist(player.getUUID(), victimId);
            }
        }

        // Also check if the killer was recently healed and award healer assist XP
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            KillParticipantTracker.recordParticipant(victimId, killer.getUUID(), currentTime);
            // Check if killer was healed and award healer assist XP
            HealerAssistHandler.awardHealerAssist(killer, currentTime, victim.getMaxHealth());
        }
    }
}
