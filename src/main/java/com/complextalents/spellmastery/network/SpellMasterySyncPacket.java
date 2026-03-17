package com.complextalents.spellmastery.network;

import com.complextalents.spellmastery.client.ClientSpellMasteryData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs spell mastery data (mastery levels and learned spells) from server to client.
 */
public class SpellMasterySyncPacket {
    private final CompoundTag data;

    public SpellMasterySyncPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(SpellMasterySyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.data);
    }

    public static SpellMasterySyncPacket decode(FriendlyByteBuf buffer) {
        return new SpellMasterySyncPacket(buffer.readNbt());
    }

    public static void handle(SpellMasterySyncPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientSpellMasteryData.updateData(msg.data);
        });
        context.setPacketHandled(true);
    }
}
