package com.complextalents.passive.network;

import com.complextalents.passive.client.ClientPassiveStackData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sync packet for passive stack data from server to client.
 * Includes all passive stack counts for the player.
 */
public class PassiveStackSyncPacket {

    private final UUID playerUuid;
    private final Map<String, Integer> stacks;

    /**
     * Create a new passive stack sync packet.
     *
     * @param playerUuid The UUID of the player
     * @param stacks Map of stack type name to count
     */
    public PassiveStackSyncPacket(UUID playerUuid, Map<String, Integer> stacks) {
        this.playerUuid = playerUuid;
        this.stacks = stacks;
    }

    /**
     * Decode a passive stack sync packet from a buffer.
     *
     * @param buffer The buffer to read from
     * @return The decoded packet
     */
    public static PassiveStackSyncPacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();

        int size = buffer.readVarInt();
        Map<String, Integer> stacks = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buffer.readUtf(32767);
            int value = buffer.readVarInt();
            stacks.put(key, value);
        }

        return new PassiveStackSyncPacket(uuid, stacks);
    }

    /**
     * Encode this packet to a buffer.
     *
     * @param buffer The buffer to write to
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUuid);

        buffer.writeVarInt(stacks.size());
        for (Map.Entry<String, Integer> entry : stacks.entrySet()) {
            buffer.writeUtf(entry.getKey(), 32767);
            buffer.writeVarInt(entry.getValue());
        }
    }

    /**
     * Handle this packet on the client side.
     *
     * @param context The network event context
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side cache
            ClientPassiveStackData.syncFromServer(stacks);
        });
        context.get().setPacketHandled(true);
    }

    /**
     * Helper method to send this packet to a player.
     *
     * @param player The server player to send to
     * @param stacks The stack data to send
     */
    public static void send(net.minecraft.server.level.ServerPlayer player, Map<String, Integer> stacks) {
        PassiveStackSyncPacket packet = new PassiveStackSyncPacket(player.getUUID(), stacks);
        com.complextalents.network.PacketHandler.sendTo(packet, player);
    }
}
