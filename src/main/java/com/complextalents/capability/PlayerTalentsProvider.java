package com.complextalents.capability;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SyncTalentsPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class PlayerTalentsProvider {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PlayerTalentsProvider::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerTalents.class);
    }

    @SubscribeEvent
    public static void attachPlayerCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            TalentsMod.LOGGER.debug("Attaching PlayerTalents capability to player (UUID: {})", player.getUUID());
            event.addCapability(TalentsCapabilities.ID, new ICapabilitySerializable<CompoundTag>() {
                private final PlayerTalentsImpl talents = new PlayerTalentsImpl();
                private final LazyOptional<PlayerTalents> lazyOptional = LazyOptional.of(() -> {
                    talents.setOwningPlayer(player);
                    return talents;
                });

                @NotNull
                @Override
                public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    if (cap == TalentsCapabilities.PLAYER_TALENTS) {
                        return lazyOptional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT() {
                    return talents.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    talents.deserializeNBT(nbt);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getOriginal().getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(oldTalents -> {
                event.getEntity().getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(newTalents -> {
                    if (newTalents instanceof PlayerTalentsImpl impl && oldTalents instanceof PlayerTalentsImpl oldImpl) {
                        impl.deserializeNBT(oldImpl.serializeNBT());
                    }
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerTalents(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerTalents(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerTalents(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                if (talents instanceof PlayerTalentsImpl impl) {
                    // Only process cooldowns every 10 ticks (0.5 seconds) to reduce overhead
                    if (player.tickCount % 10 == 0) {
                        impl.tickCooldowns(player);
                    }
                    // Batched network sync every tick (but only sends if dirty)
                    impl.tickSync();
                }
            });
        }
    }

    private static void syncPlayerTalents(ServerPlayer player) {
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            if (talents instanceof PlayerTalentsImpl impl) {
                PacketHandler.sendTo(new SyncTalentsPacket(impl.serializeNBT()), player);
            }
        });
    }
}
