package com.complextalents.skill.network;

import com.complextalents.skill.client.ClientSkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sync player skill data from server to client.
 * Includes slot assignments for client-side display/input.
 */
public class SkillDataSyncPacket {

    private final UUID playerUuid;
    private final ResourceLocation[] skillSlots;

    public SkillDataSyncPacket(UUID playerUuid, ResourceLocation[] skillSlots) {
        this.playerUuid = playerUuid;
        this.skillSlots = skillSlots;
    }

    /**
     * Decode a skill data sync packet from a buffer.
     */
    public static SkillDataSyncPacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        ResourceLocation[] slots = new ResourceLocation[4];
        for (int i = 0; i < 4; i++) {
            boolean hasSkill = buffer.readBoolean();
            if (hasSkill) {
                slots[i] = buffer.readResourceLocation();
            } else {
                slots[i] = null;
            }
        }
        return new SkillDataSyncPacket(uuid, slots);
    }

    /**
     * Encode this packet to a buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUuid);
        for (int i = 0; i < 4; i++) {
            boolean hasSkill = skillSlots[i] != null;
            buffer.writeBoolean(hasSkill);
            if (hasSkill) {
                buffer.writeResourceLocation(skillSlots[i]);
            }
        }
    }

    /**
     * Handle this packet on the client side.
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side data
            ClientSkillData.syncFromServer(skillSlots);
        });
        context.get().setPacketHandled(true);
    }

    /**
     * @return The player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * @return The skill slots array
     */
    public ResourceLocation[] getSkillSlots() {
        return skillSlots;
    }
}
