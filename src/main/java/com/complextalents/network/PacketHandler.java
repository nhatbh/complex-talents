package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.origin.network.OriginDataSyncPacket;
import com.complextalents.passive.network.PassiveStackSyncPacket;
import com.complextalents.skill.network.SkillCastPacket;
import com.complextalents.skill.network.SkillChannelStartPacket;
import com.complextalents.skill.network.SkillChannelStartResponsePacket;
import com.complextalents.skill.network.SkillCooldownSyncPacket;
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
import com.complextalents.network.yygm.SpawnYinYangGateFXPacket;
import com.complextalents.network.yygm.YinYangAnnihilationHitPacket;
import com.complextalents.network.yygm.YinYangAnnihilationSyncPacket;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import com.complextalents.network.yygm.ExposedStateSyncPacket;
import com.complextalents.network.yygm.EquilibriumSyncPacket;
import com.complextalents.network.yygm.SwordDanceDashPacket;
import com.complextalents.network.yygm.SwordDanceGateActivatePacket;
import com.complextalents.network.darkmage.SoulSyncPacket;
import com.complextalents.network.assassin.AssassinSyncPacket;
import com.complextalents.network.assassin.AssassinEntitySyncPacket;

import com.complextalents.network.elementalmage.ElementalMageSyncPacket;
import com.complextalents.network.highpriest.FaithSyncPacket;

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
                        PROTOCOL_VERSION::equals);

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

                // Seraph's Bouncing Sword FX packet
                INSTANCE.registerMessage(packetId++,
                                SpawnSeraphSwordFXPacket.class,
                                SpawnSeraphSwordFXPacket::encode,
                                SpawnSeraphSwordFXPacket::decode,
                                SpawnSeraphSwordFXPacket::handle);

                // Sanctuary Barrier FX packet
                // Holy Beam packets - Covenant of Protection visual effects
                INSTANCE.registerMessage(packetId++,
                                ActivateBeamPacket.class,
                                ActivateBeamPacket::encode,
                                ActivateBeamPacket::decode,
                                ActivateBeamPacket::handle);

                INSTANCE.registerMessage(packetId++,
                                PulseBeamPacket.class,
                                PulseBeamPacket::encode,
                                PulseBeamPacket::decode,
                                PulseBeamPacket::handle);

                INSTANCE.registerMessage(packetId++,
                                DeactivateBeamPacket.class,
                                DeactivateBeamPacket::encode,
                                DeactivateBeamPacket::decode,
                                DeactivateBeamPacket::handle);

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

                // Origin data sync packet
                INSTANCE.registerMessage(packetId++,
                                OriginDataSyncPacket.class,
                                OriginDataSyncPacket::encode,
                                OriginDataSyncPacket::decode,
                                OriginDataSyncPacket::handle);

                // Passive stack sync packet
                INSTANCE.registerMessage(packetId++,
                                PassiveStackSyncPacket.class,
                                PassiveStackSyncPacket::encode,
                                PassiveStackSyncPacket::decode,
                                PassiveStackSyncPacket::handle);

                // Skill cooldown sync packet
                INSTANCE.registerMessage(packetId++,
                                SkillCooldownSyncPacket.class,
                                SkillCooldownSyncPacket::encode,
                                SkillCooldownSyncPacket::decode,
                                SkillCooldownSyncPacket::handle);

                // Skill channel start packet (client requests server validation before
                // channeling)
                INSTANCE.registerMessage(packetId++,
                                SkillChannelStartPacket.class,
                                SkillChannelStartPacket::encode,
                                SkillChannelStartPacket::decode,
                                SkillChannelStartPacket::handle);

                // Skill channel start response packet (server responds to channel start
                // request)
                INSTANCE.registerMessage(packetId++,
                                SkillChannelStartResponsePacket.class,
                                SkillChannelStartResponsePacket::encode,
                                SkillChannelStartResponsePacket::decode,
                                SkillChannelStartResponsePacket::handle);

                // Yin Yang Grandmaster gate state sync packet
                INSTANCE.registerMessage(packetId++,
                                YinYangGateStateSyncPacket.class,
                                YinYangGateStateSyncPacket::encode,
                                YinYangGateStateSyncPacket::decode,
                                YinYangGateStateSyncPacket::handle);

                // Yin Yang Grandmaster gate FX packet (Yang hit, Yin hit, Discord, Gate spawn)
                INSTANCE.registerMessage(packetId++,
                                SpawnYinYangGateFXPacket.class,
                                SpawnYinYangGateFXPacket::encode,
                                SpawnYinYangGateFXPacket::decode,
                                SpawnYinYangGateFXPacket::handle);

                // Yin Yang Grandmaster Equilibrium sync packet
                INSTANCE.registerMessage(packetId++,
                                EquilibriumSyncPacket.class,
                                EquilibriumSyncPacket::encode,
                                EquilibriumSyncPacket::decode,
                                EquilibriumSyncPacket::handle);

                // Sword Dance dash visualization packet
                INSTANCE.registerMessage(packetId++,
                                SwordDanceDashPacket.class,
                                SwordDanceDashPacket::encode,
                                SwordDanceDashPacket::decode,
                                SwordDanceDashPacket::handle);

                // Sword Dance gate activation packet
                INSTANCE.registerMessage(packetId++,
                                SwordDanceGateActivatePacket.class,
                                SwordDanceGateActivatePacket::encode,
                                SwordDanceGateActivatePacket::decode,
                                SwordDanceGateActivatePacket::handle);

                // Exposed state sync packet (Eight Formation Battle Array Ultimate)
                INSTANCE.registerMessage(packetId++,
                                ExposedStateSyncPacket.class,
                                ExposedStateSyncPacket::encode,
                                ExposedStateSyncPacket::decode,
                                ExposedStateSyncPacket::handle);

                // Yin Yang Annihilation sync packet (spinning texture during Annihilation)
                INSTANCE.registerMessage(packetId++,
                                YinYangAnnihilationSyncPacket.class,
                                YinYangAnnihilationSyncPacket::encode,
                                YinYangAnnihilationSyncPacket::decode,
                                YinYangAnnihilationSyncPacket::handle);

                // Yin Yang Annihilation hit packet (triggers expanding ring animation on each
                // hit)
                INSTANCE.registerMessage(packetId++,
                                YinYangAnnihilationHitPacket.class,
                                YinYangAnnihilationHitPacket::encode,
                                YinYangAnnihilationHitPacket::decode,
                                YinYangAnnihilationHitPacket::handle);

                // Dark Mage soul sync packet
                INSTANCE.registerMessage(packetId++,
                                SoulSyncPacket.class,
                                SoulSyncPacket::encode,
                                SoulSyncPacket::decode,
                                SoulSyncPacket::handle);

                // Assassin sync packet
                INSTANCE.registerMessage(packetId++,
                                AssassinSyncPacket.class,
                                AssassinSyncPacket::encode,
                                AssassinSyncPacket::decode,
                                AssassinSyncPacket::handle);

                INSTANCE.registerMessage(packetId++,
                                AssassinEntitySyncPacket.class,
                                AssassinEntitySyncPacket::encode,
                                AssassinEntitySyncPacket::decode,
                                AssassinEntitySyncPacket::handle);

                INSTANCE.registerMessage(packetId++,
                                S2CSpawnAAAParticlePacket.class,
                                S2CSpawnAAAParticlePacket::encode,
                                S2CSpawnAAAParticlePacket::decode,
                                S2CSpawnAAAParticlePacket::handle);

                // Elemental Mage attribute sync packet
                INSTANCE.registerMessage(packetId++,
                                ElementalMageSyncPacket.class,
                                ElementalMageSyncPacket::encode,
                                ElementalMageSyncPacket::decode,
                                ElementalMageSyncPacket::handle);

                // High Priest faith sync packet
                INSTANCE.registerMessage(packetId++,
                                FaithSyncPacket.class,
                                FaithSyncPacket::encode,
                                FaithSyncPacket::decode,
                                FaithSyncPacket::handle);

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
                INSTANCE.send(PacketDistributor.NEAR.with(
                                () -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 64.0, level.dimension())),
                                packet);
        }
}
