package com.complextalents.network;

import mod.chloeprime.aaaparticles.api.common.AAALevel;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * Server-to-client packet to spawn AAA particles with advanced parameters.
 */
public class S2CSpawnAAAParticlePacket {
    private final ResourceLocation effect;
    private final Vec3 pos;
    private final Vector3f rotation;
    private final float scale;

    public S2CSpawnAAAParticlePacket(ResourceLocation effect, Vec3 pos) {
        this(effect, pos, new Vector3f(0), 1.0f);
    }

    public S2CSpawnAAAParticlePacket(ResourceLocation effect, Vec3 pos, Vector3f rotation, float scale) {
        this.effect = effect;
        this.pos = pos;
        this.rotation = rotation;
        this.scale = scale;
    }

    public S2CSpawnAAAParticlePacket(FriendlyByteBuf buffer) {
        this.effect = buffer.readResourceLocation();
        this.pos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.rotation = new Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        this.scale = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(effect);
        buffer.writeDouble(pos.x);
        buffer.writeDouble(pos.y);
        buffer.writeDouble(pos.z);
        buffer.writeFloat(rotation.x());
        buffer.writeFloat(rotation.y());
        buffer.writeFloat(rotation.z());
        buffer.writeFloat(scale);
    }

    public static S2CSpawnAAAParticlePacket decode(FriendlyByteBuf buffer) {
        return new S2CSpawnAAAParticlePacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;

        AAALevel.addParticle(level, false,
                new ParticleEmitterInfo(effect)
                        .position(pos.x, pos.y, pos.z)
                        .rotation(rotation.x(), rotation.y(), rotation.z())
                        .scale(scale));
    }
}
