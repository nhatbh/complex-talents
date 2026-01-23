package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.talent.TalentBranches;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to synchronize branch selections from server to client
 */
public class SyncBranchSelectionPacket {

    private final UUID playerUUID;
    private final ResourceLocation talentId;
    private final int rank;
    private final TalentBranches.BranchChoice choice;

    public SyncBranchSelectionPacket(UUID playerUUID, ResourceLocation talentId, int rank, TalentBranches.BranchChoice choice) {
        this.playerUUID = playerUUID;
        this.talentId = talentId;
        this.rank = rank;
        this.choice = choice;
    }

    public SyncBranchSelectionPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.talentId = buf.readResourceLocation();
        this.rank = buf.readInt();
        this.choice = TalentBranches.BranchChoice.fromId(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeResourceLocation(talentId);
        buf.writeInt(rank);
        buf.writeInt(choice.getId());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> handleClient());
        }

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        // Update client-side branch storage
        TalentBranches.setClientBranch(playerUUID, talentId, rank, choice);

        TalentsMod.LOGGER.debug("Received branch sync for player {} talent {} rank {} -> {}",
            playerUUID, talentId, rank, choice);
    }
}

/**
 * Packet to sync all branch selections for a player
 */
class SyncAllBranchesPacket {

    private final UUID playerUUID;
    private final CompoundTag branchData;

    public SyncAllBranchesPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.branchData = TalentBranches.saveToNBT(playerUUID);
    }

    public SyncAllBranchesPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.branchData = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeNbt(branchData);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> handleClient());
        }

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        // Load all branch data on client
        TalentBranches.loadFromNBT(playerUUID, branchData);

        TalentsMod.LOGGER.debug("Synced all branches for player {}", playerUUID);
    }
}