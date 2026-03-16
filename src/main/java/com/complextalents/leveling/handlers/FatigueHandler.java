package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.events.xp.XPAwardedEvent;
import com.complextalents.leveling.events.xp.XPPreAwardEvent;
import com.complextalents.leveling.service.FatigueService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles fatigue system for XP awards.
 * Applies fatigue multipliers to XP before it's awarded, and applies degradation after.
 *
 * <p>Fatigue is a per-chunk mechanic that reduces XP rewards after many kills
 * in the same area, encouraging exploration.</p>
 *
 * <p>Event priorities:
 * <ul>
 *   <li>XPPreAwardEvent: HIGH priority - modifies XP amount before awarding</li>
 *   <li>XPAwardedEvent: Normal priority - applies degradation after awarding</li>
 *   <li>LevelTick: Normal priority - handles recovery ticking</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class FatigueHandler {

    /**
     * Applies fatigue multiplier to XP before it's awarded.
     * Runs at HIGH priority to execute before other handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onXPPreAward(XPPreAwardEvent event) {
        ServerLevel level = event.getPlayer().serverLevel();
        ChunkPos chunkPos = event.getContext().getChunkPos();

        // Get the fatigue multiplier for this chunk
        double multiplier = FatigueService.getInstance().getMultiplier(level, chunkPos);

        // Apply multiplier to the XP amount
        event.multiplyAmount(multiplier);
    }

    /**
     * Applies fatigue degradation after XP is awarded.
     * Runs at normal priority after XP has been added.
     */
    @SubscribeEvent
    public static void onXPAwarded(XPAwardedEvent event) {
        ServerLevel level = event.getPlayer().serverLevel();
        ChunkPos chunkPos = event.getContext().getChunkPos();

        // Apply degradation based on the original XP amount
        // Use original, not final, so fatigue scales with base kill value not fatigue-adjusted value
        FatigueService.getInstance().applyDegradation(level, chunkPos, event.getOriginalAmount());
    }

    /**
     * Handles fatigue recovery ticking.
     * Recovers fatigue for all chunks gradually over time.
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;

        ServerLevel level = (ServerLevel) event.level;
        FatigueService.getInstance().tickRecovery(level);
    }
}
