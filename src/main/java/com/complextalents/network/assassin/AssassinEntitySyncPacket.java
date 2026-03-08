package com.complextalents.network.assassin;

import com.complextalents.impl.assassin.client.ClientAssassinData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sync per-entity backstab cooldown data from server to client.
 */
public class AssassinEntitySyncPacket {
    private final int entityId;
    private final long startTime;
    private final long expirationTime;

    public AssassinEntitySyncPacket(int entityId, long startTime, long expirationTime) {
        this.entityId = entityId;
        this.startTime = startTime;
        this.expirationTime = expirationTime;
    }

    public AssassinEntitySyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.startTime = buffer.readLong();
        this.expirationTime = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeLong(startTime);
        buffer.writeLong(expirationTime);
    }

    public static AssassinEntitySyncPacket decode(FriendlyByteBuf buffer) {
        return new AssassinEntitySyncPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(this::handleClient);
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        ClientAssassinData.setEntityCooldown(entityId, startTime, expirationTime);
    }
}
