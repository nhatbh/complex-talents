package com.complextalents.spellmastery.persistence;

import com.complextalents.TalentsMod;
import com.complextalents.spellmastery.capability.SpellMasteryDataProvider;
import com.complextalents.stats.persistence.GeneralStatsSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles persistence for the Spell Mastery module using GeneralStatsSavedData.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class SpellMasteryPersistenceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpellMasteryPersistenceHandler.class);

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        restoreMastery(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        saveMastery(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        saveMastery(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        restoreMastery(player);
    }

    private static void restoreMastery(ServerPlayer player) {
        GeneralStatsSavedData data = GeneralStatsSavedData.get(player.serverLevel());
        player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(mastery -> {
            CompoundTag tag = data.getStatsData(player.getUUID());
            if (tag != null && tag.contains("SpellMastery")) {
                mastery.deserializeNBT(tag.getCompound("SpellMastery"));
                LOGGER.info("[MASTERY-PERSISTENCE] Restored mastery for {}", player.getUUID());
            }
            mastery.sync();
        });
    }

    private static void saveMastery(ServerPlayer player) {
        GeneralStatsSavedData data = GeneralStatsSavedData.get(player.serverLevel());
        player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(mastery -> {
            CompoundTag playerTag = data.getStatsData(player.getUUID());
            if (playerTag == null) {
                playerTag = new CompoundTag();
            }
            playerTag.put("SpellMastery", mastery.serializeNBT());
            data.saveStatsData(player.getUUID(), playerTag);
            LOGGER.debug("[MASTERY-PERSISTENCE] Saved mastery for {}", player.getUUID());
        });
    }
}
