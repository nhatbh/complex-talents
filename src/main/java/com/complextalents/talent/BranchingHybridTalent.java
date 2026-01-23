package com.complextalents.talent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A hybrid talent that supports branching paths
 */
public abstract class BranchingHybridTalent extends HybridTalent implements BranchingTalentBase {

    public BranchingHybridTalent(ResourceLocation id, Component name, Component description,
                                int maxLevel, TalentSlotType slotType, int cooldownTicks) {
        super(id, name, description, maxLevel, slotType, cooldownTicks);
    }

    public BranchingHybridTalent(ResourceLocation id, Component name, Component description,
                                int maxLevel, TalentSlotType slotType, int cooldownTicks, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, slotType, cooldownTicks, rarityColor);
    }

    public BranchingHybridTalent(ResourceLocation id, Component name, Component description,
                                int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition, int cooldownTicks) {
        super(id, name, description, maxLevel, slotType, requiredDefinition, cooldownTicks);
    }

    public BranchingHybridTalent(ResourceLocation id, Component name, Component description,
                                int maxLevel, TalentSlotType slotType, ResourceLocation requiredDefinition,
                                int cooldownTicks, ChatFormatting rarityColor) {
        super(id, name, description, maxLevel, slotType, requiredDefinition, cooldownTicks, rarityColor);
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