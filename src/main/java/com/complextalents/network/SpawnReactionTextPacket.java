package com.complextalents.network;

import com.complextalents.elemental.ElementalReaction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for spawning floating reaction text on the client
 * Sent from server when elemental reactions trigger
 */
public class SpawnReactionTextPacket {
    private final int entityId;
    private final String reactionName;
    private final float damage;
    private final boolean isSuperReaction;

    public SpawnReactionTextPacket(Entity target, ElementalReaction reaction, float damage) {
        this.entityId = target.getId();
        this.reactionName = reaction.name();
        this.damage = damage;
        this.isSuperReaction = false;
    }

    // Constructor for Super-Reactions
    public SpawnReactionTextPacket(int entityId, String reactionText, ChatFormatting color) {
        this.entityId = entityId;
        this.reactionName = reactionText;
        this.damage = 0;
        this.isSuperReaction = true;
    }

    // Decode constructor
    public SpawnReactionTextPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.reactionName = buffer.readUtf();
        this.damage = buffer.readFloat();
        this.isSuperReaction = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeUtf(reactionName);
        buffer.writeFloat(damage);
        buffer.writeBoolean(isSuperReaction);
    }

    public static SpawnReactionTextPacket decode(FriendlyByteBuf buffer) {
        return new SpawnReactionTextPacket(buffer);
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

        Entity entity = level.getEntity(entityId);
        if (entity == null) return;

        if (isSuperReaction) {
            // Handle Super-Reaction display
            spawnSuperReactionText(entity, reactionName);
        } else {
            ElementalReaction reaction = ElementalReaction.valueOf(reactionName);
            // Spawn damage indicator (floating text)
            spawnFloatingText(entity, reaction, damage);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnFloatingText(Entity entity, ElementalReaction reaction, float damage) {
        // Format reaction name for display (convert ELECTRO_CHARGED to "Electro-Charged")
        String displayName = formatReactionName(reaction.name());

        // Get color based on reaction type and format with Minecraft color codes
        String colorCode = getReactionColorCode(reaction);

        // Create the display text with color and damage
        String damageText = damage > 0 ? String.format(" §f(%.1f)", damage) : "";
        String fullText = String.format("%s⚡ %s%s", colorCode, displayName, damageText);

        // Send message to player in action bar (overlaid text)
        if (Minecraft.getInstance().player != null) {
            double distance = Minecraft.getInstance().player.position().distanceTo(entity.position());
            if (distance < 48.0) { // Only show if within 48 blocks
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(fullText),
                    true // Action bar (overlaid text)
                );
            }
        }

        // Also spawn crit-like particles above the entity for visual emphasis
        Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.8, 0);

        // Spawn enchantment glint particles to draw attention
        for (int i = 0; i < 8; i++) {
            double offsetX = (entity.level().random.nextDouble() - 0.5) * 0.5;
            double offsetY = entity.level().random.nextDouble() * 0.3;
            double offsetZ = (entity.level().random.nextDouble() - 0.5) * 0.5;

            entity.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0.1, 0
            );
        }

        // Add reaction-specific visual flair particle
        entity.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.FLASH,
            pos.x, pos.y, pos.z,
            0, 0, 0
        );
    }

    /**
     * Formats reaction name from ENUM_CASE to "Title Case"
     */
    private String formatReactionName(String name) {
        String[] words = name.split("_");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            formatted.append(word.charAt(0));
            formatted.append(word.substring(1).toLowerCase());

            if (i < words.length - 1) {
                formatted.append("-");
            }
        }

        return formatted.toString();
    }

    /**
     * Returns a Minecraft color code for the reaction
     */
    private String getReactionColorCode(ElementalReaction reaction) {
        return switch (reaction) {
            case VAPORIZE -> "§b"; // Aqua
            case MELT -> "§6"; // Gold
            case OVERLOADED -> "§e"; // Yellow
            case ELECTRO_CHARGED -> "§d"; // Light purple
            case FROZEN -> "§b"; // Aqua
            case SUPERCONDUCT -> "§3"; // Dark aqua
            case BURNING -> "§c"; // Red
            case BLOOM -> "§a"; // Green
            case HYPERBLOOM -> "§e"; // Yellow
            case BURGEON -> "§6"; // Gold
            case UNSTABLE_WARD -> "§5"; // Dark purple
            case RIFT_PULL -> "§5"; // Dark purple
            case SINGULARITY -> "§1"; // Dark blue
            case FRACTURE -> "§f"; // White
            case WITHERING_SEED -> "§4"; // Dark red
            case DECREPIT_GRASP -> "§8"; // Dark gray
            default -> throw new IllegalArgumentException("Unexpected value: " + reaction);
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnSuperReactionText(Entity entity, String reactionText) {
        // Display the Super-Reaction text with special formatting
        String fullText = "§6§l" + reactionText; // Gold and bold

        // Send message to player in action bar
        if (Minecraft.getInstance().player != null) {
            double distance = Minecraft.getInstance().player.position().distanceTo(entity.position());
            if (distance < 64.0) { // Show Super-Reactions from farther away
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(fullText),
                    true // Action bar
                );
            }
        }

        // Spawn spectacular particles for Super-Reaction
        Vec3 pos = entity.position().add(0, entity.getBbHeight() + 1.0, 0);

        // Create explosion of particles
        for (int i = 0; i < 30; i++) {
            double offsetX = (entity.level().random.nextDouble() - 0.5) * 2.0;
            double offsetY = entity.level().random.nextDouble() * 1.0;
            double offsetZ = (entity.level().random.nextDouble() - 0.5) * 2.0;

            entity.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0.2, 0
            );

            entity.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x + offsetX * 0.5, pos.y + offsetY * 0.5, pos.z + offsetZ * 0.5,
                offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1
            );
        }

        // Add central flash
        entity.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.FLASH,
            pos.x, pos.y, pos.z,
            0, 0, 0
        );
    }
}
