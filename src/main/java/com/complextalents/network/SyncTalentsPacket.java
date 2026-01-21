package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.capability.PlayerTalentsImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTalentsPacket {
    private final CompoundTag data;

    public SyncTalentsPacket(CompoundTag data) {
        this.data = data;
    }

    public SyncTalentsPacket(FriendlyByteBuf buffer) {
        this.data = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(data);
    }

    public static SyncTalentsPacket decode(FriendlyByteBuf buffer) {
        return new SyncTalentsPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraft.client.player.LocalPlayer player = TalentsMod.LOGGER.isDebugEnabled()
                    ? null
                    : net.minecraft.client.Minecraft.getInstance().player;

            if (player != null) {
                player.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                    if (talents instanceof PlayerTalentsImpl impl) {
                        impl.deserializeNBT(data);
                        TalentsMod.LOGGER.debug("Received talent sync data from server");
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}
