package com.complextalents.leveling.network;

import com.complextalents.leveling.client.ClientLevelingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs player leveling data from server to client for HUD rendering.
 */
public class LevelDataSyncPacket {
    private final int level;
    private final double currentXP;
    private final double xpForNext;
    private final double chunkFatigue;
    private final int availableSkillPoints;

    public LevelDataSyncPacket(int level, double currentXP, double xpForNext, double chunkFatigue, int availableSkillPoints) {
        this.level = level;
        this.currentXP = currentXP;
        this.xpForNext = xpForNext;
        this.chunkFatigue = chunkFatigue;
        this.availableSkillPoints = availableSkillPoints;
    }

    public static void encode(LevelDataSyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.level);
        buffer.writeDouble(msg.currentXP);
        buffer.writeDouble(msg.xpForNext);
        buffer.writeDouble(msg.chunkFatigue);
        buffer.writeInt(msg.availableSkillPoints);
    }

    public static LevelDataSyncPacket decode(FriendlyByteBuf buffer) {
        return new LevelDataSyncPacket(
            buffer.readInt(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readInt()
        );
    }

    public static void handle(LevelDataSyncPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientLevelingData.setLevel(msg.level);
            ClientLevelingData.setCurrentXP(msg.currentXP);
            ClientLevelingData.setXpForNext(msg.xpForNext);
            ClientLevelingData.setChunkFatigue(msg.chunkFatigue);
            ClientLevelingData.setAvailableSkillPoints(msg.availableSkillPoints);
        });
        context.setPacketHandled(true);
    }
}
