package com.complextalents.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class TalentsCapabilities {
    public static final Capability<PlayerTalents> PLAYER_TALENTS = CapabilityManager.get(new CapabilityToken<PlayerTalents>() {});

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "player_talents");
}
