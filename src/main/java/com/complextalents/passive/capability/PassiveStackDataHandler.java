package com.complextalents.passive.capability;

import com.complextalents.TalentsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handlers for attaching and managing the passive stack data capability.
 * Used by both origins and skills.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class PassiveStackDataHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            PassiveStackDataProvider provider = new PassiveStackDataProvider(player);
            event.addCapability(PassiveStackDataProvider.getCapabilityId(), provider);
            event.addListener(() -> provider.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA)
                    .invalidate());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only process on server side, at end of tick
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        // Stack-specific logic is handled by origin/skill event handlers
        // No periodic tick needed for passive stack capability itself
    }

    // Clone event is now handled by PlayerDataPersistenceHandler using SavedData
    // This removes the dependency on reviveCaps() which was unreliable

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Sync initial data to client on login
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Sync data when changing dimensions
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    /**
     * Helper method to get a player's passive stack data.
     *
     * @param player The server player
     * @return LazyOptional containing the passive stack data
     */
    public static LazyOptional<IPassiveStackData> getPassiveStackData(ServerPlayer player) {
        return player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA);
    }

    /**
     * Helper method to get a player's passive stack data or throw.
     *
     * @param player The server player
     * @return The passive stack data
     * @throws IllegalStateException if the capability is not present
     */
    public static IPassiveStackData getPassiveStackDataOrThrow(ServerPlayer player) {
        return getPassiveStackData(player)
                .orElseThrow(() -> new IllegalStateException("Player passive stack data capability not present"));
    }
}
