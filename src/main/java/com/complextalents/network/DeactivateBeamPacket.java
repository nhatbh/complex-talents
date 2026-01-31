package com.complextalents.network;

import com.complextalents.impl.highpriest.client.HolyBeamClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet to deactivate and fade out a holy beam.
 * <p>
 * Contains only UUIDs - entity IDs are resolved lazily on the client.
 * Sent from server when Covenant of Protection expires or is broken.
 */
public class DeactivateBeamPacket {

    private final UUID sourceUUID;
    private final UUID targetUUID;

    /**
     * Create a new DeactivateBeamPacket.
     *
     * @param sourceUUID The UUID of the source entity (caster)
     * @param targetUUID The UUID of the target entity
     */
    public DeactivateBeamPacket(UUID sourceUUID, UUID targetUUID) {
        this.sourceUUID = sourceUUID;
        this.targetUUID = targetUUID;
    }

    /**
     * Decode constructor for network deserialization.
     */
    public DeactivateBeamPacket(FriendlyByteBuf buffer) {
        this.sourceUUID = buffer.readUUID();
        this.targetUUID = buffer.readUUID();
    }

    /**
     * Encode this packet to the network buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(sourceUUID);
        buffer.writeUUID(targetUUID);
    }

    /**
     * Static decode method for network handler registration.
     */
    public static DeactivateBeamPacket decode(FriendlyByteBuf buffer) {
        return new DeactivateBeamPacket(buffer);
    }

    /**
     * Handle this packet on the network thread.
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(this::handleClient);
        context.setPacketHandled(true);
    }

    /**
     * Handle the packet on the client side.
     */
    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        HolyBeamClientData.handleDeactivate(sourceUUID, targetUUID);
    }

    // Getters
    public UUID getSourceUUID() {
        return sourceUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }
}
