package com.complextalents.passive.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache for passive stack data.
 * Used for HUD rendering and UI display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPassiveStackData {

    private static Map<String, Integer> stacks = new HashMap<>();

    /**
     * Sync passive stack data from the server.
     *
     * @param newStacks Map of stack type name to count
     */
    public static void syncFromServer(Map<String, Integer> newStacks) {
        stacks = new HashMap<>(newStacks);
    }

    /**
     * Get the current stack count for a specific stack type.
     *
     * @param stackTypeName The stack type name
     * @return The current stack count, or 0 if not found
     */
    public static int getStackCount(String stackTypeName) {
        return stacks.getOrDefault(stackTypeName, 0);
    }

    /**
     * Get all passive stacks.
     *
     * @return Unmodifiable map of all stacks
     */
    public static Map<String, Integer> getAllStacks() {
        return Map.copyOf(stacks);
    }

    /**
     * Check if player has any passive stacks.
     *
     * @return true if there are any stacks
     */
    public static boolean hasStacks() {
        return !stacks.isEmpty();
    }

    /**
     * Clear all passive stack data.
     */
    public static void clear() {
        stacks.clear();
    }
}
