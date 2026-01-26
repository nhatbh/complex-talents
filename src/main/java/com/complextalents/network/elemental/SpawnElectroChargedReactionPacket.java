package com.complextalents.network.elemental;

import com.complextalents.elemental.client.renderers.reactions.ElectroChargedFXRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning electro-charged chain lightning particle effects on the client
 * Sent from server when the electro-charged effect zaps nearby enemies
 */
public class SpawnElectroChargedReactionPacket {
    private final Vec3 sourcePosition;
    private final Vec3 targetPosition;

    public SpawnElectroChargedReactionPacket(Vec3 sourcePosition, Vec3 targetPosition) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
    }

    // Decode constructor
    public SpawnElectroChargedReactionPacket(FriendlyByteBuf buffer) {
        this.sourcePosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.targetPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(sourcePosition.x);
        buffer.writeDouble(sourcePosition.y);
        buffer.writeDouble(sourcePosition.z);
        buffer.writeDouble(targetPosition.x);
        buffer.writeDouble(targetPosition.y);
        buffer.writeDouble(targetPosition.z);
    }

    public static SpawnElectroChargedReactionPacket decode(FriendlyByteBuf buffer) {
        return new SpawnElectroChargedReactionPacket(buffer);
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

        // Render the chain lightning effect
        ElectroChargedFXRenderer.renderChain(level, sourcePosition, targetPosition);
    }
}
