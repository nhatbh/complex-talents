package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A passive talent that supports branching paths
 */
public abstract class BranchingPassiveTalent extends PassiveTalent implements BranchingTalentBase {

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType) {
        super(id, name, description, maxLevel, slotType);
    }

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, slotType, rarityColor);
    }

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition) {
        super(id, name, description, maxLevel, slotType, requiredDefinition);
    }

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition,
                                 ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, slotType, requiredDefinition, rarityColor);
    }

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType, ResourceBarConfig resourceBarConfig) {
        super(id, name, description, maxLevel, slotType, resourceBarConfig);
    }

    public BranchingPassiveTalent(ResourceLocation id, Component name, Component description,
                                 int maxLevel, TalentSlotType slotType, ResourceBarConfig resourceBarConfig,
                                 ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, slotType, resourceBarConfig, rarityColor);
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Clear branch selections when talent is removed
        clearPlayerBranches(player);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Check if this level introduces a new branch
        if (hasBranchingAtRank(level)) {
            // Could trigger UI or notification for branch selection
            player.sendSystemMessage(Component.translatable(
                "talent.complextalents.branch_available",
                getName(), level
            ).withStyle(ChatFormatting.GOLD));
        }
    }
}