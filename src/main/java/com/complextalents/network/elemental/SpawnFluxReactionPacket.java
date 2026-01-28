package com.complextalents.network.elemental;

import com.complextalents.elemental.client.renderers.reactions.FluxFXRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning flux reaction particle effects on the client
 * Sent from server when the flux reaction triggers
 */
public class SpawnFluxReactionPacket {
    private final Vec3 position;

    public SpawnFluxReactionPacket(Vec3 position) {
        this.position = position;
    }

    // Decode constructor
    public SpawnFluxReactionPacket(FriendlyByteBuf buffer) {
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    public static SpawnFluxReactionPacket decode(FriendlyByteBuf buffer) {
        return new SpawnFluxReactionPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        // Render the flux effect
        FluxFXRenderer.render(level, position);
    }
}
