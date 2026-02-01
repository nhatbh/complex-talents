package com.complextalents.network.highpriest;

import com.complextalents.impl.highpriest.client.renderers.BarrierFXRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Network packet for spawning Sanctuary Barrier particle and sound effects on the client.
 * Supports four effect types: creation, destruction, entity/projectile hit, and ambient.
 * <p>
 * Sent from server when the barrier performs actions that need visual/audio feedback.
 */
public class SpawnBarrierFXPacket {
    private final double x;
    private final double y;
    private final double z;
    private final EffectType effectType;
    private final float radius;
    @Nullable
    private final Integer barrierId;

    /**
     * Effect types for Sanctuary Barrier visual/audio feedback.
     */
    public enum EffectType {
        CREATED,      // Barrier created - explosion of end rod particles + charge sound
        DESTROYED,    // Barrier destroyed - falling particles + deplete sound
        ENTITY_HIT,   // Entity/projectile hit barrier - flash particle + clang sound
        AMBIENT       // Ambient sound while barrier exists
    }

    /**
     * Create a new FX packet.
     *
     * @param x          The x position where the effect should occur
     * @param y          The y position where the effect should occur
     * @param z          The z position where the effect should occur
     * @param effectType The type of effect to render
     * @param radius     The barrier radius (for destruction effect)
     * @param barrierId  The barrier entity ID (for tracking, can be null)
     */
    public SpawnBarrierFXPacket(double x, double y, double z, EffectType effectType, float radius, @Nullable Integer barrierId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.effectType = effectType;
        this.radius = radius;
        this.barrierId = barrierId;
    }

    /**
     * Decode constructor for network deserialization.
     */
    public SpawnBarrierFXPacket(FriendlyByteBuf buffer) {
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
        this.effectType = EffectType.values()[buffer.readByte()];
        this.radius = buffer.readFloat();
        this.barrierId = buffer.readBoolean() ? buffer.readInt() : null;
    }

    /**
     * Encode this packet to the network buffer.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeByte(effectType.ordinal());
        buffer.writeFloat(radius);
        buffer.writeBoolean(barrierId != null);
        if (barrierId != null) {
            buffer.writeInt(barrierId);
        }
    }

    /**
     * Static decode method for network handler registration.
     */
    public static SpawnBarrierFXPacket decode(FriendlyByteBuf buffer) {
        return new SpawnBarrierFXPacket(buffer);
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
        BarrierFXRenderer.render(level, x, y, z, effectType, radius);
    }

    // Getters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public float getRadius() {
        return radius;
    }

    @Nullable
    public Integer getBarrierId() {
        return barrierId;
    }
}
