package com.complextalents.passive.capability;

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
 * Forge capability provider for player passive stack data.
 * Used by both origins and skills.
 */
public class PassiveStackDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPassiveStackData> PASSIVE_STACK_DATA = CapabilityManager.get(
            new CapabilityToken<>() {}
    );

    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "passive_stack_data");

    private final IPassiveStackData instance;
    private final LazyOptional<IPassiveStackData> lazy;

    public PassiveStackDataProvider(ServerPlayer player) {
        this.instance = new PassiveStackData(player);
        this.lazy = LazyOptional.of(() -> instance);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return PASSIVE_STACK_DATA.orEmpty(cap, lazy);
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
