package com.complextalents.persistence;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import com.complextalents.origin.capability.OriginDataProvider;
import com.complextalents.passive.capability.PassiveStackDataProvider;
import com.complextalents.skill.capability.SkillDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles persistence of player capability data using SavedData.
 * Replaces the unreliable reviveCaps() approach in Clone event.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class PlayerDataPersistenceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataPersistenceHandler.class);

    // --- LOGIN: Restore from SavedData ---

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var level = player.serverLevel();
        PlayerPersistentData persistentData = PlayerPersistentData.get(level);
        java.util.UUID playerId = player.getUUID();

        // Restore origin data
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(originData -> {
            CompoundTag savedTag = persistentData.getOriginData(playerId);
            if (savedTag != null) {
                originData.deserializeNBT(savedTag);
                LOGGER.info("[PERSISTENCE] Restored origin data for player {}", playerId);
            }
            originData.sync(); // Sync to client after restore
        });

        // Restore skill data (slots, levels, form, cooldowns - but NOT toggles)
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(cap -> {
            if (cap instanceof com.complextalents.skill.capability.PlayerSkillData skillData) {
                CompoundTag savedTag = persistentData.getSkillData(playerId);
                if (savedTag != null) {
                    skillData.deserializeNBT(savedTag);
                    skillData.syncCooldowns(); // Sync restored cooldowns to client
                    LOGGER.info("[PERSISTENCE] Restored skill data for player {}", playerId);
                }
            }
            cap.sync(); // Sync to client after restore
        });

        // Restore passive stack data
        player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(passiveData -> {
            CompoundTag savedTag = persistentData.getPassiveData(playerId);
            if (savedTag != null) {
                passiveData.deserializeNBT(savedTag);
                LOGGER.info("[PERSISTENCE] Restored passive data for player {}", playerId);
            }
            passiveData.sync(); // Sync to client after restore
        });

        // Restore Dark Mage soul data (check origin after capability restore)
        if (DarkMageOrigin.isDarkMage(player)) {
            CompoundTag savedTag = persistentData.getDarkMageData(playerId);
            if (savedTag != null) {
                SoulData.deserializeNBT(player, savedTag);
                LOGGER.info("[PERSISTENCE] Restored Dark Mage soul data for player {}", playerId);
            }
        }
    }

    // --- LOGOUT: Save to SavedData ---

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        savePlayerData(player);
        LOGGER.debug("[PERSISTENCE] Saved player data on logout for {}", player.getUUID());
    }

    // --- DEATH: Save to SavedData (before capabilities are invalidated) ---

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Save data immediately on death, before capabilities are invalidated
        savePlayerData(player);
        LOGGER.info("[PERSISTENCE] Saved player data on death for {}", player.getUUID());
    }

    // --- RESPAWN: Restore from SavedData (Clone event is now just a trigger) ---

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Only process on death
        if (!event.isWasDeath()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        var level = newPlayer.serverLevel();
        PlayerPersistentData persistentData = PlayerPersistentData.get(level);
        java.util.UUID playerId = newPlayer.getUUID();

        // Restore origin data from SavedData
        newPlayer.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(originData -> {
            CompoundTag savedTag = persistentData.getOriginData(playerId);
            if (savedTag != null) {
                originData.deserializeNBT(savedTag);
                LOGGER.info("[CLONE] Restored origin data from SavedData for {}", playerId);
            }
        });

        // Restore skill data from SavedData (slots, levels, form only)
        newPlayer.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(cap -> {
            if (cap instanceof com.complextalents.skill.capability.PlayerSkillData skillData) {
                CompoundTag savedTag = persistentData.getSkillData(playerId);
                if (savedTag != null) {
                    skillData.deserializeNBT(savedTag);
                    LOGGER.info("[CLONE] Restored skill data from SavedData for {}", playerId);
                }
                // Note: deserializeNBT in PlayerSkillData already skips toggles/cooldowns
            }
        });

        // Restore passive stack data from SavedData
        newPlayer.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(passiveData -> {
            CompoundTag savedTag = persistentData.getPassiveData(playerId);
            if (savedTag != null) {
                passiveData.deserializeNBT(savedTag);
                LOGGER.info("[CLONE] Restored passive data from SavedData for {}", playerId);
            }
        });

        // Restore Dark Mage soul data from SavedData
        if (DarkMageOrigin.isDarkMage(newPlayer)) {
            CompoundTag savedTag = persistentData.getDarkMageData(playerId);
            if (savedTag != null) {
                SoulData.deserializeNBT(newPlayer, savedTag);
                LOGGER.info("[CLONE] Restored Dark Mage soul data from SavedData for {}", playerId);
            }
        }
    }

    // --- PERIODIC SAVE (Backup safety net) ---

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Save every 60 seconds (1200 ticks)
        if (event.getServer().getTickCount() % 1200 == 0) {
            saveAllOnlinePlayers(event.getServer());
        }
    }

    // --- HELPER METHODS ---

    /**
     * Save all capability data for a player to SavedData.
     */
    private static void savePlayerData(ServerPlayer player) {
        var level = player.serverLevel();
        PlayerPersistentData persistentData = PlayerPersistentData.get(level);
        java.util.UUID playerId = player.getUUID();

        // Save origin data
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            persistentData.saveOriginData(playerId, data.serializeNBT());
        });

        // Save skill data
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(cap -> {
            if (cap instanceof com.complextalents.skill.capability.PlayerSkillData skillData) {
                persistentData.saveSkillData(playerId, skillData.serializeNBT());
            }
        });

        // Save passive data
        player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
            persistentData.savePassiveData(playerId, data.serializeNBT());
        });

        // Save Dark Mage soul data
        if (DarkMageOrigin.isDarkMage(player)) {
            persistentData.saveDarkMageData(playerId, SoulData.serializeNBT(player));
        }
    }

    /**
     * Save data for all online players. Called periodically as a backup.
     */
    private static void saveAllOnlinePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            savePlayerData(player);
        }
        LOGGER.debug("[PERSISTENCE] Periodic save completed for {} players",
                server.getPlayerList().getPlayerCount());
    }
}
