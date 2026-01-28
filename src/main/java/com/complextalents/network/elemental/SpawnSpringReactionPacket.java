package com.complextalents.network.elemental;

import com.complextalents.elemental.client.renderers.reactions.SpringFXRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning spring reaction particle effects on the client
 * Sent from server when the spring reaction triggers
 */
public class SpawnSpringReactionPacket {
    private final Vec3 position;

    public SpawnSpringReactionPacket(Vec3 position) {
        this.position = position;
    }

    // Decode constructor
    public SpawnSpringReactionPacket(FriendlyByteBuf buffer) {
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    public static SpawnSpringReactionPacket decode(FriendlyByteBuf buffer) {
        return new SpawnSpringReactionPacket(buffer);
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

        // Render the spring effect
        SpringFXRenderer.render(level, position);
    }
}
