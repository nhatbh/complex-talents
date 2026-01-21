package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class ActiveTalent extends Talent {
    private final int cooldownTicks;

    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, int cooldownTicks) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, ChatFormatting.AQUA);
        this.cooldownTicks = cooldownTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
