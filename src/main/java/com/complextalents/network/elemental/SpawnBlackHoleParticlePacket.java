package com.complextalents.network.elemental;

import com.complextalents.elemental.client.renderers.entities.BlackHoleFXRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning black hole particle effects on the client
 * Sent from server every tick the Black Hole entity exists
 */
public class SpawnBlackHoleParticlePacket {
    private final Vec3 position;
    private final boolean isImploding;

    public SpawnBlackHoleParticlePacket(Vec3 position, boolean isImploding) {
        this.position = position;
        this.isImploding = isImploding;
    }

    // Decode constructor
    public SpawnBlackHoleParticlePacket(FriendlyByteBuf buffer) {
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.isImploding = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
        buffer.writeBoolean(isImploding);
    }

    public static SpawnBlackHoleParticlePacket decode(FriendlyByteBuf buffer) {
        return new SpawnBlackHoleParticlePacket(buffer);
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

        // Render the black hole particle effect
        BlackHoleFXRenderer.render(level, position, isImploding);
    }
}
