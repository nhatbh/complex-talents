package com.complextalents.skill.network;

import com.complextalents.skill.server.SkillTargetingHandler;
import com.complextalents.targeting.TargetingSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when a skill is cast.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>The skill ID being cast</li>
 *   <li>The slot index (0-3 for combat mode hotbar)</li>
 *   <li>The channel time in milliseconds (all skills are channeled, instant skills use 0)</li>
 *   <li>The targeting snapshot from client-side resolution</li>
 * </ul>
 *
 * <p>The server trusts the targeting snapshot and uses it directly
 * for skill execution without re-calculating raycasts.</p>
 *
 * <p>Channel time is validated client-side. Server just receives and uses it.</p>
 */
public class SkillCastPacket {

    private final ResourceLocation skillId;
    private final int slotIndex;
    private final int channelTime; // milliseconds (0 for instant skills)
    private final TargetingSnapshot targetingSnapshot;

    /**
     * Create a new skill cast packet.
     *
     * @param skillId The ID of the skill being cast
     * @param slotIndex The hotbar slot index (0-3)
     * @param channelTime The channel time in milliseconds (0 for instant)
     * @param targetingSnapshot The targeting snapshot from client resolution
     */
    public SkillCastPacket(ResourceLocation skillId, int slotIndex, int channelTime, TargetingSnapshot targetingSnapshot) {
        this.skillId = skillId;
        this.slotIndex = slotIndex;
        this.channelTime = channelTime;
        this.targetingSnapshot = targetingSnapshot;
    }

    /**
     * Decode a skill cast packet from a buffer.
     *
     * @param buffer The buffer to read from
     * @return A new SkillCastPacket
     */
    public static SkillCastPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation skillId = buffer.readResourceLocation();
        int slotIndex = buffer.readVarInt();
        int channelTime = buffer.readVarInt();
        TargetingSnapshot targetingSnapshot = TargetingSnapshot.fromNetwork(buffer);
        return new SkillCastPacket(skillId, slotIndex, channelTime, targetingSnapshot);
    }

    /**
     * Encode this packet to a buffer.
     *
     * @param buffer The buffer to write to
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(skillId);
        buffer.writeVarInt(slotIndex);
        buffer.writeVarInt(channelTime);
        targetingSnapshot.toNetwork(buffer);
    }

    /**
     * Handle this packet on the server side.
     *
     * @param context The network context
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                double channelTimeSeconds = channelTime / 1000.0;

                // DEBUG: Log packet received
                player.sendSystemMessage(Component.literal("§7[DEBUG] §eSkillCastPacket received: " + skillId +
                        " (slot " + slotIndex + ", channel: " + String.format("%.3f", channelTimeSeconds) + "s)"));

                // Log targeting snapshot details
                if (targetingSnapshot.hasEntity()) {
                    player.sendSystemMessage(Component.literal("§7[DEBUG] §aTarget: ENTITY " + targetingSnapshot.getTargetEntityId() + " (ally: " + targetingSnapshot.isAlly() + ")"));
                } else {
                    player.sendSystemMessage(Component.literal("§7[DEBUG] §bTarget: POSITION " + String.format("%.1f, %.1f, %.1f",
                            targetingSnapshot.getTargetPosition().x,
                            targetingSnapshot.getTargetPosition().y,
                            targetingSnapshot.getTargetPosition().z)));
                }

                // Forward to the skill casting handler with channel time
                SkillTargetingHandler.handleSkillCast(player, skillId, slotIndex, targetingSnapshot, channelTimeSeconds);
            }
        });
        context.get().setPacketHandled(true);
    }

    /**
     * @return The skill ID being cast
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }

    /**
     * @return The hotbar slot index (0-3)
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * @return The channel time in milliseconds
     */
    public int getChannelTime() {
        return channelTime;
    }

    /**
     * @return The targeting snapshot from client resolution
     */
    public TargetingSnapshot getTargetingSnapshot() {
        return targetingSnapshot;
    }
}
