package com.complextalents.network;

import com.complextalents.impl.highpriest.client.HolyBeamClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet to activate/create a holy beam between two entities.
 * <p>
 * Contains only UUIDs - entity IDs are resolved lazily on the client.
 * Sent from server when Covenant of Protection is activated.
 */
public class ActivateBeamPacket {

    private final UUID sourceUUID;
    private final UUID targetUUID;
    private final double range;

    /**
     * Create a new ActivateBeamPacket.
     *
     * @param sourceUUID The UUID of the source entity (caster)
     * @param targetUUID The UUID of the target entity
     * @param range The maximum range for this tether
     */
    public ActivateBeamPacket(UUID sourceUUID, UUID targetUUID, double range) {
        this.sourceUUID = sourceUUID;
        this.targetUUID = targetUUID;
        this.range = range;
    }

    /**
     * Decode constructor for network deserialization.
     */
    public ActivateBeamPacket(FriendlyByteBuf buffer) {
        this.sourceUUID = buffer.readUUID();
        this.targetUUID = buffer.readUUID();
        this.range = buffer.readDouble();
    }

    /**
     * Encode this packet to the network buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(sourceUUID);
        buffer.writeUUID(targetUUID);
        buffer.writeDouble(range);
    }

    /**
     * Static decode method for network handler registration.
     */
    public static ActivateBeamPacket decode(FriendlyByteBuf buffer) {
        return new ActivateBeamPacket(buffer);
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
        HolyBeamClientData.handleActivate(sourceUUID, targetUUID, range);
    }

    // Getters
    public UUID getSourceUUID() {
        return sourceUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }
}
