package com.complextalents.network;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
                SpawnParticlesPacket.class,
                SpawnParticlesPacket::encode,
                SpawnParticlesPacket::decode,
                SpawnParticlesPacket::handle);

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

        TalentsMod.LOGGER.info("Network packets registered");
    }

    public static void sendTo(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
