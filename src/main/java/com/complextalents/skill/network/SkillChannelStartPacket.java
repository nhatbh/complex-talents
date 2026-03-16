package com.complextalents.skill.network;

import com.complextalents.network.PacketHandler;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.server.SkillCooldownHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when player starts pressing a skill key.
 *
 * <p>The server validates cooldowns and resources and responds with
 * {@link SkillChannelStartResponsePacket} to indicate whether channeling
 * can proceed.</p>
 *
 * <p>This prevents the client from showing "Channeling..." for skills that
 * are on cooldown or lack resources.</p>
 */
public class SkillChannelStartPacket {

    private final ResourceLocation skillId;
    private final int slotIndex;

    /**
     * Create a new channel start request packet.
     *
     * @param skillId The ID of the skill being channeled
     * @param slotIndex The hotbar slot index (0-3)
     */
    public SkillChannelStartPacket(ResourceLocation skillId, int slotIndex) {
        this.skillId = skillId;
        this.slotIndex = slotIndex;
    }

    /**
     * Decode a channel start packet from a buffer.
     *
     * @param buffer The buffer to read from
     * @return A new SkillChannelStartPacket
     */
    public static SkillChannelStartPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation skillId = buffer.readResourceLocation();
        int slotIndex = buffer.readVarInt();
        return new SkillChannelStartPacket(skillId, slotIndex);
    }

    /**
     * Encode this packet to a buffer.
     *
     * @param buffer The buffer to write to
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(skillId);
        buffer.writeVarInt(slotIndex);
    }

    /**
     * Handle this packet on the server side.
     *
     * @param context The network context
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            // Validate the skill cast
            SkillCooldownHandler.ValidationResult result = SkillCooldownHandler.validateSkillCast(player, skillId);

            if (result.isValid()) {
                // Success - allow channeling to start
                PacketHandler.sendTo(new SkillChannelStartResponsePacket(true, skillId, slotIndex, null, null, player.level().getGameTime()), player);
                
                // For CHARGE skills, trigger the start-charge effect
                com.complextalents.skill.server.SkillExecutionHandler.handleChargeStart(player, skillId);
            } else {
                // Failed - check if it's a cooldown failure and get expiration time
                Long cooldownExpiration = null;
                var skillDataOpt = player.getCapability(SkillDataProvider.SKILL_DATA);
                if (skillDataOpt.isPresent()) {
                    var skillData = skillDataOpt.resolve().get();
                    cooldownExpiration = skillData.getCooldownExpiration(skillId);
                }
                // Send error message with cooldown expiration if applicable
                PacketHandler.sendTo(new SkillChannelStartResponsePacket(false, skillId, slotIndex, result.getFailureReason(), cooldownExpiration, player.level().getGameTime()), player);
            }
        });
        context.get().setPacketHandled(true);
    }

    /**
     * @return The skill ID being channeled
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
}
