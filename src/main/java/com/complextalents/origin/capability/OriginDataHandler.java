package com.complextalents.origin.capability;

import com.complextalents.TalentsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handlers for attaching and managing the origin data capability.
 * Mirrors SkillDataHandler pattern.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class OriginDataHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            OriginDataProvider provider = new OriginDataProvider(player);
            event.addCapability(OriginDataProvider.getCapabilityId(), provider);
            event.addListener(() -> provider.getCapability(OriginDataProvider.ORIGIN_DATA)
                    .invalidate());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only process on server side, at end of tick
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            // Tick for passive effects
            data.tick();
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy origin data on respawn (not death)
        if (event.isWasDeath()) {
            return;
        }

        event.getOriginal().getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(oldData -> {
            event.getEntity().getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(newData -> {
                // Copy origin and level on respawn
                newData.setActiveOrigin(oldData.getActiveOrigin());
                newData.setOriginLevel(oldData.getOriginLevel());
                // Resource resets to min on respawn
                newData.setResource(newData.getResourceType() != null ? newData.getResourceType().getMin() : 0);
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Sync initial data to client on login
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Sync data when changing dimensions
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    /**
     * Reset passive stacks on player death.
     * Passive stacks are meant to be lost on death as a gameplay mechanic.
     * Now handled by the shared passive capability.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            player.getCapability(com.complextalents.passive.capability.PassiveStackDataProvider.PASSIVE_STACK_DATA)
                    .ifPresent(data -> data.resetPassiveStacks());
        }
    }

    /**
     * Helper method to get a player's origin data.
     *
     * @param player The server player
     * @return LazyOptional containing the origin data
     */
    public static LazyOptional<IPlayerOriginData> getOriginData(ServerPlayer player) {
        return player.getCapability(OriginDataProvider.ORIGIN_DATA);
    }

    /**
     * Helper method to get a player's origin data or throw.
     *
     * @param player The server player
     * @return The origin data
     * @throws IllegalStateException if the capability is not present
     */
    public static IPlayerOriginData getOriginDataOrThrow(ServerPlayer player) {
        return getOriginData(player)
                .orElseThrow(() -> new IllegalStateException("Player origin data capability not present"));
    }
}
