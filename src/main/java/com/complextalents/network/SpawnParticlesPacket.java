package com.complextalents.network;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

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
        if (level == null) {
            System.out.println("[Complex Talents] Client level is null, cannot spawn particles!");
            return;
        }

        try {
            if (type == ParticleType.ELEMENT_STACK) {
                ElementType element = ElementType.valueOf(dataString);
                System.out.println("[Complex Talents] Spawning stack particles for element: " + element + " at " + position);
                spawnStackParticles(level, position, element, extraData);
                playStackSound(level, position, element, extraData);
            } else if (type == ParticleType.REACTION) {
                ElementalReaction reaction = ElementalReaction.valueOf(dataString);
                System.out.println("[Complex Talents] Spawning reaction particles for: " + reaction + " at " + position);
                spawnReactionParticles(level, position, reaction);
                playReactionSound(level, position, reaction);
            }
        } catch (Exception e) {
            System.err.println("[Complex Talents] ERROR spawning particles:");
            e.printStackTrace();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnStackParticles(Level level, Vec3 pos, ElementType element, int stackCount) {
        // Use element-specific particle patterns for more variety
        switch (element) {
            case FIRE -> spawnFireStackParticles(level, pos, stackCount);
            case AQUA -> spawnAquaStackParticles(level, pos, stackCount);
            case ICE -> spawnIceStackParticles(level, pos, stackCount);
            case LIGHTNING -> spawnLightningStackParticles(level, pos, stackCount);
            case NATURE -> spawnNatureStackParticles(level, pos, stackCount);
            case ENDER -> spawnEnderStackParticles(level, pos, stackCount);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnFireStackParticles(Level level, Vec3 pos, int stackCount) {
        // Rising flames and embers
        ParticleOptions fireParticle = getIronParticle("fire");
        int particleCount = 8 + (stackCount * 3);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(fireParticle,
                pos.x + offsetX, pos.y + 0.2, pos.z + offsetZ,
                0, 0.15 + (stackCount * 0.02), 0);
        }
        // Add embers for higher stacks
        if (stackCount >= 2) {
            ParticleOptions emberParticle = getIronParticle("ember");
            for (int i = 0; i < stackCount * 2; i++) {
                level.addParticle(emberParticle,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.8,
                    pos.y + level.random.nextDouble() * 1.5,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.8,
                    (level.random.nextDouble() - 0.5) * 0.1,
                    0.2,
                    (level.random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnAquaStackParticles(Level level, Vec3 pos, int stackCount) {
        // Bubbling water effect
        ParticleOptions acidBubbleParticle = getIronParticle("acid_bubble");
        int particleCount = 6 + (stackCount * 2);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.7;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.7;
            level.addParticle(acidBubbleParticle,
                pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ,
                0, 0.12 + (stackCount * 0.015), 0);
        }
        // Add acid particles dripping effect
        if (stackCount >= 2) {
            ParticleOptions acidParticle = getIronParticle("acid");
            for (int i = 0; i < stackCount; i++) {
                level.addParticle(acidParticle,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.5,
                    pos.y + 1.0 + level.random.nextDouble() * 0.5,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.5,
                    0, -0.05, 0);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnIceStackParticles(Level level, Vec3 pos, int stackCount) {
        // Swirling snowflakes
        ParticleOptions snowflakeParticle = getIronParticle("snowflake");
        int particleCount = 10 + (stackCount * 3);
        double radius = 0.7 + (stackCount * 0.1);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.05);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double height = level.random.nextDouble() * (1.0 + stackCount * 0.2);
            level.addParticle(snowflakeParticle,
                pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                -Math.cos(angle) * 0.03, -0.05, -Math.sin(angle) * 0.03);
        }
        // Add snow dust for more stacks
        if (stackCount >= 3) {
            ParticleOptions snowDustParticle = getIronParticle("snow_dust");
            for (int i = 0; i < stackCount * 2; i++) {
                level.addParticle(snowDustParticle,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.8,
                    pos.y + level.random.nextDouble() * 1.2,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.8,
                    (level.random.nextDouble() - 0.5) * 0.15,
                    -0.1,
                    (level.random.nextDouble() - 0.5) * 0.15);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnLightningStackParticles(Level level, Vec3 pos, int stackCount) {
        // Electric arcs spiraling upward
        ParticleOptions electricityParticle = getIronParticle("electricity");
        int particleCount = 12 + (stackCount * 4);
        double height = 1.5 + (stackCount * 0.2);
        for (int i = 0; i < particleCount; i++) {
            double t = i / (double) particleCount;
            double angle = t * Math.PI * 4.0; // 2 full rotations
            double spiralRadius = 0.4 * (1.0 - t * 0.5);
            double offsetX = Math.cos(angle) * spiralRadius;
            double offsetZ = Math.sin(angle) * spiralRadius;
            double offsetY = t * height;
            level.addParticle(electricityParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0.1, 0);
        }
        // Add spark bursts for higher stacks
        if (stackCount >= 2) {
            ParticleOptions sparkOptions = createSparkParticle(new Vector3f(0.6f, 0.8f, 1.0f));
            for (int i = 0; i < stackCount * 2; i++) {
                level.addParticle(sparkOptions,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.6,
                    pos.y + 0.5 + level.random.nextDouble() * 1.0,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.6,
                    (level.random.nextDouble() - 0.5) * 0.2,
                    0.15,
                    (level.random.nextDouble() - 0.5) * 0.2);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnNatureStackParticles(Level level, Vec3 pos, int stackCount) {
        // Floating fireflies orbiting
        ParticleOptions fireflyParticle = getIronParticle("firefly");
        int particleCount = 8 + (stackCount * 3);
        double radius = 0.8 + (stackCount * 0.1);
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2.0) / particleCount + (level.getGameTime() * 0.08);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = 0.3 + Math.sin(level.getGameTime() * 0.1 + i) * 0.3;
            level.addParticle(fireflyParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                -Math.sin(angle) * 0.05, 0.02, Math.cos(angle) * 0.05);
        }
        // Add leaf particles for higher stacks
        if (stackCount >= 3) {
            for (int i = 0; i < stackCount; i++) {
                level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.8,
                    pos.y + 1.5,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.8,
                    (level.random.nextDouble() - 0.5) * 0.05,
                    -0.05,
                    (level.random.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnEnderStackParticles(Level level, Vec3 pos, int stackCount) {
        // Unstable void particles pulsing
        ParticleOptions unstableEnderParticle = getIronParticle("unstable_ender");
        int particleCount = 10 + (stackCount * 4);
        double pulseRadius = 0.5 + Math.sin(level.getGameTime() * 0.15) * 0.3;
        for (int i = 0; i < particleCount; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double distance = level.random.nextDouble() * pulseRadius;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            double offsetY = level.random.nextDouble() * (1.0 + stackCount * 0.15);
            level.addParticle(unstableEnderParticle,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                (level.random.nextDouble() - 0.5) * 0.15,
                (level.random.nextDouble() - 0.5) * 0.15,
                (level.random.nextDouble() - 0.5) * 0.15);
        }
        // Add portal particles for max stacks
        if (stackCount >= 5) {
            ParticleOptions portalFrameParticle = getIronParticle("portal_frame");
            for (int i = 0; i < stackCount; i++) {
                level.addParticle(portalFrameParticle,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.6,
                    pos.y + 0.5,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.6,
                    0, 0.1, 0);
            }
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
            case FIRE -> getIronParticle("fire");
            case AQUA -> getIronParticle("acid_bubble");
            case ICE -> getIronParticle("snowflake");
            case LIGHTNING -> getIronParticle("electricity");
            case NATURE -> getIronParticle("firefly");
            case ENDER -> getIronParticle("unstable_ender");
        };
    }

    private static ParticleOptions getParticleForReaction(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> createFogParticle(new Vector3f(0.8f, 0.9f, 1.0f), 2.5f);
            case MELT -> getIronParticle("acid");
            case OVERLOADED -> getIronParticle("fire"); // Explosive fire
            case ELECTRO_CHARGED -> getIronParticle("electricity");
            case FROZEN -> getIronParticle("snow_dust");
            case SUPERCONDUCT -> getIronParticle("ring_smoke");
            case BURNING -> getIronParticle("dragon_fire");
            case BLOOM -> getIronParticle("firefly");
            case HYPERBLOOM -> getIronParticle("wisp");
            case BURGEON -> getIronParticle("ember");
            case UNSTABLE_WARD -> getIronParticle("portal_frame");
            case RIFT_PULL -> getIronParticle("unstable_ender");
            case SINGULARITY -> getIronParticle("siphon");
            case FRACTURE -> getIronParticle("snowflake"); // Shattering ice/crystal
            case WITHERING_SEED -> getIronParticle("blood_ground");
            case DECREPIT_GRASP -> getIronParticle("blood");
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void addReactionSpecificEffects(Level level, Vec3 pos, ElementalReaction reaction) {
        switch (reaction) {
            case VAPORIZE -> {
                // Rising steam clouds with fog
                ParticleOptions fogOptions = createFogParticle(new Vector3f(0.8f, 0.9f, 1.0f), 3.0f);
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
                    level.addParticle(fogOptions,
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        0, 0.4, 0);
                }
                // Add acid bubbles for water effect
                ParticleOptions acidBubbleParticle = getIronParticle("acid_bubble");
                for (int i = 0; i < 15; i++) {
                    level.addParticle(acidBubbleParticle,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        0, 0.2, 0);
                }
            }
            case OVERLOADED -> {
                // Massive explosion similar to Fireball spell
                // Central explosion emitter
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y + 0.5, pos.z, 0, 0, 0);

                // Multiple explosion particles in a sphere
                for (int i = 0; i < 8; i++) {
                    double angle1 = level.random.nextDouble() * Math.PI * 2.0;
                    double angle2 = level.random.nextDouble() * Math.PI;
                    double distance = level.random.nextDouble() * 2.0;
                    double offsetX = Math.sin(angle2) * Math.cos(angle1) * distance;
                    double offsetY = Math.cos(angle2) * distance;
                    double offsetZ = Math.sin(angle2) * Math.sin(angle1) * distance;
                    level.addParticle(ParticleTypes.EXPLOSION,
                        pos.x + offsetX, pos.y + 0.5 + offsetY, pos.z + offsetZ,
                        0, 0, 0);
                }

                // Fire particles bursting outward
                ParticleOptions fireParticle = getIronParticle("fire");
                for (int i = 0; i < 40; i++) {
                    double angle = (level.random.nextDouble() - 0.5) * Math.PI * 2.0;
                    double pitch = (level.random.nextDouble() - 0.3) * Math.PI;
                    double speed = 0.5 + level.random.nextDouble() * 0.8;
                    level.addParticle(fireParticle,
                        pos.x, pos.y + 0.5, pos.z,
                        Math.cos(pitch) * Math.cos(angle) * speed,
                        Math.sin(pitch) * speed,
                        Math.cos(pitch) * Math.sin(angle) * speed);
                }

                // Lightning sparks radiating outward
                ParticleOptions sparkOptions = createSparkParticle(new Vector3f(1.0f, 0.9f, 0.3f));
                for (int i = 0; i < 60; i++) {
                    double angle = (level.random.nextDouble() - 0.5) * Math.PI * 2.0;
                    double speed = 0.6 + level.random.nextDouble() * 0.7;
                    level.addParticle(sparkOptions,
                        pos.x, pos.y + 0.5, pos.z,
                        Math.cos(angle) * speed,
                        (level.random.nextDouble() - 0.2) * 0.5,
                        Math.sin(angle) * speed);
                }

                // Add smoke clouds
                for (int i = 0; i < 25; i++) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.5,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.4);
                }
            }
            case ELECTRO_CHARGED -> {
                // Lightning bolts with electricity and zap particles
                ParticleOptions electricityParticle = getIronParticle("electricity");
                for (int i = 0; i < 25; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    level.addParticle(electricityParticle,
                        pos.x + offsetX, pos.y + 2.5, pos.z + offsetZ,
                        0, -0.6, 0);
                }
                // Add zap particles (zap goes from particle spawn to a destination)
                for (int i = 0; i < 15; i++) {
                    double startX = pos.x + (level.random.nextDouble() - 0.5) * 1.5;
                    double startY = pos.y + level.random.nextDouble() * 2.0;
                    double startZ = pos.z + (level.random.nextDouble() - 0.5) * 1.5;
                    // Create destination offset from start position
                    Vec3 destination = new Vec3(
                        startX + (level.random.nextDouble() - 0.5) * 2.0,
                        startY + (level.random.nextDouble() - 0.5) * 2.0,
                        startZ + (level.random.nextDouble() - 0.5) * 2.0
                    );
                    ParticleOptions zapOptions = createZapParticle(destination);
                    level.addParticle(zapOptions, startX, startY, startZ, 0, 0, 0);
                }
            }
            case FROZEN -> {
                // Ice crystal burst with snowflakes
                ParticleOptions snowflakeParticle = getIronParticle("snowflake");
                for (int i = 0; i < 40; i++) {
                    double angle = (i * Math.PI * 2.0) / 40;
                    level.addParticle(snowflakeParticle,
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(angle) * 0.5, 0.15, Math.sin(angle) * 0.5);
                }
                // Add snow dust
                ParticleOptions snowDustParticle = getIronParticle("snow_dust");
                for (int i = 0; i < 30; i++) {
                    level.addParticle(snowDustParticle,
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
                ParticleOptions dragonFireParticle = getIronParticle("dragon_fire");
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    double height = i * 0.12;
                    level.addParticle(dragonFireParticle,
                        pos.x + Math.cos(angle) * 1.8, pos.y + height, pos.z + Math.sin(angle) * 1.8,
                        0, 0.25, 0);
                }
                // Add embers
                ParticleOptions emberParticle = getIronParticle("ember");
                for (int i = 0; i < 25; i++) {
                    level.addParticle(emberParticle,
                        pos.x, pos.y + 0.2, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.5,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.5);
                }
            }
            case HYPERBLOOM -> {
                // Glowing wisps
                ParticleOptions wispParticle = getIronParticle("wisp");
                for (int i = 0; i < 30; i++) {
                    level.addParticle(wispParticle,
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        (level.random.nextDouble() - 0.5) * 0.7);
                }
                // Add fireflies
                ParticleOptions fireflyParticle = getIronParticle("firefly");
                for (int i = 0; i < 20; i++) {
                    level.addParticle(fireflyParticle,
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
                ParticleOptions siphonParticle = getIronParticle("siphon");
                for (int ring = 0; ring < 6; ring++) {
                    for (int i = 0; i < 25; i++) {
                        double angle = (i * Math.PI * 2.0) / 25 + ring;
                        double ringRadius = 2.5 - (ring * 0.35);
                        level.addParticle(siphonParticle,
                            pos.x + Math.cos(angle) * ringRadius,
                            pos.y + 0.5,
                            pos.z + Math.sin(angle) * ringRadius,
                            -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4);
                    }
                }
                // Add unstable ender particles
                ParticleOptions unstableEnderParticle = getIronParticle("unstable_ender");
                for (int i = 0; i < 20; i++) {
                    level.addParticle(unstableEnderParticle,
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.3);
                }
            }
            case FRACTURE -> {
                // Shattering glass/crystal effect
                // Ice/crystal shards bursting outward in all directions
                ParticleOptions snowflakeParticle = getIronParticle("snowflake");
                for (int i = 0; i < 50; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
                    double speed = 0.3 + level.random.nextDouble() * 0.9;

                    level.addParticle(snowflakeParticle,
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(pitch) * Math.cos(angle) * speed,
                        Math.sin(pitch) * speed,
                        Math.cos(pitch) * Math.sin(angle) * speed);
                }

                // Add ice shard effect with snow dust
                ParticleOptions snowDustParticle = getIronParticle("snow_dust");
                for (int i = 0; i < 70; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    double speed = 0.4 + level.random.nextDouble() * 0.8;
                    level.addParticle(snowDustParticle,
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(angle) * speed,
                        (level.random.nextDouble() - 0.3) * 0.6,
                        Math.sin(angle) * speed);
                }

                // White spark shards flying outward (like glass shards)
                ParticleOptions sparkOptions = createSparkParticle(new Vector3f(1.0f, 1.0f, 1.0f));
                for (int i = 0; i < 80; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0;
                    double pitch = (level.random.nextDouble() - 0.4) * Math.PI * 0.8;
                    double speed = 0.5 + level.random.nextDouble() * 1.2;
                    level.addParticle(sparkOptions,
                        pos.x, pos.y + 1.0, pos.z,
                        Math.cos(pitch) * Math.cos(angle) * speed,
                        Math.sin(pitch) * speed,
                        Math.cos(pitch) * Math.sin(angle) * speed);
                }

                // Central shattering point with crit particles
                for (int i = 0; i < 15; i++) {
                    level.addParticle(ParticleTypes.CRIT,
                        pos.x, pos.y + 1.0, pos.z,
                        (level.random.nextDouble() - 0.5) * 1.0,
                        (level.random.nextDouble() - 0.5) * 1.0,
                        (level.random.nextDouble() - 0.5) * 1.0);
                }

                // Add enchantment glitter for the "magical shatter" effect
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleTypes.ENCHANT,
                        pos.x + (level.random.nextDouble() - 0.5) * 1.5,
                        pos.y + 1.0,
                        pos.z + (level.random.nextDouble() - 0.5) * 1.5,
                        (level.random.nextDouble() - 0.5) * 0.4,
                        level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.4);
                }
            }
            case BLOOM -> {
                // Nature bloom with fireflies
                ParticleOptions fireflyParticle = getIronParticle("firefly");
                for (int i = 0; i < 40; i++) {
                    level.addParticle(fireflyParticle,
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.6,
                        level.random.nextDouble() * 0.5,
                        (level.random.nextDouble() - 0.5) * 0.6);
                }
            }
            case BURGEON -> {
                // Explosive nature with embers
                ParticleOptions emberParticle = getIronParticle("ember");
                for (int i = 0; i < 35; i++) {
                    level.addParticle(emberParticle,
                        pos.x, pos.y + 0.5, pos.z,
                        (level.random.nextDouble() - 0.5) * 0.7,
                        level.random.nextDouble() * 0.8,
                        (level.random.nextDouble() - 0.5) * 0.7);
                }
            }
            case UNSTABLE_WARD -> {
                // Portal frame effect
                ParticleOptions portalFrameParticle = getIronParticle("portal_frame");
                for (int i = 0; i < 20; i++) {
                    double angle = (i * Math.PI * 2.0) / 20;
                    level.addParticle(portalFrameParticle,
                        pos.x + Math.cos(angle) * 1.5,
                        pos.y + 1.0,
                        pos.z + Math.sin(angle) * 1.5,
                        0, 0.1, 0);
                }
            }
            case RIFT_PULL -> {
                // Unstable ender vortex
                ParticleOptions unstableEnderParticle = getIronParticle("unstable_ender");
                for (int i = 0; i < 30; i++) {
                    double angle = (i * Math.PI * 2.0) / 30;
                    level.addParticle(unstableEnderParticle,
                        pos.x + Math.cos(angle) * 2.0,
                        pos.y + 0.5,
                        pos.z + Math.sin(angle) * 2.0,
                        -Math.cos(angle) * 0.4, 0, -Math.sin(angle) * 0.4);
                }
            }
            case WITHERING_SEED -> {
                // Blood ground spread
                ParticleOptions bloodGroundParticle = getIronParticle("blood_ground");
                for (int i = 0; i < 25; i++) {
                    level.addParticle(bloodGroundParticle,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.1,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        0, 0.05, 0);
                }
            }
            case DECREPIT_GRASP -> {
                // Blood particles
                ParticleOptions bloodParticle = getIronParticle("blood");
                for (int i = 0; i < 30; i++) {
                    level.addParticle(bloodParticle,
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
                ParticleOptions wispParticle = getIronParticle("wisp");
                for (int i = 0; i < 20; i++) {
                    level.addParticle(wispParticle,
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

    /**
     * Gets an Iron's Spellbooks particle from the registry
     * Returns vanilla fallback if the particle is not found (production-safe)
     */
    private static ParticleOptions getIronParticle(String particleName) {
        try {
            ResourceLocation particleId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particleName);
            var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType == null) {
                return ParticleTypes.CRIT; // Fallback to vanilla particle
            }
            // For simple particle types without options, the type itself is a ParticleOptions
            if (particleType instanceof ParticleOptions options) {
                return options;
            }
            return ParticleTypes.CRIT;
        } catch (Exception e) {
            return ParticleTypes.CRIT; // Fallback to vanilla particle
        }
    }

    /**
     * Creates a FogParticleOptions using reflection to avoid direct dependency
     * This allows the code to work even if Iron's Spellbooks classes change
     */
    private static ParticleOptions createFogParticle(Vector3f color, float scale) {
        try {
            Class<?> fogClass = Class.forName("io.redspace.ironsspellbooks.particle.FogParticleOptions");
            var constructor = fogClass.getConstructor(Vector3f.class, float.class);
            return (ParticleOptions) constructor.newInstance(color, scale);
        } catch (Exception e) {
            return ParticleTypes.CLOUD; // Fallback
        }
    }

    /**
     * Creates a SparkParticleOptions using reflection
     */
    private static ParticleOptions createSparkParticle(Vector3f color) {
        try {
            Class<?> sparkClass = Class.forName("io.redspace.ironsspellbooks.particle.SparkParticleOptions");
            var constructor = sparkClass.getConstructor(Vector3f.class);
            return (ParticleOptions) constructor.newInstance(color);
        } catch (Exception e) {
            return ParticleTypes.FLAME; // Fallback
        }
    }

    /**
     * Creates a ZapParticleOption using reflection
     */
    private static ParticleOptions createZapParticle(Vec3 destination) {
        try {
            Class<?> zapClass = Class.forName("io.redspace.ironsspellbooks.particle.ZapParticleOption");
            var constructor = zapClass.getConstructor(Vec3.class);
            return (ParticleOptions) constructor.newInstance(destination);
        } catch (Exception e) {
            return getIronParticle("electricity"); // Fallback to electricity particle
        }
    }
}
