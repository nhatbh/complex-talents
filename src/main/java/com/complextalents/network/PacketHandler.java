package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.targeting.network.SkillUsePacket;
import com.complextalents.skill.network.SkillCastPacket;
import com.complextalents.skill.network.SkillDataSyncPacket;
import com.complextalents.network.elemental.SpawnBloomReactionPacket;
import com.complextalents.network.elemental.SpawnBlackHoleParticlePacket;
import com.complextalents.network.elemental.SpawnBurningReactionPacket;
import com.complextalents.network.elemental.SpawnElectroChargedReactionPacket;
import com.complextalents.network.elemental.SpawnElementFXPacket;
import com.complextalents.network.elemental.SpawnFluxReactionPacket;
import com.complextalents.network.elemental.SpawnFractureReactionPacket;
import com.complextalents.network.elemental.SpawnFreezeReactionPacket;
import com.complextalents.network.elemental.SpawnMeltReacionPacket;
import com.complextalents.network.elemental.SpawnNatureCoreExplosionPacket;
import com.complextalents.network.elemental.SpawnNatureCoreParticlePacket;
import com.complextalents.network.elemental.SpawnOvergrowthReactionPacket;
import com.complextalents.network.elemental.SpawnOverloadReactionPacket;
import com.complextalents.network.elemental.SpawnPermafrostReactionPacket;
import com.complextalents.network.elemental.SpawnReactionTextPacket;
import com.complextalents.network.elemental.SpawnSpringReactionPacket;
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

        INSTANCE.registerMessage(packetId++,
                SpawnBloomReactionPacket.class,
                SpawnBloomReactionPacket::encode,
                SpawnBloomReactionPacket::decode,
                SpawnBloomReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnNatureCoreParticlePacket.class,
                SpawnNatureCoreParticlePacket::encode,
                SpawnNatureCoreParticlePacket::decode,
                SpawnNatureCoreParticlePacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnNatureCoreExplosionPacket.class,
                SpawnNatureCoreExplosionPacket::encode,
                SpawnNatureCoreExplosionPacket::decode,
                SpawnNatureCoreExplosionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnSpringReactionPacket.class,
                SpawnSpringReactionPacket::encode,
                SpawnSpringReactionPacket::decode,
                SpawnSpringReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnFluxReactionPacket.class,
                SpawnFluxReactionPacket::encode,
                SpawnFluxReactionPacket::decode,
                SpawnFluxReactionPacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnBlackHoleParticlePacket.class,
                SpawnBlackHoleParticlePacket::encode,
                SpawnBlackHoleParticlePacket::decode,
                SpawnBlackHoleParticlePacket::handle);

        INSTANCE.registerMessage(packetId++,
                SpawnOvergrowthReactionPacket.class,
                SpawnOvergrowthReactionPacket::encode,
                SpawnOvergrowthReactionPacket::decode,
                SpawnOvergrowthReactionPacket::handle);

        // Targeting system packets
        INSTANCE.registerMessage(packetId++,
                SkillUsePacket.class,
                SkillUsePacket::encode,
                SkillUsePacket::decode,
                SkillUsePacket::handle);

        // Skill casting packets
        INSTANCE.registerMessage(packetId++,
                SkillCastPacket.class,
                SkillCastPacket::encode,
                SkillCastPacket::decode,
                SkillCastPacket::handle);

        // Skill data sync packet
        INSTANCE.registerMessage(packetId++,
                SkillDataSyncPacket.class,
                SkillDataSyncPacket::encode,
                SkillDataSyncPacket::decode,
                SkillDataSyncPacket::handle);

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
