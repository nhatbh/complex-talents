package com.complextalents.targeting.network;

import com.complextalents.targeting.TargetingSnapshot;
import com.complextalents.targeting.server.ServerTargetingHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when a skill is activated.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>The skill ID being activated</li>
 *   <li>The targeting snapshot produced by the client resolver</li>
 * </ul>
 *
 * <p>The server trusts the targeting snapshot and uses it directly
 * for skill execution without re-calculating raycasts.</p>
 */
public class SkillUsePacket {

    private final ResourceLocation skillId;
    private final TargetingSnapshot snapshot;

    /**
     * Create a new skill use packet.
     *
     * @param skillId The ID of the skill being activated
     * @param snapshot The targeting snapshot from the client
     */
    public SkillUsePacket(ResourceLocation skillId, TargetingSnapshot snapshot) {
        this.skillId = skillId;
        this.snapshot = snapshot;
    }

    /**
     * Decode a skill use packet from a buffer.
     *
     * @param buffer The buffer to read from
     * @return A new SkillUsePacket
     */
    public static SkillUsePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation skillId = buffer.readResourceLocation();
        TargetingSnapshot snapshot = TargetingSnapshot.fromNetwork(buffer);
        return new SkillUsePacket(skillId, snapshot);
    }

    /**
     * Encode this packet to a buffer.
     *
     * @param buffer The buffer to write to
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(skillId);
        snapshot.toNetwork(buffer);
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
                // Forward to the server handler for validation and processing
                ServerTargetingHandler.handleSkillUse(player, skillId, snapshot);
            }
        });
        context.get().setPacketHandled(true);
    }

    /**
     * @return The skill ID being activated
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }

    /**
     * @return The targeting snapshot from the client
     */
    public TargetingSnapshot getSnapshot() {
        return snapshot;
    }
}
