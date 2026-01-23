package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class ActiveTalent extends Talent {
    private final int cooldownTicks;

    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, int cooldownTicks) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, slotType, ChatFormatting.AQUA);
        this.cooldownTicks = cooldownTicks;
    }

    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, int cooldownTicks, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, slotType, rarityColor);
        this.cooldownTicks = cooldownTicks;
    }

    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition, int cooldownTicks) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, slotType, requiredDefinition, ChatFormatting.AQUA);
        this.cooldownTicks = cooldownTicks;
    }

    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition, int cooldownTicks, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, slotType, requiredDefinition, rarityColor);
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * Legacy constructor for backwards compatibility
     * @deprecated Use constructor with TalentSlotType parameter
     */
    @Deprecated
    public ActiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, int cooldownTicks) {
        super(id, name, description, maxLevel, TalentType.ACTIVE, ChatFormatting.AQUA);
        this.cooldownTicks = cooldownTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
