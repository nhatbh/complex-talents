package com.complextalents.skill.capability;

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
 * Forge capability provider for player skill data.
 */
public class SkillDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerSkillData> SKILL_DATA = CapabilityManager.get(
            new CapabilityToken<>() {}
    );

    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "skill_data");

    private final IPlayerSkillData instance;
    private final LazyOptional<IPlayerSkillData> lazy;

    public SkillDataProvider(ServerPlayer player) {
        this.instance = new PlayerSkillData(player);
        this.lazy = LazyOptional.of(() -> instance);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return SKILL_DATA.orEmpty(cap, lazy);
    }

    @Override
    public CompoundTag serializeNBT() {
        return ((PlayerSkillData) instance).serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ((PlayerSkillData) instance).deserializeNBT(nbt);
    }

    /**
     * Get the capability ID for this provider.
     */
    public static ResourceLocation getCapabilityId() {
        return CAPABILITY_ID;
    }
}
