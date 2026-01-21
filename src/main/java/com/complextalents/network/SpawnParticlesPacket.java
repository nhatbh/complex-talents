package com.complextalents.network;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
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
            case FIRE -> ParticleTypes.FLAME;
            case AQUA -> ParticleTypes.DRIPPING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case ENDER -> ParticleTypes.PORTAL;
        };
    }

    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> ParticleTypes.CLOUD;
            case MELT -> ParticleTypes.DRIPPING_DRIPSTONE_WATER;
            case OVERLOADED -> ParticleTypes.EXPLOSION;
            case ELECTRO_CHARGED -> ParticleTypes.ELECTRIC_SPARK;
            case FROZEN -> ParticleTypes.SNOWFLAKE;
            case SUPERCONDUCT -> ParticleTypes.SOUL_FIRE_FLAME;
            case BURNING -> ParticleTypes.LAVA;
            case BLOOM -> ParticleTypes.CHERRY_LEAVES;
            case HYPERBLOOM -> ParticleTypes.GLOW;
            case BURGEON -> ParticleTypes.FLAME;
            case UNSTABLE_WARD -> ParticleTypes.REVERSE_PORTAL;
            case RIFT_PULL -> ParticleTypes.PORTAL;
            case SINGULARITY -> ParticleTypes.WITCH;
            case FRACTURE -> ParticleTypes.FIREWORK;
            case WITHERING_SEED -> ParticleTypes.SCULK_SOUL;
            case DECREPIT_GRASP -> ParticleTypes.SOUL;
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void addReactionSpecificEffects(Level level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                // Rising steam clouds
                for (int i = 0; i < 20; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.addParticle(ParticleTypes.CLOUD,
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        0, 0.3, 0);
                }
            }
            case OVERLOADED -> {
                // Explosion shockwave
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0, 0, 0);
                for (int i = 0; i < 40; i++) {
                    double angle = (i * Math.PI * 2.0) / 40;
                    level.addParticle(ParticleTypes.SMOKE,
                        pos.x + Math.cos(angle) * 2.0, pos.y, pos.z + Math.sin(angle) * 2.0,
                        Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts
                for (int i = 0; i < 15; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + offsetX, pos.y + 2.0, pos.z + offsetZ,
                        0, -0.5, 0);
                }
            }
            case FROZEN -> {
                // Ice crystal burst
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    level.addParticle(ParticleTypes.SNOWFLAKE,
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4);
                }
                // Add ice shards
                for (int i = 0; i < 10; i++) {
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL,
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.5);
                }
            }
            case BURNING -> {
                // Fire spiral
                for (int i = 0; i < 25; i++) {
                    double angle = (i * Math.PI * 2.0) / 25;
                    double height = i * 0.1;
                    level.addParticle(ParticleTypes.FLAME,
                        pos.x + Math.cos(angle) * 1.5, pos.y + height, pos.z + Math.sin(angle) * 1.5,
                        0, 0.2, 0);
                }
                level.addParticle(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            case HYPERBLOOM -> {
                // Glowing orbs
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleTypes.END_ROD,
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        (level.random.nextDouble() - 0.5) * 0.6);
                }
            }
            case SINGULARITY -> {
                // Swirling void effect
                for (int ring = 0; ring < 5; ring++) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (i * Math.PI * 2.0) / 20 + ring;
                        double ringRadius = 2.0 - (ring * 0.3);
                        level.addParticle(ParticleTypes.WITCH,
                            pos.x + Math.cos(angle) * ringRadius,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * ringRadius,
                            -Math.cos(angle) * 0.3, 0, -Math.sin(angle) * 0.3);
                    }
                }
                level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            case FRACTURE -> {
                // Shattering effect with fireworks
                for (int i = 0; i < 50; i++) {
                    level.addParticle(ParticleTypes.FIREWORK,
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.8);
                }
                level.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y + 1.0, pos.z, 0, 0, 0);
            }
            default -> {
                // Default spectacular burst for other reactions
                for (int i = 0; i < 15; i++) {
                    level.addParticle(ParticleTypes.ENCHANT,
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
