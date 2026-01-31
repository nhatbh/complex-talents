package com.complextalents.network;

import com.complextalents.impl.highpriest.client.renderers.SeraphSwordFXRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Network packet for spawning Seraph's Bouncing Sword particle and sound effects on the client.
 * Supports three effect types: flight trail, terrain impact, and entity hit.
 * <p>
 * Sent from server when the sword performs actions that need visual/audio feedback.
 */
public class SpawnSeraphSwordFXPacket {
    private final Vec3 position;
    private final Vec3 velocity; // Used for flight trail direction, null for collision effects
    private final int effectType; // 0 = flight trail, 1 = terrain hit, 2 = entity hit

    /**
     * Create a new FX packet.
     *
     * @param position The position where the effect should occur
     * @param velocity The velocity (for flight trail direction, null for collisions)
     * @param effectType The type of effect (0=trail, 1=terrain, 2=entity)
     */
    public SpawnSeraphSwordFXPacket(Vec3 position, @Nullable Vec3 velocity, int effectType) {
        this.position = position;
        this.velocity = velocity;
        this.effectType = effectType;
    }

    /**
     * Decode constructor for network deserialization.
     */
    public SpawnSeraphSwordFXPacket(FriendlyByteBuf buffer) {
        this.position = new Vec3(
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble()
        );

        // Read velocity (may be null for collision effects)
        boolean hasVelocity = buffer.readBoolean();
        if (hasVelocity) {
            this.velocity = new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
            );
        } else {
            this.velocity = null;
        }

        this.effectType = buffer.readVarInt();
    }

    /**
     * Encode this packet to the network buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);

        // Write velocity (may be null for collision effects)
        buffer.writeBoolean(velocity != null);
        if (velocity != null) {
            buffer.writeDouble(velocity.x);
            buffer.writeDouble(velocity.y);
            buffer.writeDouble(velocity.z);
        }

        buffer.writeVarInt(effectType);
    }

    /**
     * Static decode method for network handler registration.
     */
    public static SpawnSeraphSwordFXPacket decode(FriendlyByteBuf buffer) {
        return new SpawnSeraphSwordFXPacket(buffer);
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
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        // Route to the appropriate effect renderer
        SeraphSwordFXRenderer.render(level, position, velocity, effectType);
    }
}
