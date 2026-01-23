package com.complextalents.elemental;

import net.minecraft.ChatFormatting;

public enum ElementType {
    FIRE,
    AQUA,
    LIGHTNING,
    ICE,
    NATURE,
    ENDER;

    public boolean canReactWith(ElementType other) {
        if (other == null || this == other) return false;

        return switch (this) {
            case FIRE -> other == AQUA || other == ICE || other == LIGHTNING || other == NATURE || other == ENDER;
            case AQUA -> other == FIRE || other == ICE || other == LIGHTNING || other == NATURE || other == ENDER;
            case ICE -> other == FIRE || other == AQUA || other == LIGHTNING || other == ENDER;
            case LIGHTNING -> other == FIRE || other == AQUA || other == ICE || other == NATURE || other == ENDER;
            case NATURE -> other == FIRE || other == AQUA || other == LIGHTNING || other == ENDER;
            case ENDER -> true; // Ender reacts with all elements
        };
    }

    public ElementalReaction getReactionWith(ElementType other) {
        if (!canReactWith(other)) return null;

        // Handle Ender special cases
        if (this == ENDER) {
            return switch (other) {
                case FIRE -> ElementalReaction.SINGULARITY;
                case AQUA -> ElementalReaction.DECREPIT_GRASP;
                case LIGHTNING -> ElementalReaction.RIFT_PULL;
                case ICE -> ElementalReaction.FRACTURE;
                case NATURE -> ElementalReaction.WITHERING_SEED;
                default -> null;
            };
        }

        if (other == ENDER) {
            return switch (this) {
                case FIRE -> ElementalReaction.SINGULARITY;
                case AQUA -> ElementalReaction.DECREPIT_GRASP;
                case LIGHTNING -> ElementalReaction.RIFT_PULL;
                case ICE -> ElementalReaction.FRACTURE;
                case NATURE -> ElementalReaction.WITHERING_SEED;
                default -> null;
            };
        }

        return switch (this) {
            case FIRE -> switch (other) {
                case AQUA -> ElementalReaction.VAPORIZE;
                case ICE -> ElementalReaction.MELT;
                case LIGHTNING -> ElementalReaction.OVERLOADED;
                case NATURE -> ElementalReaction.BURNING;
                default -> null;
            };
            case AQUA -> switch (other) {
                case FIRE -> ElementalReaction.VAPORIZE;
                case ICE -> ElementalReaction.FROZEN;
                case LIGHTNING -> ElementalReaction.ELECTRO_CHARGED;
                case NATURE -> ElementalReaction.BLOOM;
                default -> null;
            };
            case ICE -> switch (other) {
                case FIRE -> ElementalReaction.MELT;
                case AQUA -> ElementalReaction.FROZEN;
                case LIGHTNING -> ElementalReaction.SUPERCONDUCT;
                default -> null;
            };
            case LIGHTNING -> switch (other) {
                case FIRE -> ElementalReaction.OVERLOADED;
                case AQUA -> ElementalReaction.ELECTRO_CHARGED;
                case ICE -> ElementalReaction.SUPERCONDUCT;
                case NATURE -> ElementalReaction.HYPERBLOOM; // Lightning on Bloom cores
                default -> null;
            };
            case NATURE -> switch (other) {
                case FIRE -> ElementalReaction.BURNING;
                case AQUA -> ElementalReaction.BLOOM;
                case LIGHTNING -> ElementalReaction.HYPERBLOOM;
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Get the display name for this element
     */
    public String getDisplayName() {
        return switch (this) {
            case FIRE -> "Fire";
            case AQUA -> "Aqua";
            case LIGHTNING -> "Lightning";
            case ICE -> "Ice";
            case NATURE -> "Nature";
            case ENDER -> "Ender";
        };
    }

    /**
     * Get the chat color for this element
     */
    public ChatFormatting getChatColor() {
        return switch (this) {
            case FIRE -> ChatFormatting.RED;
            case AQUA -> ChatFormatting.BLUE;
            case LIGHTNING -> ChatFormatting.YELLOW;
            case ICE -> ChatFormatting.AQUA;
            case NATURE -> ChatFormatting.GREEN;
            case ENDER -> ChatFormatting.DARK_PURPLE;
        };
    }

    /**
     * Get the particle color as RGB values (0-1 range)
     */
    public float[] getParticleRGB() {
        return switch (this) {
            case FIRE -> new float[]{1.0f, 0.3f, 0.0f};      // Orange-red
            case AQUA -> new float[]{0.0f, 0.5f, 1.0f};      // Light blue
            case LIGHTNING -> new float[]{1.0f, 1.0f, 0.0f};  // Yellow
            case ICE -> new float[]{0.7f, 0.9f, 1.0f};       // Ice blue
            case NATURE -> new float[]{0.2f, 0.8f, 0.2f};    // Green
            case ENDER -> new float[]{0.5f, 0.0f, 0.8f};     // Purple
        };
    }
}
