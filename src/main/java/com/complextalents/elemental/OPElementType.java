package com.complextalents.elemental;

/**
 * Supported elements for the Overwhelming Power mechanic.
 */
public enum OPElementType {
    FIRE(ElementType.FIRE),
    AQUA(ElementType.AQUA),
    NATURE(ElementType.NATURE),
    LIGHTNING(ElementType.LIGHTNING),
    ICE(ElementType.ICE);

    private final ElementType baseType;

    OPElementType(ElementType baseType) {
        this.baseType = baseType;
    }

    public ElementType getBaseType() {
        return baseType;
    }

    public static OPElementType fromBaseType(ElementType type) {
        for (OPElementType opType : values()) {
            if (opType.baseType == type) {
                return opType;
            }
        }
        return null;
    }
}
