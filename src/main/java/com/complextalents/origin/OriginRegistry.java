package com.complextalents.origin;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all origins.
 * Mirrors the SkillRegistry pattern.
 */
public class OriginRegistry {

    private static final OriginRegistry INSTANCE = new OriginRegistry();
    private final Map<ResourceLocation, Origin> origins = new HashMap<>();
    private boolean initialized = false;

    private OriginRegistry() {
    }

    /**
     * Get the singleton instance of the OriginRegistry.
     */
    public static OriginRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register an origin.
     *
     * @param origin The origin to register
     */
    public void register(Origin origin) {
        ResourceLocation id = origin.getId();
        if (origins.containsKey(id)) {
            throw new IllegalArgumentException("Origin already registered: " + id);
        }
        origins.put(id, origin);
    }

    /**
     * Get an origin by ID.
     *
     * @param id The origin ID
     * @return The origin, or null if not found
     */
    @Nullable
    public Origin getOrigin(ResourceLocation id) {
        return origins.get(id);
    }

    /**
     * Get an origin by ID string.
     *
     * @param id The origin ID string (e.g., "complextalents:cleric")
     * @return The origin, or null if not found
     */
    @Nullable
    public Origin getOrigin(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        return loc != null ? origins.get(loc) : null;
    }

    /**
     * Check if an origin is registered.
     */
    public boolean hasOrigin(ResourceLocation id) {
        return origins.containsKey(id);
    }

    /**
     * Get all registered origins.
     */
    public Collection<Origin> getAllOrigins() {
        return origins.values();
    }

    /**
     * Clear all registered origins.
     * For testing/reloading purposes.
     */
    public void clear() {
        origins.clear();
        initialized = false;
    }

    /**
     * Mark the registry as initialized.
     * Called during mod initialization.
     */
    public void initialize() {
        initialized = true;
    }

    /**
     * Check if the registry has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}
