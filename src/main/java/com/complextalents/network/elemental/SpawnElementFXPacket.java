package com.complextalents.network.elemental;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.client.renderers.reactions.ElementFXRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Network packet for spawning particle effects on the client
 * Sent from server when element stacks are applied or reactions trigger
 */
public class SpawnElementFXPacket {
    public enum ParticleType {
        ELEMENT_STACK,
        REACTION
    }

    private final ParticleType type;
    private final Vec3 position;
    private final String dataString; // Element name or Reaction name
    private final int extraData; // Stack count for ELEMENT_STACK, unused for REACTION

    // Constructor for element stack particles
    public SpawnElementFXPacket(Vec3 position, ElementType element, int stackCount) {
        this.type = ParticleType.ELEMENT_STACK;
        this.position = position;
        this.dataString = element.name();
        this.extraData = stackCount;
    }

    // Constructor for reaction particles
    public SpawnElementFXPacket(Vec3 position, ElementalReaction reaction) {
        this.type = ParticleType.REACTION;
        this.position = position;
        this.dataString = reaction.name();
        this.extraData = 0;
    }

    // Decode constructor
    public SpawnElementFXPacket(FriendlyByteBuf buffer) {
        this.type = buffer.readEnum(ParticleType.class);
        this.position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.dataString = buffer.readUtf();
        this.extraData = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(type);
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
        buffer.writeUtf(dataString);
        buffer.writeInt(extraData);
    }

    public static SpawnElementFXPacket decode(FriendlyByteBuf buffer) {
        return new SpawnElementFXPacket(buffer);
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
            System.out.println("[Complex Talents] Client level is null, cannot spawn particles!");
            return;
        }

        try {
            if (type == ParticleType.ELEMENT_STACK) {
                ElementType element = ElementType.valueOf(dataString);
                System.out.println("[Complex Talents] Spawning stack particles for element: " + element + " at " + position);
                ElementFXRenderer.play(level, position, element, extraData);
            } 
        } catch (Exception e) {
            System.err.println("[Complex Talents] ERROR spawning particles:");
            e.printStackTrace();
        }
    }
}
