package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.network.elemental.SpawnBurningReactionPacket;
import com.complextalents.network.elemental.SpawnElectroChargedReactionPacket;
import com.complextalents.network.elemental.SpawnElementFXPacket;
import com.complextalents.network.elemental.SpawnFractureReactionPacket;
import com.complextalents.network.elemental.SpawnFreezeReactionPacket;
import com.complextalents.network.elemental.SpawnMeltReacionPacket;
import com.complextalents.network.elemental.SpawnOverloadReactionPacket;
import com.complextalents.network.elemental.SpawnPermafrostReactionPacket;
import com.complextalents.network.elemental.SpawnReactionTextPacket;
import com.complextalents.network.elemental.SpawnSuperconductReactionPacket;
import com.complextalents.network.elemental.SpawnVaporizeReactionPacket;
import com.complextalents.network.elemental.SpawnVoidfireReactionPacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.registerMessage(packetId++,
                SpawnElementFXPacket.class,
                SpawnElementFXPacket::encode,
                SpawnElementFXPacket::decode,
                SpawnElementFXPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnReactionTextPacket.class,
                SpawnReactionTextPacket::encode,
                SpawnReactionTextPacket::decode,
                SpawnReactionTextPacket::handle);

        INSTANCE.registerMessage(packetId++,
                ToggleCombatModePacket.class,
                ToggleCombatModePacket::encode,
                ToggleCombatModePacket::decode,
                ToggleCombatModePacket::handle);

        // Reaction effect packets
        INSTANCE.registerMessage(packetId++,
                SpawnBurningReactionPacket.class,
                SpawnBurningReactionPacket::encode,
                SpawnBurningReactionPacket::decode,
                SpawnBurningReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnOverloadReactionPacket.class,
                SpawnOverloadReactionPacket::encode,
                SpawnOverloadReactionPacket::decode,
                SpawnOverloadReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnMeltReacionPacket.class,
                SpawnMeltReacionPacket::encode,
                SpawnMeltReacionPacket::decode,
                SpawnMeltReacionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnVaporizeReactionPacket.class,
                SpawnVaporizeReactionPacket::encode,
                SpawnVaporizeReactionPacket::decode,
                SpawnVaporizeReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnVoidfireReactionPacket.class,
                SpawnVoidfireReactionPacket::encode,
                SpawnVoidfireReactionPacket::decode,
                SpawnVoidfireReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnFreezeReactionPacket.class,
                SpawnFreezeReactionPacket::encode,
                SpawnFreezeReactionPacket::decode,
                SpawnFreezeReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnSuperconductReactionPacket.class,
                SpawnSuperconductReactionPacket::encode,
                SpawnSuperconductReactionPacket::decode,
                SpawnSuperconductReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnPermafrostReactionPacket.class,
                SpawnPermafrostReactionPacket::encode,
                SpawnPermafrostReactionPacket::decode,
                SpawnPermafrostReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnFractureReactionPacket.class,
                SpawnFractureReactionPacket::encode,
                SpawnFractureReactionPacket::decode,
                SpawnFractureReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnElectroChargedReactionPacket.class,
                SpawnElectroChargedReactionPacket::encode,
                SpawnElectroChargedReactionPacket::decode,
                SpawnElectroChargedReactionPacket::handle);

        TalentsMod.LOGGER.info("Network packets registered");
    }

    public static void sendTo(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    /**
     * Sends a packet to all clients near a position (64 block range)
     */
    public static void sendToNearby(Object packet, ServerLevel level, Vec3 pos) {
        INSTANCE.send(PacketDistributor.NEAR.with(() ->
            new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 64.0, level.dimension())), packet);
    }
}
