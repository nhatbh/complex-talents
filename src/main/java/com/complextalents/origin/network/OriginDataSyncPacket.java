package com.complextalents.origin.network;

import com.complextalents.origin.client.ClientOriginData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sync player origin data from server to client.
 * Includes active origin, origin level, and resource value for HUD rendering.
 */
public class OriginDataSyncPacket {

    private final UUID playerUuid;
    private final ResourceLocation originId;
    private final int originLevel;
    private final double resourceValue;
    private final double resourceMax;
    private final ResourceLocation resourceTypeId;

    public OriginDataSyncPacket(UUID playerUuid, ResourceLocation originId, int originLevel,
                                  double resourceValue, double resourceMax, ResourceLocation resourceTypeId) {
        this.playerUuid = playerUuid;
        this.originId = originId;
        this.originLevel = originLevel;
        this.resourceValue = resourceValue;
        this.resourceMax = resourceMax;
        this.resourceTypeId = resourceTypeId;
    }

    /**
     * Decode an origin data sync packet from a buffer.
     */
    public static OriginDataSyncPacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();

        boolean hasOrigin = buffer.readBoolean();
        ResourceLocation originId = null;
        int originLevel = 1;
        double resourceValue = 0;
        double resourceMax = 0;
        ResourceLocation resourceTypeId = null;

        if (hasOrigin) {
            originId = buffer.readResourceLocation();
            originLevel = buffer.readVarInt();
            resourceValue = buffer.readDouble();
            resourceMax = buffer.readDouble();

            boolean hasResourceType = buffer.readBoolean();
            if (hasResourceType) {
                resourceTypeId = buffer.readResourceLocation();
            }
        }

        return new OriginDataSyncPacket(uuid, originId, originLevel, resourceValue, resourceMax, resourceTypeId);
    }

    /**
     * Encode this packet to a buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUuid);

        boolean hasOrigin = originId != null;
        buffer.writeBoolean(hasOrigin);

        if (hasOrigin) {
            buffer.writeResourceLocation(originId);
            buffer.writeVarInt(originLevel);
            buffer.writeDouble(resourceValue);
            buffer.writeDouble(resourceMax);

            boolean hasResourceType = resourceTypeId != null;
            buffer.writeBoolean(hasResourceType);
            if (hasResourceType) {
                buffer.writeResourceLocation(resourceTypeId);
            }
        }
    }

    /**
     * Handle this packet on the client side.
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side data
            ClientOriginData.syncFromServer(originId, originLevel, resourceValue, resourceMax, resourceTypeId);
        });
        context.get().setPacketHandled(true);
    }

    /**
     * Helper method to send this packet.
     */
    public static void send(net.minecraft.server.level.ServerPlayer player,
                            ResourceLocation originId,
                            int originLevel,
                            double resourceValue,
                            double resourceMax,
                            ResourceLocation resourceTypeId) {
        OriginDataSyncPacket packet = new OriginDataSyncPacket(
                player.getUUID(),
                originId,
                originLevel,
                resourceValue,
                resourceMax,
                resourceTypeId
        );
        com.complextalents.network.PacketHandler.sendTo(packet, player);
    }

    /**
     * @return The player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * @return The origin ID
     */
    public ResourceLocation getOriginId() {
        return originId;
    }

    /**
     * @return The origin level
     */
    public int getOriginLevel() {
        return originLevel;
    }

    /**
     * @return The current resource value
     */
    public double getResourceValue() {
        return resourceValue;
    }

    /**
     * @return The maximum resource value
     */
    public double getResourceMax() {
        return resourceMax;
    }

    /**
     * @return The resource type ID
     */
    public ResourceLocation getResourceTypeId() {
        return resourceTypeId;
    }
}
