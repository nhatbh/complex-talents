package com.complextalents.network.highpriest;

import com.complextalents.impl.highpriest.client.ClientFaithData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs Faith data from server to client.
 */
public class FaithSyncPacket {
    private final double faith;

    public FaithSyncPacket(double faith) {
        this.faith = faith;
    }

    public FaithSyncPacket(FriendlyByteBuf buf) {
        this.faith = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(faith);
    }

    public static FaithSyncPacket decode(FriendlyByteBuf buf) {
        return new FaithSyncPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Handle on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientFaithData.setFaith(faith);
            });
        });
    }
}
