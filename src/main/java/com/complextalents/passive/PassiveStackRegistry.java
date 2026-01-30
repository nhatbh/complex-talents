package com.complextalents.passive;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for passive stack type definitions.
 * Maps (ownerId + ownerType + stackName) -> PassiveStackDef
 * <p>
 * When an owner (origin or skill) is built, its passive stack definitions are registered here.
 * The capability can then look up definitions to clamp values and get display info.
 * </p>
 */
public class PassiveStackRegistry {

    /**
     * Compound key for looking up stack definitions.
     */
    private record PassiveStackKey(String ownerId, String ownerType, String stackName) {}

    /**
     * Storage for all passive stack definitions.
     */
    private static final Map<PassiveStackKey, PassiveStackDef> REGISTRY = new HashMap<>();

    // Private constructor to prevent instantiation
    private PassiveStackRegistry() {}

    // ========== New API - Using PassiveOwner ==========

    /**
     * Register a passive stack definition for an owner.
     * Called automatically when an origin or skill is built.
     *
     * @param owner The owner (origin or skill)
     * @param stackName The stack type name
     * @param def The stack definition
     */
    public static void register(PassiveOwner owner, String stackName, PassiveStackDef def) {
        REGISTRY.put(new PassiveStackKey(owner.getOwnerId().toString(), owner.getOwnerType(), stackName), def);
    }

    /**
     * Get a passive stack definition for an owner.
     *
     * @param owner The owner (origin or skill)
     * @param stackName The stack type name
     * @return The stack definition, or null if not found
     */
    public static PassiveStackDef getDef(PassiveOwner owner, String stackName) {
        return REGISTRY.get(new PassiveStackKey(owner.getOwnerId().toString(), owner.getOwnerType(), stackName));
    }

    /**
     * Get all passive stack definitions for an owner.
     *
     * @param owner The owner (origin or skill)
     * @return Unmodifiable map of stack name -> definition (empty if none)
     */
    public static Map<String, PassiveStackDef> getStacksForOwner(PassiveOwner owner) {
        Map<String, PassiveStackDef> result = new HashMap<>();
        for (Map.Entry<PassiveStackKey, PassiveStackDef> entry : REGISTRY.entrySet()) {
            if (entry.getKey().ownerId().equals(owner.getOwnerId().toString())
                    && entry.getKey().ownerType().equals(owner.getOwnerType())) {
                result.put(entry.getKey().stackName(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    // ========== Legacy API - Using ResourceLocation (for backward compatibility) ==========

    /**
     * Register a passive stack definition for an origin (legacy method).
     * Assumes owner type is "origin".
     *
     * @param ownerId The origin ID
     * @param stackName The stack type name
     * @param def The stack definition
     * @deprecated Use {@link #register(PassiveOwner, String, PassiveStackDef)} instead
     */
    @Deprecated
    public static void register(ResourceLocation ownerId, String stackName, PassiveStackDef def) {
        REGISTRY.put(new PassiveStackKey(ownerId.toString(), "origin", stackName), def);
    }

    /**
     * Get a passive stack definition for an origin (legacy method).
     * Assumes owner type is "origin".
     *
     * @param ownerId The origin ID
     * @param stackName The stack type name
     * @return The stack definition, or null if not found
     * @deprecated Use {@link #getDef(PassiveOwner, String)} instead
     */
    @Deprecated
    public static PassiveStackDef getDef(ResourceLocation ownerId, String stackName) {
        return REGISTRY.get(new PassiveStackKey(ownerId.toString(), "origin", stackName));
    }

    /**
     * Get all passive stack definitions for an origin (legacy method).
     * Assumes owner type is "origin".
     *
     * @param ownerId The origin ID
     * @return Unmodifiable map of stack name -> definition (empty if none)
     * @deprecated Use {@link #getStacksForOwner(PassiveOwner)} instead
     */
    @Deprecated
    public static Map<String, PassiveStackDef> getStacksForOrigin(ResourceLocation ownerId) {
        Map<String, PassiveStackDef> result = new HashMap<>();
        for (Map.Entry<PassiveStackKey, PassiveStackDef> entry : REGISTRY.entrySet()) {
            if (entry.getKey().ownerId().equals(ownerId.toString())
                    && entry.getKey().ownerType().equals("origin")) {
                result.put(entry.getKey().stackName(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    // ========== Utility Methods ==========

    /**
     * Clear all registered stack definitions.
     * Useful for testing or mod reload scenarios.
     */
    public static void clear() {
        REGISTRY.clear();
    }

    /**
     * Check if the registry has any data.
     *
     * @return true if there are any registered stack definitions
     */
    public static boolean isEmpty() {
        return REGISTRY.isEmpty();
    }

    /**
     * Get statistics about the registry.
     *
     * @return Map containing statistics
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_stack_definitions", REGISTRY.size());

        // Count owners with stacks
        Map<String, Integer> ownerCounts = new HashMap<>();
        for (PassiveStackKey key : REGISTRY.keySet()) {
            String ownerKey = key.ownerType() + ":" + key.ownerId();
            ownerCounts.merge(ownerKey, 1, Integer::sum);
        }
        stats.put("owners_with_stacks", ownerCounts.size());
        stats.put("owner_counts", ownerCounts);

        return stats;
    }

    /**
     * Get a stack definition by raw key components.
     * Searches all owner types for the given stack name.
     * Used by capability for max stack lookups.
     *
     * @param ownerId The owner ID as string
     * @param stackName The stack name
     * @return The stack definition, or null if not found
     */
    public static PassiveStackDef getDefRaw(String ownerId, String stackName) {
        for (PassiveStackKey key : REGISTRY.keySet()) {
            if (key.ownerId().equals(ownerId) && key.stackName().equals(stackName)) {
                return REGISTRY.get(key);
            }
        }
        return null;
    }
}
