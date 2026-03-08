package com.complextalents.network.assassin;

import com.complextalents.impl.assassin.client.ClientAssassinData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sync Assassin-specific data (stealth gauge) from server to client.
 */
public class AssassinSyncPacket {
    private final double gauge;
    private final double maxGauge;

    public AssassinSyncPacket(double gauge, double maxGauge) {
        this.gauge = gauge;
        this.maxGauge = maxGauge;
    }

    public AssassinSyncPacket(FriendlyByteBuf buffer) {
        this.gauge = buffer.readDouble();
        this.maxGauge = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(gauge);
        buffer.writeDouble(maxGauge);
    }

    public static AssassinSyncPacket decode(FriendlyByteBuf buffer) {
        return new AssassinSyncPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(this::handleClient);
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        ClientAssassinData.setData(gauge, maxGauge);
    }
}
