package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.data.PlayerLevelingData;
import com.complextalents.leveling.events.level.PlayerLevelUpEvent;
import com.complextalents.leveling.events.xp.XPAwardedEvent;
import com.complextalents.leveling.events.xp.XPSource;
import com.complextalents.leveling.fatigue.ChunkFatigueData;
import com.complextalents.leveling.network.LevelDataSyncPacket;
import com.complextalents.network.PacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Handles network syncing of leveling data to clients.
 * Uses event-driven sync only - no polling ticks!
 *
 * <p>Syncs on:
 * <ul>
 *   <li>XP awards (XPAwardedEvent)</li>
 *   <li>Level-ups (PlayerLevelUpEvent)</li>
 *   <li>Player join (EntityJoinLevelEvent)</li>
 *   <li>Player respawn (PlayerEvent.PlayerRespawnEvent)</li>
 * </ul>
 *
 * <p>This is much more efficient than the previous 1-second polling approach.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class LevelingSyncHandler {

    /**
     * Syncs level data and notifies player of XP gains.
     */
    @SubscribeEvent
    public static void onXPAwarded(XPAwardedEvent event) {
        ServerPlayer player = event.getPlayer();
        syncPlayerLevelData(player);
        notifyXPGain(player, event);
    }

    /**
     * Syncs level data and notifies player on level-up.
     */
    @SubscribeEvent
    public static void onPlayerLevelUp(PlayerLevelUpEvent event) {
        ServerPlayer player = event.getPlayer();

        // Sync level data
        syncPlayerLevelData(player);

        // Send notification message
        Component message = Component.literal(
                String.format("§6✦ Level Up! §f%d → §e%d §f(+%d Skill Points)",
                        event.getOldLevel(), event.getNewLevel(), event.getSkillPointsAwarded())
        );
        player.displayClientMessage(message, false);

        // Could also play a sound here
        // player.playNotifySound(...);
    }

    /**
     * Syncs level data when player joins the level.
     */
    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide) {
            return;
        }

        syncPlayerLevelData(player);
    }

    /**
     * Syncs level data when player respawns.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerLevelData(player);
        }
    }

    /**
     * Sends level and fatigue data to the player.
     */
    private static void syncPlayerLevelData(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PlayerLevelingData levelingData = PlayerLevelingData.get(level);
        UUID uuid = player.getUUID();

        // Get level, XP, and calculate XP for next level
        int playerLevel = levelingData.getLevel(uuid);
        double currentXP = levelingData.getCurrentXP(uuid);
        double xpForNext = 100 + (Math.pow(playerLevel, 1.5) * 50);
        int availableSP = levelingData.getAvailableSkillPoints(uuid);

        // Get fatigue multiplier for current chunk
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        double fatigue = fatigueData.getMultiplier(chunkPos);

        // Send packet to player
        PacketHandler.sendTo(new LevelDataSyncPacket(playerLevel, currentXP, xpForNext, fatigue, availableSP), player);
    }

    /**
     * Displays a notification to the player about XP gain from secondary sources.
     * Primary combat XP (kills/assists) is not shown as it's obvious from gameplay.
     * Always shows severe chunk fatigue warnings (below 30%) regardless of source.
     */
    private static void notifyXPGain(ServerPlayer player, XPAwardedEvent event) {
        double finalAmount = event.getFinalAmount();
        double multiplier = event.getMultiplier();

        // Check for severe chunk fatigue (below 30%) - warn regardless of source
        if (multiplier < 0.30) {
            String fatigueWarning = String.format("§c⚠ CHUNK FATIGUE: §4%.0f%% XP penalty!",
                    (1 - multiplier) * 100);
            player.sendSystemMessage(Component.literal(fatigueWarning));
            return; // Show fatigue warning only, skip normal XP notification
        }

        // Skip primary combat sources - they're obvious from gameplay
        if (event.getSource() == XPSource.PRIMARY_COMBAT) {
            return;
        }

        if (finalAmount < 0.1) return; // Don't notify for negligible amounts

        String sourceName = event.getSource().getDisplayName();
        String message;

        // Check if fatigue reduced the XP
        if (multiplier < 0.99) {
            // Show fatigue reduction
            message = String.format("§a+%.1f XP §7(%s) §c[%.0f%% Effective]",
                    finalAmount, sourceName, multiplier * 100);
        } else {
            // Normal XP display
            message = String.format("§a+%.1f XP §7(%s)",
                    finalAmount, sourceName);
        }

        player.sendSystemMessage(Component.literal(message));
    }
}
