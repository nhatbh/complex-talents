package com.complextalents.network;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
public class SpawnParticlesPacket {
    public enum ParticleType {
        ELEMENT_STACK,
        REACTION
    }

    private final ParticleType type;
    private final Vec3 position;
    private final String dataString; // Element name or Reaction name
    private final int extraData; // Stack count for ELEMENT_STACK, unused for REACTION

    // Constructor for element stack particles
    public SpawnParticlesPacket(Vec3 position, ElementType element, int stackCount) {
        this.type = ParticleType.ELEMENT_STACK;
        this.position = position;
        this.dataString = element.name();
        this.extraData = stackCount;
    }

    // Constructor for reaction particles
    public SpawnParticlesPacket(Vec3 position, ElementalReaction reaction) {
        this.type = ParticleType.REACTION;
        this.position = position;
        this.dataString = reaction.name();
        this.extraData = 0;
    }

    // Decode constructor
    public SpawnParticlesPacket(FriendlyByteBuf buffer) {
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

    public static SpawnParticlesPacket decode(FriendlyByteBuf buffer) {
        return new SpawnParticlesPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        if (type == ParticleType.ELEMENT_STACK) {
            ElementType element = ElementType.valueOf(dataString);
            spawnStackParticles(level, position, element, extraData);
            playStackSound(level, position, element, extraData);
        } else if (type == ParticleType.REACTION) {
            ElementalReaction reaction = ElementalReaction.valueOf(dataString);
            spawnReactionParticles(level, position, reaction);
            playReactionSound(level, position, reaction);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnStackParticles(Level level, Vec3 pos, ElementType element, int stackCount) {
        ParticleOptions particle = getParticleForElement(element);
        if (particle == null) return;

        // Scale effects based on stack count
        int particleCount = 5 + (stackCount * 4); // More particles per stack
        double radius = 0.6 + (stackCount * 0.1); // Larger radius for higher stacks
        double height = 1.2 + (stackCount * 0.15); // Taller effect for more stacks

        // Main particle spiral effect
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.1);
            double spiralHeight = (i / (double) particleCount) * height;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = spiralHeight;

            // Spiral upward velocity
            double velocityX = Math.cos(angle) * 0.05;
            double velocityY = 0.15 + (stackCount * 0.02);
            double velocityZ = Math.sin(angle) * 0.05;

            level.addParticle(particle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                velocityX, velocityY, velocityZ);
        }

        // Add burst particles for high stacks
        if (stackCount >= 3) {
            for (int i = 0; i < stackCount * 3; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * radius * 1.5;
                double offsetY = level.random.nextDouble() * height;
                double offsetZ = (level.random.nextDouble() - 0.5) * radius * 1.5;

                double velocityX = (level.random.nextDouble() - 0.5) * 0.2;
                double velocityY = level.random.nextDouble() * 0.25;
                double velocityZ = (level.random.nextDouble() - 0.5) * 0.2;

                level.addParticle(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    velocityX, velocityY, velocityZ);
            }
        }

        // Add glow effect for max stacks
        if (stackCount >= 5) {
            level.addParticle(ParticleTypes.GLOW,
                pos.x, pos.y + 0.5, pos.z,
                0, 0.1, 0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnReactionParticles(Level level, Vec3 pos, ElementalReaction reaction) {
        ParticleOptions particle = getParticleForReaction(reaction);
        if (particle == null) return;

        // MASSIVE explosion burst
        int particleCount = 60; // Tripled particle count
        double radius = 2.0; // Doubled radius

        // Expanding ring effect
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = radius * (ring + 1) / 3.0;
            int ringParticles = particleCount / 3;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (i * Math.PI * 2.0) / ringParticles;
                double offsetX = Math.cos(angle) * ringRadius;
                double offsetZ = Math.sin(angle) * ringRadius;
                double offsetY = level.random.nextDouble() * 2.5;

                double velocityX = Math.cos(angle) * (0.4 + ring * 0.1);
                double velocityY = (level.random.nextDouble() - 0.3) * 0.4;
                double velocityZ = Math.sin(angle) * (0.4 + ring * 0.1);

                level.addParticle(particle,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    velocityX, velocityY, velocityZ);
            }
        }

        // Central explosion core
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            double velocityX = (level.random.nextDouble() - 0.5) * 0.6;
            double velocityY = level.random.nextDouble() * 0.8;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.6;

            level.addParticle(particle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                velocityX, velocityY, velocityZ);
        }

        // Multiple flash effects
        for (int i = 0; i < 5; i++) {
            double flashOffset = i * 0.3;
            level.addParticle(ParticleTypes.FLASH,
                pos.x, pos.y + 0.5 + flashOffset, pos.z,
                0, 0, 0);
        }

        // Add secondary particle effects based on reaction type
        addReactionSpecificEffects(level, pos, reaction);
    }

    private static ParticleOptions getParticleForElement(ElementType element) {
        return switch (element) {
            case FIRE -> ParticleRegistry.FIRE_PARTICLE.get();
            case AQUA -> ParticleRegistry.ACID_BUBBLE_PARTICLE.get();
            case ICE -> ParticleRegistry.SNOWFLAKE_PARTICLE.get();
            case LIGHTNING -> ParticleRegistry.ELECTRICITY_PARTICLE.get();
            case NATURE -> ParticleRegistry.FIREFLY_PARTICLE.get();
            case ENDER -> ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get();
        };
    }

    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> ParticleRegistry.FOG_PARTICLE.get();
            case MELT -> ParticleRegistry.ACID_PARTICLE.get();
            case OVERLOADED -> ParticleRegistry.BLASTWAVE_PARTICLE.get();
            case ELECTRO_CHARGED -> ParticleRegistry.ZAP_PARTICLE.get();
            case FROZEN -> ParticleRegistry.SNOW_DUST.get();
            case SUPERCONDUCT -> ParticleRegistry.RING_SMOKE_PARTICLE.get();
            case BURNING -> ParticleRegistry.DRAGON_FIRE_PARTICLE.get();
            case BLOOM -> ParticleRegistry.FIREFLY_PARTICLE.get();
            case HYPERBLOOM -> ParticleRegistry.WISP_PARTICLE.get();
            case BURGEON -> ParticleRegistry.EMBER_PARTICLE.get();
            case UNSTABLE_WARD -> ParticleRegistry.PORTAL_FRAME_PARTICLE.get();
            case RIFT_PULL -> ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get();
            case SINGULARITY -> ParticleRegistry.SIPHON_PARTICLE.get();
            case FRACTURE -> ParticleRegistry.SHOCKWAVE_PARTICLE.get();
            case WITHERING_SEED -> ParticleRegistry.BLOOD_GROUND_PARTICLE.get();
            case DECREPIT_GRASP -> ParticleRegistry.BLOOD_PARTICLE.get();
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void addReactionSpecificEffects(Level level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                // Rising steam clouds
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
                    level.addParticle(ParticleRegistry.FOG_PARTICLE.get(),
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        0, 0.4, 0);
                }
                // Add acid bubbles for water effect
                for (int i = 0; i < 15; i++) {
                    level.addParticle(ParticleRegistry.ACID_BUBBLE_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        0, 0.2, 0);
                }
            }
            case OVERLOADED -> {
                // Explosion shockwave with blastwave particles
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleRegistry.BLASTWAVE_PARTICLE.get(),
                        pos.x, pos.y + (i * 0.3), pos.z, 0, 0, 0);
                }
                // Add spark ring
                for (int i = 0; i < 50; i++) {
                    double angle = (i * Math.PI * 2.0) / 50;
                    level.addParticle(ParticleRegistry.SPARK_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 2.5, pos.y, pos.z + Math.sin(angle) * 2.5,
                        Math.cos(angle) * 0.6, 0.1, Math.sin(angle) * 0.6);
                }
                // Add smoke
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleRegistry.RING_SMOKE_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        level.random.nextDouble() * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.3);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts with electricity and zap particles
                for (int i = 0; i < 25; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.addParticle(ParticleRegistry.ELECTRICITY_PARTICLE.get(),
                        pos.x + offsetX, pos.y + 2.5, pos.z + offsetZ,
                        0, -0.6, 0);
                }
                // Add zap particles
                for (int i = 0; i < 15; i++) {
                    level.addParticle(ParticleRegistry.ZAP_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3);
                }
            }
            case FROZEN -> {
                // Ice crystal burst with snowflakes
                for (int i = 0; i < 40; i++) {
                    double angle = (i * Math.PI * 2.0) / 40;
                    level.addParticle(ParticleRegistry.SNOWFLAKE_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(angle) * 0.5, 0.15, Math.sin(angle) * 0.5);
                }
                // Add snow dust
                for (int i = 0; i < 30; i++) {
                    level.addParticle(ParticleRegistry.SNOW_DUST.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.4);
                }
            }
            case BURNING -> {
                // Fire spiral with dragon fire
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    double height = i * 0.12;
                    level.addParticle(ParticleRegistry.DRAGON_FIRE_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 1.8, pos.y + height, pos.z + Math.sin(angle) * 1.8,
                        0, 0.25, 0);
                }
                // Add embers
                for (int i = 0; i < 25; i++) {
                    level.addParticle(ParticleRegistry.EMBER_PARTICLE.get(),
                        pos.x, pos.y + 0.2, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.5);
                }
            }
            case HYPERBLOOM -> {
                // Glowing wisps
                for (int i = 0; i < 30; i++) {
                    level.addParticle(ParticleRegistry.WISP_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7);
                }
                // Add fireflies
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleRegistry.FIREFLY_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.2);
                }
            }
            case SINGULARITY -> {
                // Swirling void effect with siphon particles
                for (int ring = 0; ring < 6; ring++) {
                    for (int i = 0; i < 25; i++) {
                        double angle = (i * Math.PI * 2.0) / 25 + ring;
                        double ringRadius = 2.5 - (ring * 0.35);
                        level.addParticle(ParticleRegistry.SIPHON_PARTICLE.get(),
                            pos.x + Math.cos(angle) * ringRadius,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * ringRadius,
                            -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4);
                    }
                }
                // Add unstable ender particles
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3);
                }
            }
            case FRACTURE -> {
                // Shattering effect with shockwave
                for (int i = 0; i < 5; i++) {
                    level.addParticle(ParticleRegistry.SHOCKWAVE_PARTICLE.get(),
                        pos.x, pos.y + 1.0 + (i * 0.2), pos.z,
                        0, 0, 0);
                }
                // Add spark explosion
                for (int i = 0; i < 60; i++) {
                    level.addParticle(ParticleRegistry.SPARK_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.9,
                        (level.random.nextDouble() - 0.5) * 0.9,
                        (level.random.nextDouble() - 0.5) * 0.9);
                }
            }
            case BLOOM -> {
                // Nature bloom with fireflies
                for (int i = 0; i < 40; i++) {
                    level.addParticle(ParticleRegistry.FIREFLY_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        level.random.nextDouble() * 0.5,
                        (level.random.nextDouble() - 0.5) * 0.6);
                }
            }
            case BURGEON -> {
                // Explosive nature with embers
                for (int i = 0; i < 35; i++) {
                    level.addParticle(ParticleRegistry.EMBER_PARTICLE.get(),
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.7);
                }
            }
            case UNSTABLE_WARD -> {
                // Portal frame effect
                for (int i = 0; i < 20; i++) {
                    double angle = (i * Math.PI * 2.0) / 20;
                    level.addParticle(ParticleRegistry.PORTAL_FRAME_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 1.5,
                        pos.y + 1.0,
                        pos.z + Math.sin(angle) * 1.5,
                        0, 0.1, 0);
                }
            }
            case RIFT_PULL -> {
                // Unstable ender vortex
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    level.addParticle(ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get(),
                        pos.x + Math.cos(angle) * 2.0,
                        pos.y + 0.5,
                        pos.z + Math.sin(angle) * 2.0,
                        -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4);
                }
            }
            case WITHERING_SEED -> {
                // Blood ground spread
                for (int i = 0; i < 25; i++) {
                    level.addParticle(ParticleRegistry.BLOOD_GROUND_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.1,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        0, 0.05, 0);
                }
            }
            case DECREPIT_GRASP -> {
                // Blood particles
                for (int i = 0; i < 30; i++) {
                    level.addParticle(ParticleRegistry.BLOOD_PARTICLE.get(),
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + level.random.nextDouble() * 2.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        (level.random.nextDouble() - 0.5) * 0.2,
                        -0.1,
                        (level.random.nextDouble() - 0.5) * 0.2);
                }
            }
            default -> {
                // Default spectacular burst with wisps
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleRegistry.WISP_PARTICLE.get(),
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.5,
                        (level.random.nextDouble() - 0.5) * 0.5);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void playStackSound(Level level, Vec3 pos, ElementType element, int stackCount) {
        float volume = 0.3f + (stackCount * 0.1f); // Louder for higher stacks
        float pitch = 1.0f + (stackCount * 0.15f); // Higher pitch for higher stacks

        switch (element) {
            case FIRE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                volume, pitch, false);
            case AQUA -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                volume, pitch, false);
            case ICE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                volume, pitch + 0.3f, false);
            case LIGHTNING -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                volume * 0.5f, pitch, false);
            case NATURE -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS,
                volume, pitch, false);
            case ENDER -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                volume * 0.7f, pitch + 0.5f, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void playReactionSound(Level level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    1.5f, 1.0f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.8f, 1.5f, false);
            }
            case MELT -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.LAVA_POP, SoundSource.PLAYERS,
                1.5f, 0.8f, false);
            case OVERLOADED -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    2.0f, 0.9f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                    1.5f, 1.2f, false);
            }
            case ELECTRO_CHARGED -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                1.5f, 1.3f, false);
            case FROZEN -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                    1.8f, 0.7f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS,
                    1.5f, 0.9f, false);
            }
            case SUPERCONDUCT -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS,
                1.8f, 0.8f, false);
            case BURNING -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                    1.5f, 0.9f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS,
                    1.0f, 1.2f, false);
            }
            case BLOOM -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.FLOWERING_AZALEA_BREAK, SoundSource.PLAYERS,
                2.0f, 0.8f, false);
            case HYPERBLOOM -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                    1.5f, 1.5f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.2f, 1.3f, false);
            }
            case BURGEON -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                1.8f, 0.8f, false);
            case UNSTABLE_WARD -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS,
                1.5f, 1.3f, false);
            case RIFT_PULL -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                1.8f, 0.6f, false);
            case SINGULARITY -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS,
                    1.5f, 1.5f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS,
                    1.2f, 0.8f, false);
            }
            case FRACTURE -> {
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                    2.0f, 0.5f, false);
                level.playLocalSound(pos.x, pos.y, pos.z,
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                    1.5f, 1.0f, false);
            }
            case WITHERING_SEED -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS,
                1.5f, 0.9f, false);
            case DECREPIT_GRASP -> level.playLocalSound(pos.x, pos.y, pos.z,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS,
                1.5f, 0.7f, false);
        }
    }
}
