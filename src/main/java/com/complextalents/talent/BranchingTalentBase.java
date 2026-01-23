package com.complextalents.talent;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base interface for talents with branching paths at certain ranks
 * Can be implemented by any talent type (Passive, Active, or Hybrid)
 */
public interface BranchingTalentBase {

    /**
     * Get the talent's resource location ID
     */
    ResourceLocation getId();

    /**
     * Get the branch path a player has chosen for a specific rank
     */
    default TalentBranches.BranchChoice getPlayerBranch(ServerPlayer player, int rank) {
        return TalentBranches.getBranch(player, getId(), rank);
    }

    /**
     * Check if player has selected path A at a specific rank
     */
    default boolean hasPathA(ServerPlayer player, int rank) {
        return getPlayerBranch(player, rank) == TalentBranches.BranchChoice.PATH_A;
    }

    /**
     * Check if player has selected path B at a specific rank
     */
    default boolean hasPathB(ServerPlayer player, int rank) {
        return getPlayerBranch(player, rank) == TalentBranches.BranchChoice.PATH_B;
    }

    /**
     * Check if player has selected path C at a specific rank
     */
    default boolean hasPathC(ServerPlayer player, int rank) {
        return getPlayerBranch(player, rank) == TalentBranches.BranchChoice.PATH_C;
    }

    /**
     * Check if player has selected path D at a specific rank
     */
    default boolean hasPathD(ServerPlayer player, int rank) {
        return getPlayerBranch(player, rank) == TalentBranches.BranchChoice.PATH_D;
    }

    /**
     * Set the branch path a player has chosen for a specific rank
     */
    default void setPlayerBranch(ServerPlayer player, int rank, TalentBranches.BranchChoice choice) {
        TalentBranches.setBranch(player, getId(), rank, choice);
    }

    /**
     * Clear all branch selections for a player (used when talent is removed)
     */
    default void clearPlayerBranches(ServerPlayer player) {
        TalentBranches.clearBranches(player, getId());
    }

    /**
     * Called when a player upgrades to a new rank
     * Can be overridden to handle branch selection
     */
    default void onRankUp(ServerPlayer player, int newRank) {
        // Default implementation - override in implementations if needed
        if (hasBranchingAtRank(newRank)) {
            // Branch selection would be handled here
            // For now, we'll need a command or UI to select branches
        }
    }

    /**
     * Check if a rank has branching options
     */
    boolean hasBranchingAtRank(int rank);

    /**
     * Get how many branch options are available at a rank (2, 3, or 4)
     */
    default int getBranchCount(int rank) {
        return 2; // Default to 2-way branches
    }

    /**
     * Get the description for a specific branch path
     */
    Component getBranchDescription(int rank, TalentBranches.BranchChoice choice);

    /**
     * Get the name of a branch
     */
    Component getBranchName(int rank, TalentBranches.BranchChoice choice);

    /**
     * Get the effect description for a specific branch
     * This can include specific numbers and scaling
     */
    default Component getBranchEffectDescription(int rank, TalentBranches.BranchChoice choice, int talentLevel) {
        return getBranchDescription(rank, choice);
    }
}