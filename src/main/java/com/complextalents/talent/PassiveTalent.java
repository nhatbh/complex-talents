package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class PassiveTalent extends Talent {
    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType);
    }

    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType, rarityColor);
    }

    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType, requiredDefinition);
    }

    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType, requiredDefinition, rarityColor);
    }

    /**
     * Constructor for Definition talents with a resource bar
     */
    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceBarConfig resourceBarConfig) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType, null, resourceBarConfig, ChatFormatting.WHITE);
    }

    /**
     * Constructor for Definition talents with a resource bar and custom rarity color
     */
    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel, TalentSlotType slotType, ResourceBarConfig resourceBarConfig, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, TalentType.PASSIVE, slotType, null, resourceBarConfig, rarityColor);
    }

    /**
     * Legacy constructor for backwards compatibility
     * @deprecated Use constructor with TalentSlotType parameter
     */
    @Deprecated
    public PassiveTalent(ResourceLocation id, Component name, Component description, int maxLevel) {
        super(id, name, description, maxLevel, TalentType.PASSIVE);
    }
}
