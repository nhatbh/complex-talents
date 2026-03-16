package com.complextalents.leveling.events;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.fatigue.ChunkFatigueData;
import com.complextalents.leveling.util.XPFormula;
import com.complextalents.persistence.PlayerPersistentData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import com.complextalents.network.PacketHandler;
import com.complextalents.leveling.network.LevelDataSyncPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Main event handler for the leveling system.
 * Manages XP distribution, assists, and fatigue using PlayerPersistentData (SavedData).
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class LevelingEventHandler {

    private static final long ASSIST_WINDOW_MS = 10000; // 10 seconds

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerPersistentData.get((ServerLevel) player.level()).recordAssist(
                player.getUUID(), 
                event.getEntity().getUUID(), 
                player.level().getGameTime() * 50
            );
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) victim.level();
        ChunkPos chunkPos = new ChunkPos(victim.blockPosition());
        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        double multiplier = fatigueData.getMultiplier(chunkPos);

        // Calculate XP
        double baseXP = XPFormula.calculatePrimaryXP(victim.getMaxHealth());
        double finalXP = baseXP * multiplier;

        PlayerPersistentData persistentData = PlayerPersistentData.get(level);
        UUID victimId = victim.getUUID();

        // Award XP to killer
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            persistentData.addXP(killer.getUUID(), finalXP);
            persistentData.clearAssist(killer.getUUID(), victimId);
        }

        // Award XP to assistants
        long currentTime = level.getGameTime() * 50;
        for (ServerPlayer player : level.players()) {
            if (player == event.getSource().getEntity()) continue;
            
            if (persistentData.hasAssist(player.getUUID(), victimId, currentTime, ASSIST_WINDOW_MS)) {
                persistentData.addXP(player.getUUID(), finalXP);
                persistentData.clearAssist(player.getUUID(), victimId);
            }
        }

        // Apply fatigue degradation ONCE per kill event
        fatigueData.applyDegradation(chunkPos, baseXP);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Death Penalty: Clear current level XP
            PlayerPersistentData.get((ServerLevel) player.level()).resetCurrentXP(player.getUUID());
        }
    }

    private static void awardXP(ServerPlayer player, double amount) {
        if (amount <= 0) return;
        
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        ChunkFatigueData fatigueData = ChunkFatigueData.get(level);
        
        double multiplier = fatigueData.getMultiplier(chunkPos);
        double finalAmount = amount * multiplier;
        
        PlayerPersistentData data = PlayerPersistentData.get(level);
        data.addXP(player.getUUID(), finalAmount);
        
        // Secondary sources also cause fatigue based on raw amount
        fatigueData.applyDegradation(chunkPos, amount);

        // Notify and sync
        player.displayClientMessage(Component.literal("\u00A7a+" + String.format("%.1f", finalAmount) + " XP"), true);
        syncLevelData(player);
    }

    public static void syncLevelData(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PlayerPersistentData data = PlayerPersistentData.get(level);
        UUID uuid = player.getUUID();
        
        int pLevel = data.getLevel(uuid);
        double xp = data.getCurrentXP(uuid);
        double xpForNext = 100 + (Math.pow(pLevel, 1.5) * 50);
        double fatigue = ChunkFatigueData.get(level).getMultiplier(new ChunkPos(player.blockPosition()));
        
        PacketHandler.sendTo(new LevelDataSyncPacket(pLevel, xp, xpForNext, fatigue), player);
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getLevel().isClientSide) {
            syncLevelData(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            ChunkFatigueData.get(serverLevel).tick(serverLevel);
            
            // Periodic sync (every 1 second) to keep fatigue HUD updated
            if (serverLevel.getGameTime() % 20 == 0) {
                for (ServerPlayer player : serverLevel.players()) {
                    syncLevelData(player);
                }
            }
        }
    }

    /**
     * Utility method to award secondary XP from other handlers.
     */
    public static void awardSecondaryXP(ServerPlayer player, double amount) {
        awardXP(player, amount);
    }
}
