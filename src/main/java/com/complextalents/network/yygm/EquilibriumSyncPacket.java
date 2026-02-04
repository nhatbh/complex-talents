package com.complextalents.network.yygm;

import com.complextalents.TalentsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for syncing YYGM Equilibrium stacks from server to client.
 * Contains the current equilibrium count (0-8) and last hit time for timer display.
 */
public class EquilibriumSyncPacket {
    private final int equilibrium;
    private final long lastHitTime;

    public EquilibriumSyncPacket(int equilibrium, long lastHitTime) {
        this.equilibrium = equilibrium;
        this.lastHitTime = lastHitTime;
    }

    // Decode constructor
    public EquilibriumSyncPacket(FriendlyByteBuf buffer) {
        this.equilibrium = buffer.readInt();
        this.lastHitTime = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(equilibrium);
        buffer.writeLong(lastHitTime);
    }

    public static EquilibriumSyncPacket decode(FriendlyByteBuf buffer) {
        return new EquilibriumSyncPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        ClientEquilibriumData.setEquilibrium(equilibrium, lastHitTime);
        TalentsMod.LOGGER.debug("Received EquilibriumSyncPacket: {} stacks, lastHitTime: {}", equilibrium, lastHitTime);
    }

    public int getEquilibrium() {
        return equilibrium;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }
}
