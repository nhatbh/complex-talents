package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class HybridTalent extends Talent {
    private final int cooldownTicks;

    public HybridTalent(ResourceLocation id, Component name, Component description, int maxLevel, int cooldownTicks) {
        super(id, name, description, maxLevel, TalentType.HYBRID, ChatFormatting.GOLD);
        this.cooldownTicks = cooldownTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
