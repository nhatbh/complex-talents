package com.complextalents.network.darkmage;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.client.ClientSoulData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for syncing Dark Mage soul data from server to client.
 * Contains the current soul count (uncapped), Blood Pact active state,
 * and Phylactery cooldown remaining ticks.
 */
public class SoulSyncPacket {
    private final int souls;
    private final boolean bloodPactActive;
    private final long phylacteryCooldownTicks;
    private final long phylacteryTotalCooldownTicks;

    public SoulSyncPacket(int souls, boolean bloodPactActive, long phylacteryCooldownTicks, long phylacteryTotalCooldownTicks) {
        this.souls = souls;
        this.bloodPactActive = bloodPactActive;
        this.phylacteryCooldownTicks = phylacteryCooldownTicks;
        this.phylacteryTotalCooldownTicks = phylacteryTotalCooldownTicks;
    }

    // Decode constructor
    public SoulSyncPacket(FriendlyByteBuf buffer) {
        this.souls = buffer.readVarInt();
        this.bloodPactActive = buffer.readBoolean();
        this.phylacteryCooldownTicks = buffer.readVarLong();
        this.phylacteryTotalCooldownTicks = buffer.readVarLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(souls);
        buffer.writeBoolean(bloodPactActive);
        buffer.writeVarLong(phylacteryCooldownTicks);
        buffer.writeVarLong(phylacteryTotalCooldownTicks);
    }

    public static SoulSyncPacket decode(FriendlyByteBuf buffer) {
        return new SoulSyncPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(this::handleClient);
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        ClientSoulData.setSouls(souls);
        ClientSoulData.setBloodPactActive(bloodPactActive);
        ClientSoulData.setPhylacteryCooldown(phylacteryCooldownTicks, phylacteryTotalCooldownTicks);
        TalentsMod.LOGGER.debug("Received SoulSyncPacket: {} souls, bloodPactActive: {}, phylacteryCooldown: {}/{}",
                souls, bloodPactActive, phylacteryCooldownTicks, phylacteryTotalCooldownTicks);
    }

    public int getSouls() {
        return souls;
    }

    public boolean isBloodPactActive() {
        return bloodPactActive;
    }

    public long getPhylacteryCooldownTicks() {
        return phylacteryCooldownTicks;
    }

    public long getPhylacteryTotalCooldownTicks() {
        return phylacteryTotalCooldownTicks;
    }
}
