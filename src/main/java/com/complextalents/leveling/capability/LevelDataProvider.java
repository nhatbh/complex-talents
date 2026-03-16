package com.complextalents.leveling.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for the PlayerLevelData capability.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class LevelDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerLevelData> PLAYER_LEVEL_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(TalentsMod.MODID, "player_level_data");

    private final IPlayerLevelData implementation;
    private final LazyOptional<IPlayerLevelData> optional;

    public LevelDataProvider(ServerPlayer player) {
        this.implementation = new PlayerLevelData(player);
        this.optional = LazyOptional.of(() -> implementation);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return PLAYER_LEVEL_DATA.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return implementation.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        implementation.deserializeNBT(nbt);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            event.addCapability(IDENTIFIER, new LevelDataProvider(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Logic for death penalty will be handled in a separate handler or here.
            // Persist Level and SP, but wipe Current XP.
            event.getOriginal().getCapability(PLAYER_LEVEL_DATA).ifPresent(oldData -> {
                event.getEntity().getCapability(PLAYER_LEVEL_DATA).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                    newData.resetCurrentLevelXP(); // Death Penalty: XP wiped to 0
                });
            });
        } else {
            // Just copy everything (e.g. dimension change)
            event.getOriginal().getCapability(PLAYER_LEVEL_DATA).ifPresent(oldData -> {
                event.getEntity().getCapability(PLAYER_LEVEL_DATA).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                });
            });
        }
    }
}
