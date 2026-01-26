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
            case ICE -> other == FIRE || other == AQUA || other == LIGHTNING || other == ENDER;
            case LIGHTNING -> other == FIRE || other == AQUA || other == ICE || other == NATURE || other == ENDER;
            case NATURE -> other == FIRE || other == AQUA || other == LIGHTNING || other == ENDER;
            case ENDER -> true; // Ender reacts with all elements
        };
    }

    public ElementalReaction getReactionWith(ElementType other) {
        if (!canReactWith(other)) return null;

        // Handle Ender special cases - only VOIDFIRE is implemented
        if (this == ENDER && other == FIRE) {
            return ElementalReaction.VOIDFIRE;
        }
        if (this == FIRE && other == ENDER) {
            return ElementalReaction.VOIDFIRE;
        }

        // Only return reactions that have strategy implementations
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
                default -> null; // Other Aqua reactions not implemented yet
            };
            case ICE -> switch (other) {
                case FIRE -> ElementalReaction.MELT;
                default -> null; // Other Ice reactions not implemented yet
            };
            case LIGHTNING -> switch (other) {
                case FIRE -> ElementalReaction.OVERLOADED;
                default -> null; // Other Lightning reactions not implemented yet
            };
            case NATURE -> switch (other) {
                case FIRE -> ElementalReaction.BURNING;
                default -> null; // Other Nature reactions not implemented yet
            };
            default -> null;
        };
    }
}
