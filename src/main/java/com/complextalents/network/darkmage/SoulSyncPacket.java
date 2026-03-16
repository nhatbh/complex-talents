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
 * Phylactery cooldown remaining ticks, and Blood Pact combat stats.
 */
public class SoulSyncPacket {
    private final double souls;
    private final boolean bloodPactActive;
    private final long phylacteryCooldownTicks;
    private final long phylacteryTotalCooldownTicks;
    private final float spellPower;
    private final float critChance;
    private final float critDamage;
    private final float drainMultiplier;
    private final float soulMultiplier;

    public SoulSyncPacket(double souls, boolean bloodPactActive, long phylacteryCooldownTicks, long phylacteryTotalCooldownTicks,
                          float spellPower, float critChance, float critDamage, float drainMultiplier, float soulMultiplier) {
        this.souls = souls;
        this.bloodPactActive = bloodPactActive;
        this.phylacteryCooldownTicks = phylacteryCooldownTicks;
        this.phylacteryTotalCooldownTicks = phylacteryTotalCooldownTicks;
        this.spellPower = spellPower;
        this.critChance = critChance;
        this.critDamage = critDamage;
        this.drainMultiplier = drainMultiplier;
        this.soulMultiplier = soulMultiplier;
    }

    // Decode constructor
    public SoulSyncPacket(FriendlyByteBuf buffer) {
        this.souls = buffer.readDouble();
        this.bloodPactActive = buffer.readBoolean();
        this.phylacteryCooldownTicks = buffer.readVarLong();
        this.phylacteryTotalCooldownTicks = buffer.readVarLong();
        this.spellPower = buffer.readFloat();
        this.critChance = buffer.readFloat();
        this.critDamage = buffer.readFloat();
        this.drainMultiplier = buffer.readFloat();
        this.soulMultiplier = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(souls);
        buffer.writeBoolean(bloodPactActive);
        buffer.writeVarLong(phylacteryCooldownTicks);
        buffer.writeVarLong(phylacteryTotalCooldownTicks);
        buffer.writeFloat(spellPower);
        buffer.writeFloat(critChance);
        buffer.writeFloat(critDamage);
        buffer.writeFloat(drainMultiplier);
        buffer.writeFloat(soulMultiplier);
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
        ClientSoulData.setBloodPactStats(spellPower, critChance, critDamage);
        ClientSoulData.setBloodPactMultipliers(drainMultiplier, soulMultiplier);
        TalentsMod.LOGGER.debug("Received SoulSyncPacket: {} souls, bloodPactActive: {}, multipliers: {}/{}",
                souls, bloodPactActive, drainMultiplier, soulMultiplier);
    }

    public double getSouls() {
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
