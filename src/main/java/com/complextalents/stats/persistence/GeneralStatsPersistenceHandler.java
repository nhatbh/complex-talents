package com.complextalents.stats.persistence;

import com.complextalents.TalentsMod;
import com.complextalents.stats.capability.GeneralStatsDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles persistence for the General Stats module independently of core systems.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class GeneralStatsPersistenceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralStatsPersistenceHandler.class);

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        restoreStats(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        saveStats(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        saveStats(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        restoreStats(player);
    }

    private static void restoreStats(ServerPlayer player) {
        GeneralStatsSavedData data = GeneralStatsSavedData.get(player.serverLevel());
        player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(stats -> {
            CompoundTag tag = data.getStatsData(player.getUUID());
            if (tag != null) {
                stats.deserializeNBT(tag);
                LOGGER.info("[STATS-PERSISTENCE] Restored stats for {}", player.getUUID());
            }
            stats.sync();
        });
    }

    private static void saveStats(ServerPlayer player) {
        GeneralStatsSavedData data = GeneralStatsSavedData.get(player.serverLevel());
        player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(stats -> {
            data.saveStatsData(player.getUUID(), stats.serializeNBT());
            LOGGER.debug("[STATS-PERSISTENCE] Saved stats for {}", player.getUUID());
        });
    }
}
