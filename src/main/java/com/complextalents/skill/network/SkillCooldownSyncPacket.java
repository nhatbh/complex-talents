package com.complextalents.skill.network;

import com.complextalents.skill.client.ClientSkillData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sync cooldown data from server to client.
 * Sent when cooldowns change.
 */
public class SkillCooldownSyncPacket {

    private final Map<ResourceLocation, Long> cooldowns;
    private final long currentGameTime;

    public SkillCooldownSyncPacket(Map<ResourceLocation, Long> cooldowns, long currentGameTime) {
        this.cooldowns = cooldowns != null ? cooldowns : new HashMap<>();
        this.currentGameTime = currentGameTime;
    }

    public static SkillCooldownSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, Long> cooldowns = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation skillId = buffer.readResourceLocation();
            long expiration = buffer.readVarLong();
            cooldowns.put(skillId, expiration);
        }
        long gameTime = buffer.readVarLong();
        return new SkillCooldownSyncPacket(cooldowns, gameTime);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(cooldowns.size());
        for (Map.Entry<ResourceLocation, Long> entry : cooldowns.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeVarLong(entry.getValue());
        }
        buffer.writeVarLong(currentGameTime);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientSkillData.syncCooldowns(cooldowns, currentGameTime);
        });
        context.get().setPacketHandled(true);
    }
}
