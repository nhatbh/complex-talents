package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.data.LevelStats;
import com.complextalents.leveling.data.PlayerLevelingData;
import com.complextalents.leveling.events.xp.XPAwardedEvent;
import com.complextalents.leveling.service.LevelingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles level-up detection and event firing.
 * Listens to XPAwardedEvent and checks if the player has leveled up.
 *
 * <p>When a level-up is detected, fires PlayerLevelUpEvent for other systems
 * (like sync handlers) to react to.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class LevelUpHandler {

    /**
     * Checks for level-ups after XP is awarded.
     */
    @SubscribeEvent
    public static void onXPAwarded(XPAwardedEvent event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = player.serverLevel();

        // Get the player's current stats
        PlayerLevelingData data = PlayerLevelingData.get(level);
        LevelStats oldStats = data.getStats(player.getUUID());

        // Calculate the level that should correspond to the player's total XP
        // This works because PlayerLevelingData.addXP() performs the level-up calculation
        // internally, so we can reconstruct the new level from total XP
        int calculatedLevel = calculateLevelFromTotalXP(oldStats.getTotalXP());
        if (calculatedLevel > oldStats.getLevel()) {
            // A level-up occurred
            LevelingService.getInstance().fireLevelUpEvent(
                    player,
                    oldStats.getLevel(),
                    calculatedLevel,
                    2 * (calculatedLevel - oldStats.getLevel()) // 2 skill points per level
            );
        }
    }

    /**
     * Calculates the current level from total accumulated XP.
     * This is the inverse of the XP formula.
     */
    private static int calculateLevelFromTotalXP(double totalXP) {
        int level = 1;
        double xpSpent = 0;

        // XP required for next level: 100 + (level^1.5 * 50)
        while (true) {
            double xpForNext = 100 + (Math.pow(level, 1.5) * 50);
            if (xpSpent + xpForNext > totalXP) {
                break; // Can't level up anymore
            }
            xpSpent += xpForNext;
            level++;
        }

        return level;
    }
}
