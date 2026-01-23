package com.complextalents.talent;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SyncBranchSelectionPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages branch selections for ALL talents in the game
 * Provides a universal branching system with persistence and synchronization
 */
public class TalentBranches {

    // Store branch selections: PlayerUUID -> TalentID -> Rank -> Branch
    private static final Map<UUID, Map<ResourceLocation, Map<Integer, BranchChoice>>> playerBranches = new HashMap<>();

    public enum BranchChoice {
        NONE(0, "None"),
        PATH_A(1, "Path A"),
        PATH_B(2, "Path B"),
        PATH_C(3, "Path C"), // For potential 3-way branches
        PATH_D(4, "Path D"); // For potential 4-way branches

        private final int id;
        private final String displayName;

        BranchChoice(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static BranchChoice fromId(int id) {
            for (BranchChoice choice : values()) {
                if (choice.id == id) {
                    return choice;
                }
            }
            return NONE;
        }
    }

    /**
     * Set a branch choice for a player's talent at a specific rank
     */
    public static void setBranch(ServerPlayer player, ResourceLocation talentId, int rank, BranchChoice choice) {
        playerBranches
            .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .computeIfAbsent(talentId, k -> new HashMap<>())
            .put(rank, choice);

        TalentsMod.LOGGER.debug("Player {} selected branch {} for talent {} at rank {}",
            player.getName().getString(), choice.getDisplayName(), talentId, rank);

        // Sync to client
        PacketHandler.sendTo(new SyncBranchSelectionPacket(player.getUUID(), talentId, rank, choice), player);
    }

    /**
     * Client-side method to update branch storage from sync packet
     * Only called on client when receiving branch data from server
     */
    public static void setClientBranch(UUID playerUUID, ResourceLocation talentId, int rank, BranchChoice choice) {
        playerBranches
            .computeIfAbsent(playerUUID, k -> new HashMap<>())
            .computeIfAbsent(talentId, k -> new HashMap<>())
            .put(rank, choice);
    }

    /**
     * Get a player's branch choice for a talent at a specific rank
     */
    public static BranchChoice getBranch(UUID playerUUID, ResourceLocation talentId, int rank) {
        return playerBranches
            .getOrDefault(playerUUID, new HashMap<>())
            .getOrDefault(talentId, new HashMap<>())
            .getOrDefault(rank, BranchChoice.NONE);
    }

    /**
     * Get a player's branch choice for a talent at a specific rank
     */
    public static BranchChoice getBranch(ServerPlayer player, ResourceLocation talentId, int rank) {
        return getBranch(player.getUUID(), talentId, rank);
    }

    /**
     * Check if player has selected a specific branch
     */
    public static boolean hasBranch(ServerPlayer player, ResourceLocation talentId, int rank, BranchChoice choice) {
        return getBranch(player, talentId, rank) == choice;
    }

    /**
     * Check if player has any branch selected at a rank (not NONE)
     */
    public static boolean hasAnyBranch(ServerPlayer player, ResourceLocation talentId, int rank) {
        return getBranch(player, talentId, rank) != BranchChoice.NONE;
    }

    /**
     * Clear all branch selections for a player's talent
     */
    public static void clearBranches(ServerPlayer player, ResourceLocation talentId) {
        Map<ResourceLocation, Map<Integer, BranchChoice>> talentMap = playerBranches.get(player.getUUID());
        if (talentMap != null) {
            talentMap.remove(talentId);
        }
    }

    /**
     * Clear all branch selections for a player
     */
    public static void clearAllBranches(UUID playerUUID) {
        playerBranches.remove(playerUUID);
    }

    /**
     * Save branch data to NBT
     */
    public static CompoundTag saveToNBT(UUID playerUUID) {
        CompoundTag tag = new CompoundTag();
        Map<ResourceLocation, Map<Integer, BranchChoice>> talentMap = playerBranches.get(playerUUID);

        if (talentMap != null) {
            CompoundTag talentsTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Map<Integer, BranchChoice>> talentEntry : talentMap.entrySet()) {
                CompoundTag rankTag = new CompoundTag();
                for (Map.Entry<Integer, BranchChoice> rankEntry : talentEntry.getValue().entrySet()) {
                    rankTag.putInt("rank_" + rankEntry.getKey(), rankEntry.getValue().getId());
                }
                talentsTag.put(talentEntry.getKey().toString(), rankTag);
            }
            tag.put("branches", talentsTag);
        }

        return tag;
    }

    /**
     * Load branch data from NBT
     */
    public static void loadFromNBT(UUID playerUUID, CompoundTag tag) {
        if (tag.contains("branches")) {
            CompoundTag talentsTag = tag.getCompound("branches");
            Map<ResourceLocation, Map<Integer, BranchChoice>> talentMap = new HashMap<>();

            for (String talentKey : talentsTag.getAllKeys()) {
                ResourceLocation talentId = ResourceLocation.tryParse(talentKey);
                if (talentId != null) {
                    CompoundTag rankTag = talentsTag.getCompound(talentKey);
                    Map<Integer, BranchChoice> rankMap = new HashMap<>();

                    for (String rankKey : rankTag.getAllKeys()) {
                        if (rankKey.startsWith("rank_")) {
                            try {
                                int rank = Integer.parseInt(rankKey.substring(5));
                                BranchChoice choice = BranchChoice.fromId(rankTag.getInt(rankKey));
                                rankMap.put(rank, choice);
                            } catch (NumberFormatException e) {
                                TalentsMod.LOGGER.warn("Invalid rank key in branch data: {}", rankKey);
                            }
                        }
                    }

                    if (!rankMap.isEmpty()) {
                        talentMap.put(talentId, rankMap);
                    }
                }
            }

            if (!talentMap.isEmpty()) {
                playerBranches.put(playerUUID, talentMap);
            }
        }
    }

    /**
     * Get all branches for a specific talent
     */
    public static Map<Integer, BranchChoice> getTalentBranches(ServerPlayer player, ResourceLocation talentId) {
        return playerBranches
            .getOrDefault(player.getUUID(), new HashMap<>())
            .getOrDefault(talentId, new HashMap<>());
    }

    /**
     * Copy branches from one player to another (useful for testing)
     */
    public static void copyBranches(UUID fromPlayer, UUID toPlayer) {
        Map<ResourceLocation, Map<Integer, BranchChoice>> fromBranches = playerBranches.get(fromPlayer);
        if (fromBranches != null) {
            playerBranches.put(toPlayer, new HashMap<>(fromBranches));
        }
    }

    /**
     * Get a formatted string of all branches for debug/display
     */
    public static String getFormattedBranches(ServerPlayer player, ResourceLocation talentId) {
        Map<Integer, BranchChoice> branches = getTalentBranches(player, talentId);
        if (branches.isEmpty()) {
            return "No branches selected";
        }

        StringBuilder sb = new StringBuilder();
        branches.forEach((rank, choice) -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Rank ").append(rank).append(": ").append(choice.getDisplayName());
        });
        return sb.toString();
    }
}