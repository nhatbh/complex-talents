package com.complextalents.talent;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class PassiveTalent extends Talent {
    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel) {
        super(id, name, description, maxLevel, TalentType.PASSIVE);
    }
}
