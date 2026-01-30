package com.complextalents.origin.capability;

import com.complextalents.TalentsMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forge capability provider for player origin data.
 * Mirrors SkillDataProvider pattern.
 */
public class OriginDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerOriginData> ORIGIN_DATA = CapabilityManager.get(
            new CapabilityToken<>() {}
    );

    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "origin_data");

    private final IPlayerOriginData instance;
    private final LazyOptional<IPlayerOriginData> lazy;

    public OriginDataProvider(ServerPlayer player) {
        this.instance = new PlayerOriginData(player);
        this.lazy = LazyOptional.of(() -> instance);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ORIGIN_DATA.orEmpty(cap, lazy);
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    /**
     * Get the capability ID for this provider.
     */
    public static ResourceLocation getCapabilityId() {
        return CAPABILITY_ID;
    }
}
