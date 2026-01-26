package com.complextalents.elemental;

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
            case ICE -> other == FIRE || other == AQUA || other == LIGHTNING || other == NATURE || other == ENDER;
            case LIGHTNING -> other == FIRE || other == AQUA || other == ICE || other == NATURE || other == ENDER;
            case NATURE -> other == FIRE || other == AQUA || other == ICE || other == LIGHTNING || other == ENDER;
            case ENDER -> true; // Ender reacts with all elements
        };
    }

    public ElementalReaction getReactionWith(ElementType other) {
        if (!canReactWith(other)) return null;

        // Handle Ender special cases
        if (this == ENDER && other == FIRE) {
            return ElementalReaction.VOIDFIRE;
        }
        if (this == FIRE && other == ENDER) {
            return ElementalReaction.VOIDFIRE;
        }
        if (this == ENDER && other == ICE) {
            return ElementalReaction.FRACTURE;
        }
        if (this == ICE && other == ENDER) {
            return ElementalReaction.FRACTURE;
        }

        // Return reactions that have strategy implementations
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
                case ICE -> ElementalReaction.FREEZE;
                case LIGHTNING -> ElementalReaction.ELECTRO_CHARGED;
                default -> null;
            };
            case ICE -> switch (other) {
                case FIRE -> ElementalReaction.MELT;
                case AQUA -> ElementalReaction.FREEZE;
                case LIGHTNING -> ElementalReaction.SUPERCONDUCT;
                case NATURE -> ElementalReaction.PERMAFROST;
                default -> null;
            };
            case LIGHTNING -> switch (other) {
                case FIRE -> ElementalReaction.OVERLOADED;
                case ICE -> ElementalReaction.SUPERCONDUCT;
                case AQUA -> ElementalReaction.ELECTRO_CHARGED;
                default -> null;
            };
            case NATURE -> switch (other) {
                case FIRE -> ElementalReaction.BURNING;
                case ICE -> ElementalReaction.PERMAFROST;
                default -> null;
            };
            default -> null;
        };
    }
}
