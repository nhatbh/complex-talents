package com.complextalents.network.elemental;

import com.complextalents.elemental.client.renderers.reactions.VaporizeFXRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning vaporize reaction particle effects on the client
 * Sent from server when the vaporize reaction triggers
 */
public class SpawnVaporizeReactionPacket {
    private final Vec3 position;

    public SpawnVaporizeReactionPacket(Vec3 position) {
        this.position = position;
    }

    // Decode constructor
    public SpawnVaporizeReactionPacket(FriendlyByteBuf buffer) {
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    public static SpawnVaporizeReactionPacket decode(FriendlyByteBuf buffer) {
        return new SpawnVaporizeReactionPacket(buffer);
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

        // Render the vaporize effect
        VaporizeFXRenderer.renderParticles(level, position);
    }
}
