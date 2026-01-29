package com.complextalents.skill.capability;

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
 * Event handlers for attaching and managing the skill data capability.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class SkillDataHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            SkillDataProvider provider = new SkillDataProvider(player);
            event.addCapability(SkillDataProvider.getCapabilityId(), provider);
            event.addListener(() -> provider.getCapability(SkillDataProvider.SKILL_DATA)
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

        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            // Update cooldowns and toggle resource consumption
            data.tick();
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy skill data on respawn (not death)
        if (event.isWasDeath()) {
            return;
        }

        event.getOriginal().getCapability(SkillDataProvider.SKILL_DATA).ifPresent(oldData -> {
            event.getEntity().getCapability(SkillDataProvider.SKILL_DATA).ifPresent(newData -> {
                // Copy slot assignments on respawn
                for (int i = 0; i < IPlayerSkillData.SLOT_COUNT; i++) {
                    newData.setSkillInSlot(i, oldData.getSkillInSlot(i));
                }
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Sync initial data to client on login
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Sync data when changing dimensions
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                data.sync();
            });
        }
    }

    /**
     * Helper method to get a player's skill data.
     *
     * @param player The server player
     * @return LazyOptional containing the skill data
     */
    public static LazyOptional<IPlayerSkillData> getSkillData(ServerPlayer player) {
        return player.getCapability(SkillDataProvider.SKILL_DATA);
    }

    /**
     * Helper method to get a player's skill data or throw.
     *
     * @param player The server player
     * @return The skill data
     * @throws IllegalStateException if the capability is not present
     */
    public static IPlayerSkillData getSkillDataOrThrow(ServerPlayer player) {
        return getSkillData(player)
                .orElseThrow(() -> new IllegalStateException("Player skill data capability not present"));
    }
}
