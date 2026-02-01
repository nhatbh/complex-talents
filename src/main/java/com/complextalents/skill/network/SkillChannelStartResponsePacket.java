package com.complextalents.skill.network;

import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.client.ChannelManager;
import com.complextalents.skill.client.SkillCastingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client in response to {@link SkillChannelStartPacket}.
 *
 * <p>Indicates whether the server allows channeling to start. If successful,
 * the client begins the channeling state. If failed, displays an error message.</p>
 */
public class SkillChannelStartResponsePacket {

    private final boolean success;
    private final ResourceLocation skillId;
    private final int slotIndex;
    private final String failureReason; // null if success
    private final Long cooldownExpiration; // null if success or non-cooldown failure
    private final long currentServerTime; // Server game time for cooldown sync

    /**
     * Create a new channel start response packet.
     *
     * @param success Whether the server approved the channel start
     * @param skillId The skill ID (echoed back from request)
     * @param slotIndex The slot index (echoed back from request)
     * @param failureReason The failure reason, or null if successful
     * @param cooldownExpiration The cooldown expiration time, or null if not a cooldown failure
     * @param currentServerTime The current server game time for cooldown sync
     */
    public SkillChannelStartResponsePacket(boolean success, ResourceLocation skillId, int slotIndex, String failureReason, Long cooldownExpiration, long currentServerTime) {
        this.success = success;
        this.skillId = skillId;
        this.slotIndex = slotIndex;
        this.failureReason = failureReason;
        this.cooldownExpiration = cooldownExpiration;
        this.currentServerTime = currentServerTime;
    }

    /**
     * Decode a channel start response packet from a buffer.
     *
     * @param buffer The buffer to read from
     * @return A new SkillChannelStartResponsePacket
     */
    public static SkillChannelStartResponsePacket decode(FriendlyByteBuf buffer) {
        boolean success = buffer.readBoolean();
        ResourceLocation skillId = buffer.readResourceLocation();
        int slotIndex = buffer.readVarInt();
        String failureReason = success ? null : buffer.readUtf();
        Long cooldownExpiration = null;
        long currentServerTime = 0;
        if (!success) {
            boolean hasCooldown = buffer.readBoolean();
            if (hasCooldown) {
                cooldownExpiration = buffer.readVarLong();
            }
            currentServerTime = buffer.readVarLong();
        }
        return new SkillChannelStartResponsePacket(success, skillId, slotIndex, failureReason, cooldownExpiration, currentServerTime);
    }

    /**
     * Encode this packet to a buffer.
     *
     * @param buffer The buffer to write to
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(success);
        buffer.writeResourceLocation(skillId);
        buffer.writeVarInt(slotIndex);
        if (!success) {
            buffer.writeUtf(failureReason != null ? failureReason : "Unknown error");
            buffer.writeBoolean(cooldownExpiration != null);
            if (cooldownExpiration != null) {
                buffer.writeVarLong(cooldownExpiration);
            }
            buffer.writeVarLong(currentServerTime);
        }
    }

    /**
     * Handle this packet on the client side.
     *
     * @param context The network context
     */
    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            // Clear pending state regardless of result
            ChannelManager.clearPendingChannelStart();

            if (success) {
                // Get the skill to determine max channel time
                Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                if (skill != null) {
                    // Start channeling now that server approved
                    SkillCastingClient.startChanneling(slotIndex, skill.getMaxChannelTime());

                    // Only show channeling message for skills with actual channel time
                    if (skill.getMaxChannelTime() > 0) {
                        mc.player.displayClientMessage(Component.literal("§eChanneling..."), true);
                    }
                }
            } else {
                // Show failure reason
                String message = failureReason != null ? failureReason : "Failed to start channeling";
                mc.player.displayClientMessage(Component.literal("§c" + message), true);

                // Sync cooldown to client if failure was due to cooldown
                if (cooldownExpiration != null) {
                    com.complextalents.skill.client.ClientSkillData.setCooldown(skillId, cooldownExpiration, currentServerTime);
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    /**
     * @return Whether the server approved the channel start
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return The skill ID
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }

    /**
     * @return The slot index
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * @return The failure reason, or null if successful
     */
    public String getFailureReason() {
        return failureReason;
    }
}
