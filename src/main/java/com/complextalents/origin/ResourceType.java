package com.complextalents.origin;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a type of resource that origins can use.
 * Resource types are reusable across multiple origins.
 * <p>
 * For example, both Cleric and Paladin origins might use the "Piety" resource type.
 */
public class ResourceType {
    private static final Map<ResourceLocation, ResourceType> REGISTRY = new HashMap<>();

    private final ResourceLocation id;
    private final String name;
    private final double min;
    private final double max;
    private final int color; // ARGB format

    private ResourceType(ResourceLocation id, String name, double min, double max, int color) {
        this.id = id;
        this.name = name;
        this.min = min;
        this.max = max;
        this.color = color;
    }

    /**
     * Register a new resource type.
     *
     * @param id    The resource type ID (e.g., "complextalents:piety")
     * @param name  The display name (e.g., "Piety")
     * @param min   The minimum value (typically 0)
     * @param max   The maximum value (e.g., 100)
     * @param color The color for the resource bar (ARGB format)
     * @return The registered resource type
     */
    public static ResourceType register(ResourceLocation id, String name, double min, double max, int color) {
        ResourceType resourceType = new ResourceType(id, name, min, max, color);
        REGISTRY.put(id, resourceType);
        return resourceType;
    }

    /**
     * Get a resource type by ID.
     *
     * @param id The resource type ID
     * @return The resource type, or null if not found
     */
    @Nullable
    public static ResourceType get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * Get all registered resource types.
     */
    public static Map<ResourceLocation, ResourceType> getAll() {
        return new HashMap<>(REGISTRY);
    }

    /**
     * Clear all registered resource types (for testing/reloading).
     */
    public static void clear() {
        REGISTRY.clear();
    }

    public ResourceLocation getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getColor() {
        return color;
    }

    /**
     * Clamp a value to this resource type's min/max range.
     */
    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
